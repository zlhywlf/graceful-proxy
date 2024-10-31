package zlhywlf.proxy.server;

public record ProxyConfig(
    String name,
    int port,
    String eventLoopClass,
    String channelClass
) {
}
