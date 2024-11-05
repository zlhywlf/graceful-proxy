package zlhywlf.proxy.server.adapters;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

import java.net.InetSocketAddress;

@Getter
public class ClientToProxyAdapter extends AbsAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final EventLoopGroup workerGroup;
    private volatile long lastReadTime;
    private volatile AbsAdapter server;

    public ClientToProxyAdapter(ProxyServer<Channel> context, ChannelPipeline pipeline, EventLoopGroup workerGroup) {
        super(context, ProxyState.AWAITING_INITIAL);
        this.workerGroup = workerGroup;
        logger.info("Configuring ChannelPipeline");
        pipeline.addLast("httpServerCodec", new HttpRequestDecoder());
        pipeline.addLast("ClientToProxyAdapter", this);
        logger.info("Created ClientToProxyAdapter");
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Disconnected");
            if (server.getChannel() != null && server.getChannel().isActive()) {
                server.getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.channelInactive(ctx);
        }
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
                    if (server.getChannel() == null || !server.getChannel().isActive()) {
                        Thread.sleep(5000);
                    }
                    server.getChannel().writeAndFlush(msg);
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
        server = new ProxyToServerAdapter(getContext(), this, workerGroup, new InetSocketAddress(host, port));
        server.write(httpRequest);
        return ProxyState.AWAITING_INITIAL;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return null;
    }
}
