package zlhywlf.proxy.core;

import io.netty.channel.Channel;

public interface ProxyServer {
    void registerChannel(Channel channel);

    void unregisterChannel(Channel channel);
}
