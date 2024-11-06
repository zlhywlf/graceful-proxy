package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyThreadPoolGroup;
import zlhywlf.proxy.core.ProxyConfig;

import java.lang.reflect.RecordComponent;
import java.net.InetSocketAddress;
import java.util.Objects;

@Getter
public class ProxyBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ProxyBootstrap.class);

    public static final Option helpOption = new Option("h", "help", false, "display command line help.");
    public static final Option notAllowLocalOnlyOption = new Option(null, "notAllowLocalOnly", false, "not binding only to localhost.");
    public static final Options options = new Options() {{
        this.addOption(helpOption);
        this.addOption(notAllowLocalOnlyOption);
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
    private ProxyThreadPoolGroup proxyThreadPoolGroup;
    private InetSocketAddress requestedAddress;
    private boolean allowLocalOnly = true;

    public ProxyBootstrap(String[] args) {
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public ProxyBootstrap(CommandLine cmd) {
        this.cmd = cmd;
    }

    public ProxyServer start() {
        if (cmd.hasOption(helpOption)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("gracefulProxy", options);
            return null;
        }
        return doStart();
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

    public ProxyBootstrap withProxyThreadPoolGroup(ProxyThreadPoolGroup proxyThreadPoolGroup) {
        this.proxyThreadPoolGroup = proxyThreadPoolGroup;
        return this;
    }

    public ProxyBootstrap withRequestedAddress(InetSocketAddress requestedAddress) {
        this.requestedAddress = requestedAddress;
        return this;
    }

    public ProxyBootstrap withAllowLocalOnly(boolean allowLocalOnly) {
        this.allowLocalOnly = allowLocalOnly;
        return this;
    }

    private ProxyServer doStart() {
        ProxyConfig proxyConfig = createProxyConfig();
        ProxyThreadPoolGroup proxyThreadPoolGroup = Objects.requireNonNullElseGet(this.proxyThreadPoolGroup, () -> {
            int proxyThreadPoolGroupId = DefaultProxyThreadPoolGroup.proxyThreadPoolGroupCount.getAndIncrement();
            String category = StringUtils.joinWith("-", proxyConfig.name(), proxyThreadPoolGroupId);
            String eventLoopClassName = proxyConfig.eventLoopClass();
            EventLoopGroup bossPool = ProxyThreadFactory.create(category + "-boss", eventLoopClassName, 1);
            EventLoopGroup clientToProxyPool = ProxyThreadFactory.create(category + "-clientToProxy", eventLoopClassName, 0);
            EventLoopGroup proxyToServerPool = ProxyThreadFactory.create(category + "-proxyToServer", eventLoopClassName, 0);
            return new DefaultProxyThreadPoolGroup(proxyThreadPoolGroupId, bossPool, clientToProxyPool, proxyToServerPool);
        });
        return new DefaultProxyServer(proxyConfig, proxyThreadPoolGroup, determineListenAddress(proxyConfig)).start();
    }

    private InetSocketAddress determineListenAddress(ProxyConfig proxyConfig) {
        if (requestedAddress != null) {
            return requestedAddress;
        } else {
            if (allowLocalOnly) {
                return new InetSocketAddress("127.0.0.1", proxyConfig.port());
            } else {
                return new InetSocketAddress(proxyConfig.port());
            }
        }
    }
}
