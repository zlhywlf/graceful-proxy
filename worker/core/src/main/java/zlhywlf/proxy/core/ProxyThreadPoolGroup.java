package zlhywlf.proxy.core;

public interface ProxyThreadPoolGroup<T, K> {
    void registerProxyServer(ProxyServer<K> proxyServer);

    void unregisterProxyServer(ProxyServer<K> proxyServer, boolean graceful);

    T getProxyToServerPool();

    T getBossPool();

    T getClientToProxyPool();
}
