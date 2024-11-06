package zlhywlf.proxy.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

import java.net.InetSocketAddress;

public class ProxyToServerAdapter extends AbsAdapter<HttpResponse, HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyToServerAdapter.class);

    private final InetSocketAddress remoteAddress;
    private final Object connectLock = new Object();

    public ProxyToServerAdapter(ProxyServer context, AbsAdapter<HttpRequest, HttpResponse> target, InetSocketAddress remoteAddress) {
        super(context, ProxyState.DISCONNECTED, target.getWorkerGroup(), target);
        this.remoteAddress = remoteAddress;
    }

    @Override
    public ChannelFuture write(Object msg) {
        logger.info("Requested write of {}", msg);
        if (is(ProxyState.DISCONNECTED) && msg instanceof HttpRequest msg0) {
            logger.info("Currently disconnected, connect and then write the message");
            connectAndWrite(msg0);
            return getTarget().getChannel().newSucceededFuture();
        }
        if (getCurrentState() == ProxyState.CONNECTING) {
            synchronized (connectLock) {
                if (getCurrentState() == ProxyState.CONNECTING) {
                    try {
                        connectLock.wait(30000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return super.write(msg);
    }

    public void connectAndWrite(HttpRequest initialRequest) {
        logger.info("Starting new connection to: {}", remoteAddress);
        getTarget().getChannel().config().setAutoRead(false);
        become(ProxyState.CONNECTING);
        new Bootstrap()
            .group(getWorkerGroup())
            .channel(getTarget().getChannel().getClass())
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast("httpRequestEncoder", new HttpRequestEncoder());
                    channel.pipeline().addLast("HttpResponseDecoder", new HttpResponseDecoder());
                    channel.pipeline().addLast("proxyToServerAdapter", ProxyToServerAdapter.this);
                }
            })
            .connect(remoteAddress).addListener(f -> {
                synchronized (connectLock) {
                    if (!f.isSuccess()) {
                        getTarget().getChannel().close();
                    }
                    become(ProxyState.AWAITING_INITIAL);
                    write(initialRequest);
                    getTarget().getChannel().config().setAutoRead(true);
                    connectLock.notifyAll();
                }
            });
    }

    @Override
    public ProxyState readHttpInitial(HttpResponse msg) {
        logger.info("Received raw response: {}", msg);
        getTarget().write(msg);
        return ProxyState.AWAITING_CHUNK;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Disconnected");
            if (getTarget().getChannel() != null && getTarget().getChannel().isActive()) {
                getTarget().getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.channelInactive(ctx);
        }
    }
}
