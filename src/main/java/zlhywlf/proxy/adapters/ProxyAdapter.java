package zlhywlf.proxy.adapters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

@Getter
@Setter
public abstract class ProxyAdapter<T extends HttpObject, K extends HttpObject> extends BaseAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProxyAdapter.class);

    private volatile ProxyState currentState;
    private volatile long lastReadTime;
    private volatile ProxyAdapter<K, T> target;
    private volatile boolean tunneling;

    public ProxyAdapter(ProxyServer context, ProxyState currentState) {
        this(context, currentState, null);
    }

    public ProxyAdapter(ProxyServer context, ProxyState currentState, ProxyAdapter<K, T> target) {
        super(context);
        this.currentState = currentState;
        this.target = target;
    }

    @Override
    public void disconnected() {
        logger.info("Disconnected");
        if (target.getChannel() != null && target.getChannel().isActive()) {
            target.getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void become(ProxyState state) {
        currentState = state;
    }

    public boolean is(ProxyState state) {
        return currentState == state;
    }

    public void read(Object msg) {
        logger.info("Reading: {}", msg);
        lastReadTime = System.currentTimeMillis();
        if (tunneling || msg instanceof ByteBuf) {
            readRaw((ByteBuf) msg);
        } else if (msg instanceof HttpObject msg0) {
            readHttp(msg0);
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
                readHttpChunk(msg0);
                nextState = msg instanceof LastHttpContent ? ProxyState.AWAITING_INITIAL : ProxyState.AWAITING_CHUNK;
            }
        }
        become(nextState);
    }

    public abstract ProxyState readHttpInitial(T msg);

    public void readRaw(ByteBuf msg) {
        getTarget().write(msg);
    }

    public void readHttpChunk(HttpContent msg) {
        getTarget().write(msg);
    }

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
            getChannel().write(msg);
            logger.info("Writing an empty buffer to signal the end of our chunked transfer");
            return writeToChannel(Unpooled.EMPTY_BUFFER);
        }
        return writeToChannel(msg);
    }

    public ChannelFuture writeToChannel(final Object msg) {
        return getChannel().writeAndFlush(msg);
    }
}
