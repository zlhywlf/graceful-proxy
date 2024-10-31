package zlhywlf.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.adapters.ClientToProxyAdapter;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class ProxyServer {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private final ProxyContext context;
    private final ChannelGroup channels = new DefaultChannelGroup("server group", GlobalEventExecutor.INSTANCE);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Thread jvmShutdownHook = new Thread(this::stop, "graceful-proxy-stop-hook");
    private volatile SocketAddress boundAddress;

    public ProxyServer(ProxyContext context) {
        this.context = context;
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
            context.getProxyThreadPoolGroup().unregisterProxyServer(this, graceful);
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

    @SuppressWarnings("unchecked")
    public void start() {
        if (!isStopped()) {
            try {
                Class<?> channelClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), context.getProxyConfig().channelClass(), false);
                if (!ClassUtils.isAssignable(channelClazz, ServerChannel.class)) {
                    throw new IllegalArgumentException("channelClass");
                }
                context.getProxyThreadPoolGroup().registerProxyServer(this);
                ChannelFuture cf = new ServerBootstrap()
                    .group(context.getProxyThreadPoolGroup().getBossPool(), context.getProxyThreadPoolGroup().getClientToProxyPool())
                    .channel((Class<? extends ServerChannel>) channelClazz)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel channel) {
                            new ClientToProxyAdapter(ProxyServer.this, channel.pipeline());
                        }
                    })
                    .bind(context.getRequestedAddress()).addListener((ChannelFutureListener) f -> {
                        if (f.isSuccess()) registerChannel(f.channel());
                    })
                    .awaitUninterruptibly();
                Throwable cause = cf.cause();
                if (cause != null) {
                    abort();
                    throw new RuntimeException(cause);
                }
                Runtime.getRuntime().addShutdownHook(getJvmShutdownHook());
                boundAddress = cf.channel().localAddress();
                logger.info("Proxy started at address: {}", boundAddress);
                return;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("Attempted to start proxy, but proxy's server group is already stopped");
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
