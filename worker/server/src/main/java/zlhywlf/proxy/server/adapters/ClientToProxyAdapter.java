package zlhywlf.proxy.server.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

@Getter
public class ClientToProxyAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final ProxyServer<Channel> server;
    private Channel clientChannel;
    private Channel remoteChannel;
    private final EventLoopGroup workerGroup;
    private volatile ChannelHandlerContext ctx;
    private volatile long lastReadTime;
    private volatile ProxyState currentState;

    public ClientToProxyAdapter(ProxyServer<Channel> server, ChannelPipeline pipeline, EventLoopGroup workerGroup) {
        this.server = server;
        this.workerGroup = workerGroup;
        currentState = ProxyState.AWAITING_INITIAL;
        logger.info("Configuring ChannelPipeline");
        pipeline.addLast("httpServerCodec", new HttpRequestDecoder());
        pipeline.addLast("ClientToProxyAdapter", this);
        logger.info("Created ClientToProxyAdapter");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Connected");
        } finally {
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            read(msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        try {
            this.ctx = ctx;
            clientChannel = ctx.channel();
            server.registerChannel(clientChannel);
        } finally {
            super.channelRegistered(ctx);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        try {
            server.unregisterChannel(ctx.channel());
        } finally {
            super.channelUnregistered(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Disconnected");
            if (remoteChannel != null && remoteChannel.isActive()) {
                remoteChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.channelInactive(ctx);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        logger.info("Writability changed. Is writable: {}", clientChannel.isWritable());
        try {
            if (clientChannel.isWritable()) {
                logger.info("Became writeable");
            } else {
                logger.info("Became saturated");
            }
        } finally {
            super.channelWritabilityChanged(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    public void read(Object msg) throws InterruptedException {
        logger.info("Reading: {}", msg);
        lastReadTime = System.currentTimeMillis();
        if (msg instanceof HttpObject msg0) {
            readHttp(msg0);
        } else {
            throw new UnsupportedOperationException("Unsupported message type: " + msg.getClass().getName());
        }
    }

    void readHttp(HttpObject msg) throws InterruptedException {
        ProxyState nextState = getCurrentState();
        switch (nextState) {
            case AWAITING_INITIAL -> {
                if (msg instanceof HttpMessage) {
                    nextState = readHttpInitial((HttpRequest) msg);
                } else {
                    if (!remoteChannel.isActive()) {
                        Thread.sleep(5000);
                    }
                    remoteChannel.writeAndFlush(msg);
                    logger.info("Dropping message because HTTP object was not an HttpMessage. HTTP object may be orphaned content from a short-circuited response. Message: {}", msg);
                }
            }
        }
        become(nextState);
    }

    ProxyState readHttpInitial(HttpRequest httpRequest) {
        String hostAndPortStr = httpRequest.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        int port = 80;
        if (hostPortArray.length > 1) {
            port = Integer.parseInt(hostPortArray[1]);
        }
        logger.info("host {} port {}", host, port);
        clientChannel.config().setAutoRead(false);
        ChannelFuture cf = new Bootstrap()
            .group(workerGroup)
            .channel(clientChannel.getClass())
            .handler(new HttpRequestEncoder())
            .connect(host, port).addListener(f -> {
                if (!f.isSuccess()) {
                    clientChannel.close();
                }
                remoteChannel.write(httpRequest);
                remoteChannel.pipeline().remove(HttpRequestEncoder.class);
                clientChannel.config().setAutoRead(true);
                remoteChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        clientChannel.writeAndFlush(msg);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        if (clientChannel != null && clientChannel.isActive()) {
                            clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                });
            });
        remoteChannel = cf.channel();
        this.ctx.pipeline().remove(HttpRequestDecoder.class);
        return ProxyState.AWAITING_INITIAL;
    }

    public void become(ProxyState state) {
        currentState = state;
    }
}
