package zlhywlf.proxy.server.adapters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

@Getter
public abstract class AbsAdapter<T extends HttpObject> extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AbsAdapter.class);

    private volatile ProxyState currentState;
    private final ProxyServer<Channel> context;
    protected volatile Channel channel;
    private volatile ChannelHandlerContext ctx;
    private final EventLoopGroup workerGroup;
    private volatile long lastReadTime;

    public AbsAdapter(ProxyServer<Channel> context, ProxyState currentState, EventLoopGroup workerGroup) {
        this.context = context;
        this.currentState = currentState;
        this.workerGroup = workerGroup;
    }

    public void become(ProxyState state) {
        currentState = state;
    }

    public boolean is(ProxyState state) {
        return currentState == state;
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
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        try {
            this.ctx = ctx;
            channel = ctx.channel();
            context.registerChannel(channel);
        } finally {
            super.channelRegistered(ctx);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        try {
            context.unregisterChannel(ctx.channel());
        } finally {
            super.channelUnregistered(ctx);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        logger.info("Writability changed. Is writable: {}", channel.isWritable());
        try {
            if (channel.isWritable()) {
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

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            read(msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    public void read(Object msg) {
        logger.info("Reading: {}", msg);
        lastReadTime = System.currentTimeMillis();
        if (msg instanceof HttpObject msg0) {
            readHttp(msg0);
        } else if (msg instanceof ByteBuf) {
            readRaw((ByteBuf) msg);
        } else {
            throw new UnsupportedOperationException("Unsupported message type: " + msg.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    void readHttp(HttpObject msg) {
        ProxyState nextState = getCurrentState();
        switch (nextState) {
            case AWAITING_INITIAL -> {
                if (msg instanceof HttpMessage) {
                    nextState = readHttpInitial((T) msg);
                } else {
                    logger.info("Dropping message because HTTP object was not an HttpMessage. HTTP object may be orphaned content from a short-circuited response. Message: {}", msg);
                }
            }
            case AWAITING_CHUNK -> {
                HttpContent msg0 = (HttpContent) msg;
                readHTTPChunk(msg0);
                nextState = msg instanceof LastHttpContent ? ProxyState.AWAITING_INITIAL : ProxyState.AWAITING_CHUNK;
            }
        }
        become(nextState);
    }

    public abstract ProxyState readHttpInitial(T msg);

    public abstract void readRaw(ByteBuf msg);

    public abstract void readHTTPChunk(HttpContent chunk);

    public ChannelFuture write(Object msg) {
        if (msg instanceof ReferenceCounted) {
            logger.info("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }
        logger.info("Writing: {}", msg);
        if (msg instanceof HttpObject msg0) {
            return writeHttp(msg0);
        }
        return writeToChannel(msg);
    }

    public ChannelFuture writeHttp(HttpObject msg) {
        if (msg instanceof LastHttpContent) {
            channel.write(msg);
            logger.info("Writing an empty buffer to signal the end of our chunked transfer");
            return writeToChannel(Unpooled.EMPTY_BUFFER);
        }
        return writeToChannel(msg);
    }

    public ChannelFuture writeToChannel(final Object msg) {
        return channel.writeAndFlush(msg);
    }
}
