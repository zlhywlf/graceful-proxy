package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

@Getter
public abstract class AbsAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(AbsAdapter.class);

    private volatile ProxyState currentState;
    private final ProxyServer<Channel> context;
    protected volatile Channel channel;
    private volatile ChannelHandlerContext ctx;
    private final EventLoopGroup workerGroup;

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

    public abstract ChannelFuture write(Object msg);

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
}
