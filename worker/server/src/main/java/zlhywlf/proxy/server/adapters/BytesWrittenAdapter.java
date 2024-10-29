package zlhywlf.proxy.server.adapters;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class BytesWrittenAdapter extends ChannelOutboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BytesWrittenAdapter.class);

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            if (msg instanceof ByteBuf) {
                logger.info("Written : {}", ((ByteBuf) msg).readableBytes());
            }
        } catch (Throwable t) {
            logger.warn("Unable to record bytesRead", t);
        } finally {
            super.write(ctx, msg, promise);
        }
    }
}
