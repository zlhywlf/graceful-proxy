package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
public class ProxyThreadPool {
    private static final Logger logger = LoggerFactory.getLogger(ProxyThreadPool.class);

    private final EventLoopGroup bossPool;
    private final EventLoopGroup clientToProxyPool;
    private final EventLoopGroup proxyToServerPool;

    public ProxyThreadPool(ProxyContext context) {
        int serverGroupId = context.getProxyThreadPoolGroupId();
        ProxyConfig proxyConfig = context.getProxyConfig();
        try {
            Class<?> eventLoopClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), proxyConfig.eventLoopClass(), false);
            if (!ClassUtils.isAssignable(eventLoopClazz, EventLoopGroup.class)) {
                throw new IllegalArgumentException("eventLoopClass");
            }
            bossPool = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 1, new ProxyThreadFactory(eventLoopClazz, serverGroupId, proxyConfig.name(), "boss"));
            clientToProxyPool = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 0, new ProxyThreadFactory(eventLoopClazz, serverGroupId, proxyConfig.name(), "clientToProxy"));
            proxyToServerPool = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 0, new ProxyThreadFactory(eventLoopClazz, serverGroupId, proxyConfig.name(), "proxyToServer"));
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public void close(boolean graceful) {
        List<EventLoopGroup> pools = List.of(bossPool, clientToProxyPool, proxyToServerPool);
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
                    assert pool.awaitTermination(60L, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while shutting down event loop", e);
                }
            });
        }
    }
}
