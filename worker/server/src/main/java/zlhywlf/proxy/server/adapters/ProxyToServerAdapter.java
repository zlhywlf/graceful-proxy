package zlhywlf.proxy.server.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

import java.net.InetSocketAddress;


public class ProxyToServerAdapter extends AbsAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ProxyToServerAdapter.class);

    private final AbsAdapter client;
    private final InetSocketAddress remoteAddress;
    private final EventLoopGroup workerGroup;

    public ProxyToServerAdapter(ProxyServer<Channel> context, AbsAdapter client, EventLoopGroup workerGroup, InetSocketAddress remoteAddress) {
        super(context, ProxyState.DISCONNECTED);
        this.client = client;
        this.remoteAddress = remoteAddress;
        this.workerGroup = workerGroup;
    }

    @Override
    public ChannelFuture write(Object msg) {
        logger.info("Requested write of {}", msg);
        if (msg instanceof ReferenceCounted) {
            logger.info("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }
        if (is(ProxyState.DISCONNECTED) && msg instanceof HttpRequest msg0) {
            logger.info("Currently disconnected, connect and then write the message");
            connectAndWrite(msg0);
            return client.getChannel().newSucceededFuture();
        }
        return null;
    }

    public void connectAndWrite(HttpRequest initialRequest) {
        logger.info("Starting new connection to: {}", remoteAddress);
        client.getChannel().config().setAutoRead(false);
        new Bootstrap()
            .group(workerGroup)
            .channel(client.getChannel().getClass())
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast("httpRequestEncoder", new HttpRequestEncoder());
                    channel.pipeline().addLast("proxyToServerAdapter", ProxyToServerAdapter.this);
                }
            })
            .connect(remoteAddress).addListener(f -> {
                if (!f.isSuccess()) {
                    client.getChannel().close();
                }
                getChannel().write(initialRequest);
                getCtx().pipeline().remove(HttpRequestEncoder.class);
                client.getChannel().config().setAutoRead(true);
            });
        client.getCtx().pipeline().remove(HttpRequestDecoder.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        client.getChannel().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Disconnected");
            if (client.getChannel() != null && client.getChannel().isActive()) {
                client.getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.channelInactive(ctx);
        }
    }
}
