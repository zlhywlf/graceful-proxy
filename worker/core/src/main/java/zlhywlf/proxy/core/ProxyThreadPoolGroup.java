package zlhywlf.proxy.core;

public interface ProxyThreadPoolGroup<T> {
    void registerProxyServer(ProxyServer proxyServer);

    void unregisterProxyServer(ProxyServer proxyServer, boolean graceful);

    T getProxyToServerPool();

    T getBossPool();

    T getClientToProxyPool();
}
