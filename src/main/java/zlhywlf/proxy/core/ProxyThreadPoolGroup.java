package zlhywlf.proxy.core;

import io.netty.channel.EventLoopGroup;

public interface ProxyThreadPoolGroup {
    void registerProxyServer(ProxyServer proxyServer);

    void unregisterProxyServer(ProxyServer proxyServer, boolean graceful);

    EventLoopGroup getProxyToServerPool();

    EventLoopGroup getBossPool();

    EventLoopGroup getClientToProxyPool();
}
