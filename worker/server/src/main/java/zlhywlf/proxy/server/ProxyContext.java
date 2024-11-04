package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zlhywlf.proxy.core.ProxyThreadPoolGroup;
import zlhywlf.proxy.server.config.ProxyConfig;

import java.net.SocketAddress;

@Getter
@RequiredArgsConstructor
public class ProxyContext {
    private final ProxyConfig proxyConfig;
    private final ProxyThreadPoolGroup<EventLoopGroup> proxyThreadPoolGroup;
    private final SocketAddress requestedAddress;
}
