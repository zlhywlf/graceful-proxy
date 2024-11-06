package zlhywlf.proxy.core;

import io.netty.channel.Channel;

public interface ProxyServer extends Server {
    void registerChannel(Channel channel);

    void unregisterChannel(Channel channel);

    ProxyThreadPoolGroup getProxyThreadPoolGroup();
}
