package zlhywlf.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.model.ProxyServerConfig;

import java.util.Arrays;

public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        logger.info("Running with args: {}", Arrays.asList(args));
        final int port = 8080;
        logger.info("About to start server on port: {}", port);
        ProxyServerConfig config = ProxyServerConfig.builder().port(port).build();
        new DefaultProxyServer(config).start();
    }
}
