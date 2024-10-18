package zlhywlf.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.model.ProxyServerConfig;

public class DefaultProxyServer implements ProxyServer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyServer.class);

    private final ProxyServerConfig config;

    public DefaultProxyServer(ProxyServerConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        logger.info("Starting with config: {}", this.config);
    }
}
