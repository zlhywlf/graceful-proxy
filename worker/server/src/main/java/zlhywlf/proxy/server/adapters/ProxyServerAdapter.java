package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.TypeParameterMatcher;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyServer;
import zlhywlf.proxy.server.ProxyState;

@Getter
public abstract class ProxyServerAdapter<T extends HttpObject> extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServerAdapter.class);

    private final ProxyServer server;
    private volatile ProxyState currentState;
    private final TypeParameterMatcher matcher;
    protected volatile Channel channel;

    public ProxyServerAdapter(ProxyServer server, ProxyState state) {
        this.server = server;
        this.matcher = TypeParameterMatcher.find(this, ProxyServerAdapter.class, "T");
        become(state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(@NonNull ChannelHandlerContext ctx, @NonNull Object msg) throws Exception {
        try {
            if (!this.matcher.match(msg)) {
                logger.warn("Unsupported message type: " + msg.getClass().getName());
//                throw new UnsupportedOperationException("Unsupported message type: " + msg.getClass().getName());
            }
            channelRead0((T) msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    protected void channelRead0(T msg) {
        logger.info("Reading: {}", msg);
        ProxyState nextState = currentState;
        switch (currentState) {
            case AWAITING_INITIAL:
                if (msg instanceof HttpMessage) {
                    nextState = readHttpInitial(msg);
                    break;
                }
                logger.info("Dropping message because HTTP object was not an HttpMessage. HTTP object may be orphaned content from a short-circuited response. Message: {}", msg);
                break;
            case AWAITING_CHUNK:
                if (msg instanceof HttpContent) {
                    readHTTPChunk((HttpContent) msg);
                }
                break;
        }
        become(nextState);
    }

    public abstract ProxyState readHttpInitial(T msg);

    public abstract void readHTTPChunk(HttpContent chunk);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    public void become(ProxyState state) {
        currentState = state;
    }
}
