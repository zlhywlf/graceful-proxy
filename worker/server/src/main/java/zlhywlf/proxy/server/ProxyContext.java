package zlhywlf.proxy.server;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.SocketAddress;

@Getter
@RequiredArgsConstructor
public class ProxyContext {
    private final ProxyThreadPoolGroup server;
    private final int proxyThreadPoolGroupId;
    private final ProxyConfig proxyConfig;
    private final SocketAddress requestedAddress;
}
