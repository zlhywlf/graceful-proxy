package zlhywlf.proxy.server.config;

public record ProxyConfig(
    String name,
    int port,
    String eventLoopClass,
    String channelClass
) {
}
