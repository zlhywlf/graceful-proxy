package zlhywlf.proxy.server.adapters;

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

public class ClientToProxyAdapter extends AbsAdapter<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private volatile AbsAdapter<HttpResponse> server;

    public ClientToProxyAdapter(ProxyServer<Channel> context, ChannelPipeline pipeline, EventLoopGroup workerGroup) {
        super(context, ProxyState.AWAITING_INITIAL, workerGroup);
        logger.info("Configuring ChannelPipeline");
        pipeline.addLast("httpServerCodec", new HttpRequestDecoder());
        pipeline.addLast("HttpResponseEncoder", new HttpResponseEncoder());
        pipeline.addLast("ClientToProxyAdapter", this);
        logger.info("Created ClientToProxyAdapter");
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            logger.info("Disconnected");
            if (server.getChannel() != null && server.getChannel().isActive()) {
                server.getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.channelInactive(ctx);
        }
    }


    @Override
    public ProxyState readHttpInitial(HttpRequest httpRequest) {
        String hostAndPortStr = httpRequest.headers().get("Host");
        String[] hostPortArray = hostAndPortStr.split(":");
        String host = hostPortArray[0];
        int port = 80;
        if (hostPortArray.length > 1) {
            port = Integer.parseInt(hostPortArray[1]);
        }
        server = new ProxyToServerAdapter(getContext(), this, new InetSocketAddress(host, port));
        server.write(httpRequest);
        return httpRequest instanceof LastHttpContent ? ProxyState.AWAITING_INITIAL : ProxyState.AWAITING_CHUNK;
    }

    @Override
    public void readRaw(ByteBuf msg) {
        server.write(msg);
    }

    @Override
    public void readHTTPChunk(HttpContent chunk) {
        server.write(chunk);
    }

    @Override
    public ChannelFuture write(Object msg) {
        if (msg instanceof ReferenceCounted) {
            logger.info("Retaining reference counted message");
            ((ReferenceCounted) msg).retain();
        }
        return write0(msg);
    }
}
