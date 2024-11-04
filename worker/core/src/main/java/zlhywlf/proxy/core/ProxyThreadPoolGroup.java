package zlhywlf.proxy.core;

public interface ProxyThreadPoolGroup<T> {
    void registerProxyServer(ProxyServer<T> proxyServer);

    void unregisterProxyServer(ProxyServer<T> proxyServer, boolean graceful);

    T getProxyToServerPool();

    T getBossPool();

    T getClientToProxyPool();
}
