package zlhywlf.proxy.core;

public record ProxyConfig(
    String name,
    int port,
    String eventLoopClass,
    String channelClass
) {
}
