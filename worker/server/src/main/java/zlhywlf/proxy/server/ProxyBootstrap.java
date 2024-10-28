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
        if (!group.isStopped()) {
            validate();
            doStart();
            return;
        }
        throw new IllegalStateException("Attempted to start proxy, but proxy's server group is already stopped");
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
            .bind(localAddress).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) group.registerChannel(f.channel());
            })
            .awaitUninterruptibly();
        Throwable cause = cf.cause();
        if (cause != null) {
            abort();
            throw new RuntimeException(cause);
        }
        Runtime.getRuntime().addShutdownHook(group.getJvmShutdownHook());
        logger.info("Proxy started at address: {}", cf.channel().localAddress());
    }

    public void abort() {
        group.doStop(false);
    }

    public ProxyBootstrap group(ProxyThreadPoolGroup group) {
        this.group = group;
        return this;
    }
}
