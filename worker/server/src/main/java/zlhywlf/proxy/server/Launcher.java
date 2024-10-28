package zlhywlf.proxy.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        logger.info("Running with args: {}", Arrays.asList(args));
        final int port = 8080;
        new ProxyBootstrap().group(new ProxyThreadPoolGroup()).bind(port).start();
    }
}
