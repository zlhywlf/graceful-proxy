package zlhywlf.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyConfig;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyThreadPoolGroup;
import zlhywlf.proxy.server.adapters.ClientToProxyAdapter;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class DefaultProxyServer implements ProxyServer<EventLoopGroup> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyServer.class);

    private final ProxyConfig config;
    private final ChannelGroup channels = new DefaultChannelGroup("server group", GlobalEventExecutor.INSTANCE);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Thread jvmShutdownHook = new Thread(this::stop, "graceful-proxy-stop-hook");
    private volatile InetSocketAddress boundAddress;
    private final ProxyThreadPoolGroup<EventLoopGroup> proxyThreadPoolGroup;
    private final InetSocketAddress requestedAddress;

    public DefaultProxyServer(ProxyConfig config, ProxyThreadPoolGroup<EventLoopGroup> proxyThreadPoolGroup, InetSocketAddress requestedAddress) {
        this.proxyThreadPoolGroup = proxyThreadPoolGroup;
        this.config = config;
        this.requestedAddress = requestedAddress;
    }

    public void registerChannel(Channel channel) {
        channels.add(channel);
    }

    public void unregisterChannel(Channel channel) {
        if (channel.isOpen()) channel.close();
        channels.remove(channel);
    }

    public void closeChannels(boolean graceful) {
        ChannelGroupFuture f = channels.close();
        if (graceful) {
            try {
                f.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for channels to shut down gracefully.");
            }
            if (!f.isSuccess()) {
                f.forEach(cf -> {
                    if (!cf.isSuccess()) {
                        logger.warn("Unable to close channel. Cause of failure for {} is {}", cf.channel(), String.valueOf(cf.cause()));
                    }
                });
            }
        }
    }

    public void doStop(boolean graceful) {
        if (stopped.compareAndSet(false, true)) {
            logger.info("Shutting down proxy server {}", graceful ? "(graceful)" : "(non-graceful)");
            closeChannels(graceful);
            proxyThreadPoolGroup.unregisterProxyServer(this, graceful);
            try {
                Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
            } catch (IllegalStateException ignore) {
            }
            logger.info("Done shutting down proxy server");
        }
    }

    public void stop() {
        doStop(true);
    }

    public void abort() {
        doStop(false);
    }

    public DefaultProxyServer start() {
        if (!isStopped()) {
            proxyThreadPoolGroup.registerProxyServer(this);
            ChannelFuture cf = new ServerBootstrap()
                .group(proxyThreadPoolGroup.getBossPool(), proxyThreadPoolGroup.getClientToProxyPool())
                .channel(ProxyUtils.getClazz(config.channelClass()))
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(@NonNull Channel channel) {
                        new ClientToProxyAdapter(DefaultProxyServer.this, channel.pipeline());
                    }
                })
                .bind(requestedAddress).addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) registerChannel(f.channel());
                })
                .awaitUninterruptibly();
            Throwable cause = cf.cause();
            if (cause != null) {
                abort();
                throw new RuntimeException(cause);
            }
            Runtime.getRuntime().addShutdownHook(getJvmShutdownHook());
            boundAddress = (InetSocketAddress) cf.channel().localAddress();
            logger.info("Proxy started at address: {}", boundAddress);
            return this;
        }
        throw new IllegalStateException("Attempted to start proxy, but proxy's server group is already stopped");
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
