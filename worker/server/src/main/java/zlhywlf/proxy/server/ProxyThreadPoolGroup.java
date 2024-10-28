package zlhywlf.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class ProxyThreadPoolGroup {
    private static final Logger logger = LoggerFactory.getLogger(ProxyThreadPoolGroup.class);
    private static final AtomicInteger proxyThreadPoolGroupCount = new AtomicInteger(0);

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Class<? extends ServerChannel> channelClazz;
    private final ChannelGroup channels = new DefaultChannelGroup("server group", GlobalEventExecutor.INSTANCE);
    private final int proxyThreadPoolGroupId;
    private final String name;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Thread jvmShutdownHook = new Thread(this::stop, "graceful-proxy-stop-hook");

    @SuppressWarnings("unchecked")
    public ProxyThreadPoolGroup() {
        proxyThreadPoolGroupId = proxyThreadPoolGroupCount.getAndIncrement();
        name = SystemPropertyUtil.get("proxy.name", "GracefulProxy");
        String eventLoopClassName = SystemPropertyUtil.get("proxy.eventLoopClass", "io.netty.channel.nio.NioEventLoopGroup");
        String channelClassName = SystemPropertyUtil.get("proxy.channelClass", "io.netty.channel.socket.nio.NioServerSocketChannel");
        try {
            Class<?> eventLoopClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), eventLoopClassName, false);
            Class<?> channelClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), channelClassName, false);
            if (!ClassUtils.isAssignable(eventLoopClazz, EventLoopGroup.class)) {
                throw new IllegalArgumentException("proxy.eventLoopClass");
            }
            bossGroup = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 1, new ProxyThreadFactory(eventLoopClazz, proxyThreadPoolGroupId, name, "boss"));
            workerGroup = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 0, new ProxyThreadFactory(eventLoopClazz, proxyThreadPoolGroupId, name, "worker"));
            if (!ClassUtils.isAssignable(channelClazz, ServerChannel.class)) {
                throw new IllegalArgumentException("proxy.channelClass");
            }
            this.channelClazz = (Class<? extends ServerChannel>) channelClazz;
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
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

    public void closePools(boolean graceful) {
        List<EventLoopGroup> pools = List.of(bossGroup, workerGroup);
        pools.forEach(pool -> {
            if (graceful) {
                pool.shutdownGracefully();
                return;
            }
            pool.shutdownGracefully(0L, 0L, TimeUnit.SECONDS);
        });
        if (graceful) {
            pools.forEach(pool -> {
                try {
                    pool.awaitTermination(60L, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while shutting down event loop");
                }
            });
        }
    }

    public void doStop(boolean graceful) {
        if (stopped.compareAndSet(false, true)) {
            logger.info("Shutting down proxy server {}", graceful ? "(graceful)" : "(non-graceful)");
            closeChannels(graceful);
            closePools(graceful);
            try {
                Runtime.getRuntime().removeShutdownHook(jvmShutdownHook);
            } catch (IllegalStateException ignore) {
            }
            logger.info("Done shutting down proxy server");
            return;
        }
        logger.info("Shutdown requested, but ServerGroup is already stopped. Doing nothing.");
    }

    public void stop() {
        doStop(true);
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
