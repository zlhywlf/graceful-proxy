package zlhywlf.proxy.server.adapters;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;

@ChannelHandler.Sharable
public class ProxyServerAdapter<I extends HttpObject> extends SimpleChannelInboundHandler<I> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyServerAdapter.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, I msg) throws Exception {
        logger.info("Reading: {}", msg);
        if (msg instanceof HttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK, Unpooled.copiedBuffer((new Date(System.currentTimeMillis()) + "\n").getBytes()));
            response.headers().set(CONTENT_TYPE, TEXT_PLAIN).setInt(CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                response.headers().set(CONNECTION, CLOSE);
            }
            ChannelFuture f = ctx.writeAndFlush(response);
            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
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
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }
}
