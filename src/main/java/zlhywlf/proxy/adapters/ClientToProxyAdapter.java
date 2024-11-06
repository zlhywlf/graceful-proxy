package zlhywlf.proxy.adapters;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;

import java.net.InetSocketAddress;

public class ClientToProxyAdapter extends AbsAdapter<HttpRequest, HttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    public ClientToProxyAdapter(ProxyServer context, ChannelPipeline pipeline, EventLoopGroup workerGroup) {
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
            if (getTarget().getChannel() != null && getTarget().getChannel().isActive()) {
                getTarget().getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
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
        setTarget(new ProxyToServerAdapter(getContext(), this, new InetSocketAddress(host, port)));
        getTarget().write(httpRequest);
        return httpRequest instanceof LastHttpContent ? ProxyState.AWAITING_INITIAL : ProxyState.AWAITING_CHUNK;
    }
}
