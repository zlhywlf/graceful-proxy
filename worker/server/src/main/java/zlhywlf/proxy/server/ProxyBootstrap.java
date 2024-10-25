package zlhywlf.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.enums.EventLoopEnum;
import zlhywlf.proxy.core.enums.TransportProtocolEnum;
import zlhywlf.proxy.server.adapters.InitializeAdapter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Properties;

public class ProxyBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ProxyBootstrap.class);

    private volatile SocketAddress localAddress;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Class<? extends ServerChannel> channelClass;


    public ProxyBootstrap() {
        Properties props = new Properties();
        props.put("proxy.protocol", "tcp".toUpperCase());
        props.put("proxy.eventLoopType", "nio".toUpperCase());
        TransportProtocolEnum protocol = EnumUtils.getEnum(TransportProtocolEnum.class, props.get("proxy.protocol").toString());
        switch (protocol) {
            case TCP:
                EventLoopEnum eventLoopType = EnumUtils.getEnum(EventLoopEnum.class, props.get("proxy.eventLoopType").toString());
                if (eventLoopType == EventLoopEnum.NIO) {
                    bossGroup = new NioEventLoopGroup(1);
                    workerGroup = new NioEventLoopGroup();
                    channelClass = NioServerSocketChannel.class;
                }
                break;
            case UDP:
                throw new RuntimeException(String.format("Unimplemented TransportProtocol: %1$s", protocol));
            default:
                throw new RuntimeException(String.format("Unknown TransportProtocol: %1$s", protocol));
        }
    }

    public void start() {
        validate();
        doStart();
    }

    public ProxyBootstrap bind(int port) {
        logger.info("About to start server on port: {}", port);
        localAddress = new InetSocketAddress(port);
        return this;
    }

    public void validate() {
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }
        if (bossGroup == null) {
            throw new IllegalStateException("eventLoop not set");
        }
    }

    private void doStart() {
        ChannelFuture cf = new ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(channelClass)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new InitializeAdapter<SocketChannel>())
            .bind(localAddress)
            .awaitUninterruptibly();
        Throwable cause = cf.cause();
        if (cause != null) {
            stop();
            throw new RuntimeException(cause);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "graceful-proxy-stop"));
        logger.info("Proxy started at address: {}", cf.channel().localAddress());
    }

    public void stop() {
        logger.info("About to shutdown");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
