package zlhywlf.proxy.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.adapters.InitializeAdapter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ProxyBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(ProxyBootstrap.class);

    private volatile SocketAddress localAddress;
    private ProxyThreadPoolGroup group;

    public void start() {
        validate();
        doStart();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "graceful-proxy-stop"));
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
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
    }

    private void doStart() {
        ChannelFuture cf = new ServerBootstrap()
            .group(group.getBossGroup(), group.getWorkerGroup())
            .channel(group.getChannelClazz())
            .option(ChannelOption.SO_BACKLOG, 1024)
            .childHandler(new InitializeAdapter<SocketChannel>())
            .bind(localAddress)
            .awaitUninterruptibly();
        Throwable cause = cf.cause();
        if (cause != null) {
            stop();
            throw new RuntimeException(cause);
        }
        logger.info("Proxy started at address: {}", cf.channel().localAddress());
    }

    public void stop() {
        logger.info("About to shutdown");
        group.getBossGroup().shutdownGracefully();
        group.getWorkerGroup().shutdownGracefully();
    }

    public ProxyBootstrap group(ProxyThreadPoolGroup group) {
        this.group = group;
        return this;
    }
}
