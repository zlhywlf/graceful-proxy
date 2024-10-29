package zlhywlf.proxy.server.adapters;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class BytesReadAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BytesReadAdapter.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof ByteBuf) {
                logger.info("Read : {}", ((ByteBuf) msg).readableBytes());
            }
        } catch (Exception t) {
            logger.warn("Unable to record bytesRead", t);
        } finally {
            super.channelRead(ctx, msg);
        }
    }
}

