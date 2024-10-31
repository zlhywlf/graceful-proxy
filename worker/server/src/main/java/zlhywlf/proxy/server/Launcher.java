package zlhywlf.proxy.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Launcher {
    private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) throws ParseException {
        logger.info("Running with args: {}", Arrays.asList(args));
        CommandLine cmd = new DefaultParser().parse(ProxyBootstrap.options, args);
        new ProxyBootstrap(cmd).withAllowLocalOnly(!cmd.hasOption(ProxyBootstrap.notAllowLocalOnlyOption)).start();
    }
}
