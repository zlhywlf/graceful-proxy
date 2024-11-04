package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyThreadPoolGroup;
import zlhywlf.proxy.server.config.ProxyThreadPoolConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class DefaultProxyThreadPoolGroup implements ProxyThreadPoolGroup<EventLoopGroup> {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyThreadPoolGroup.class);
    private static final AtomicInteger proxyThreadPoolGroupCount = new AtomicInteger(1);

    private final int proxyThreadPoolGroupId;
    private final EventLoopGroup bossPool;
    private final EventLoopGroup clientToProxyPool;
    private final EventLoopGroup proxyToServerPool;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final List<ProxyServer<EventLoopGroup>> registeredServers = new ArrayList<>(1);
    private final Object SERVER_REGISTRATION_LOCK = new Object();
    private final String category;

    public DefaultProxyThreadPoolGroup(ProxyThreadPoolConfig config) {
        proxyThreadPoolGroupId = proxyThreadPoolGroupCount.getAndIncrement();

        try {
            Class<? extends EventLoopGroup> eventLoopClazz = config.getEventLoopClazz();
            category = StringUtils.joinWith("-", config.getName(), proxyThreadPoolGroupId);
            bossPool = ConstructorUtils.invokeConstructor(eventLoopClazz, 1, new ProxyThreadFactory("boss", eventLoopClazz, category));
            clientToProxyPool = ConstructorUtils.invokeConstructor(eventLoopClazz, 0, new ProxyThreadFactory("clientToProxy", eventLoopClazz, category));
            proxyToServerPool = ConstructorUtils.invokeConstructor(eventLoopClazz, 0, new ProxyThreadFactory("proxyToServer", eventLoopClazz, category));
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private void close(boolean graceful) {
        if (!stopped.compareAndSet(false, true)) {
            logger.info("Shutdown requested, but thread pool group is already stopped. Doing nothing.");
            return;
        }
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
        logger.info("Done shutting down thread pool group");
    }

    @Override
    public void registerProxyServer(ProxyServer<EventLoopGroup> proxyServer) {
        synchronized (SERVER_REGISTRATION_LOCK) {
            registeredServers.add(proxyServer);
        }
    }

    @Override
    public void unregisterProxyServer(ProxyServer<EventLoopGroup> proxyServer, boolean graceful) {
        synchronized (SERVER_REGISTRATION_LOCK) {
            boolean wasRegistered = registeredServers.remove(proxyServer);
            if (!wasRegistered) {
                logger.warn("Attempted to unregister proxy server from thread pool group that it was not registered with. Was the proxy unregistered twice?");
            }
            if (registeredServers.isEmpty()) {
                logger.info("Proxy server unregistered from thread pool group. No proxy servers remain registered, so shutting down thread pool group.");
                close(graceful);
            } else {
                logger.info("Proxy server unregistered from thread pool group. Not shutting down thread pool group ({} proxy servers remain registered).", registeredServers.size());
            }
        }
    }
}