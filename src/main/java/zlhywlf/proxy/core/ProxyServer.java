package zlhywlf.proxy.core;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

public interface ProxyServer extends Server {
    void registerChannel(Channel channel);

    void unregisterChannel(Channel channel);

    ProxyThreadPoolGroup getProxyThreadPoolGroup();

    void stop();

    InetSocketAddress getBoundAddress();
}
