package zlhywlf.proxy.server;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Objects;

public class ProxyBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ProxyBootstrap.class);

    public static final Option helpOption = new Option("h", "help", false, "display command line help.");
    public static final Options options = new Options() {{
        this.addOption(helpOption);
        this.addOption(Option
            .builder("n")
            .longOpt("name")
            .argName("GracefulProxy")
            .desc("the service name")
            .hasArg()
            .build());
        this.addOption(Option
            .builder("p")
            .longOpt("port")
            .argName("8080")
            .desc("the proxy service's port")
            .hasArg()
            .type(Integer.class)
            .build());
        this.addOption(Option
            .builder("e")
            .longOpt("eventLoopClass")
            .argName("io.netty.channel.nio.NioEventLoopGroup")
            .desc("the event loop class")
            .hasArg()
            .build());
        this.addOption(Option
            .builder("c")
            .longOpt("channelClass")
            .argName("io.netty.channel.socket.nio.NioServerSocketChannel")
            .desc("the channel class")
            .hasArg()
            .build());
    }};

    private final CommandLine cmd;


    public ProxyBootstrap(String[] args) {
        logger.info("Running with args: {}", Arrays.asList(args));
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        if (cmd.hasOption(helpOption)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("gracefulProxy", options);
            return;
        }
        new ProxyThreadPoolGroup(createProxyConfig()).start();
    }

    public ProxyConfig createProxyConfig() {
        try {
            RecordComponent[] components = ProxyConfig.class.getRecordComponents();
            Object[] paramValues = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                Option option = Objects.requireNonNull(options.getOption(component.getName()));
                paramValues[i] = Objects.requireNonNull(cmd.getParsedOptionValue(option, option.getConverter().apply(option.getArgName())));
            }
            return ConstructorUtils.invokeConstructor(ProxyConfig.class, paramValues);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
