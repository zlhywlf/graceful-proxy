package zlhywlf.proxy.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zlhywlf.proxy.server.config.ProxyConfig;

import java.net.SocketAddress;

@Getter
@RequiredArgsConstructor
public class ProxyContext {
    private final ProxyConfig proxyConfig;
    private final ProxyThreadPoolGroup proxyThreadPoolGroup;
    private final SocketAddress requestedAddress;
}
