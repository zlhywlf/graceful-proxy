package zlhywlf.proxy.core;

public interface ProxyServer<T> {
    void registerChannel(T channel);

    void unregisterChannel(T channel);
}
