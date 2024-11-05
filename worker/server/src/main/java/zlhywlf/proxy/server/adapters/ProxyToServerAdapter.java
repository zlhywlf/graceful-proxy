package zlhywlf.proxy.server.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

import java.net.InetSocketAddress;


public class ProxyToServerAdapter extends AbsAdapter<HttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyToServerAdapter.class);

    private final AbsAdapter<HttpRequest> client;
    private final InetSocketAddress remoteAddress;
    private final Object connectLock = new Object();

    public ProxyToServerAdapter(ProxyServer<Channel> context, AbsAdapter<HttpRequest> client, InetSocketAddress remoteAddress) {
        super(context, ProxyState.DISCONNECTED, client.getWorkerGroup());
        this.client = client;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public ChannelFuture write(Object msg) {
        logger.info("Requested write of {}", msg);
        if (is(ProxyState.DISCONNECTED) && msg instanceof HttpRequest msg0) {
            logger.info("Currently disconnected, connect and then write the message");
            connectAndWrite(msg0);
            return client.getChannel().newSucceededFuture();
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
        client.getChannel().config().setAutoRead(false);
        become(ProxyState.CONNECTING);
        new Bootstrap()
            .group(getWorkerGroup())
            .channel(client.getChannel().getClass())
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
                        client.getChannel().close();
                    }
                    become(ProxyState.AWAITING_INITIAL);
                    write(initialRequest);
                    client.getChannel().config().setAutoRead(true);
                    connectLock.notifyAll();
                }
            });
    }

    @Override
    public ProxyState readHttpInitial(HttpResponse msg) {
        logger.info("Received raw response: {}", msg);
        client.write(msg);
        return ProxyState.AWAITING_CHUNK;
    }

    @Override
    public void readRaw(ByteBuf msg) {
        client.write(msg);
    }

    @Override
    public void readHTTPChunk(HttpContent chunk) {
        client.write(chunk);
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
