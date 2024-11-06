package zlhywlf.proxy.core;

public enum ProxyState {
    AWAITING_INITIAL,
    DISCONNECTED,
    CONNECTING,
    AWAITING_CHUNK,
    NEGOTIATING_CONNECT
}
