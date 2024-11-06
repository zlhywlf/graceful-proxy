package zlhywlf.proxy.core;

import io.netty.channel.EventLoopGroup;

public interface ProxyThreadPoolGroup {
    void registerProxyServer(Server server);

    void unregisterProxyServer(Server server, boolean graceful);

    EventLoopGroup getProxyToServerPool();

    EventLoopGroup getBossPool();

    EventLoopGroup getClientToProxyPool();
}
