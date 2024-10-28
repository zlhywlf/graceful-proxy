package zlhywlf.proxy.server.adapters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@ChannelHandler.Sharable
public class TimeAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TimeAdapter.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        logger.info("receive : {}{}", new String(bytes), this);
        ByteBuf res = Unpooled.copiedBuffer((new Date(System.currentTimeMillis()) + "\n").getBytes());
        ctx.writeAndFlush(res);
    }
}
