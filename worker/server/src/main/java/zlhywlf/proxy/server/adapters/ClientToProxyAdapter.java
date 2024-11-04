package zlhywlf.proxy.server.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;

public class ClientToProxyAdapter extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final ProxyServer server;
    private Channel clientChannel;
    private Channel remoteChannel;

    public ClientToProxyAdapter(ProxyServer server, ChannelPipeline pipeline) {
        this.server = server;
        pipeline.addLast("httpServerCodec", new HttpRequestDecoder());
        pipeline.addLast("ClientToProxyAdapter", this);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        clientChannel = ctx.channel();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest httpRequest) {
            String hostAndPortStr = httpRequest.headers().get("Host");
            String[] hostPortArray = hostAndPortStr.split(":");
            String host = hostPortArray[0];
            int port = 80;
            if (hostPortArray.length > 1) {
                port = Integer.parseInt(hostPortArray[1]);
            }
            logger.info("host {} port {}", host, port);
            clientChannel.config().setAutoRead(false);
            ChannelFuture cf = new Bootstrap()
                .group((EventLoopGroup) server.getProxyThreadPoolGroup().getProxyToServerPool())
                .channel(clientChannel.getClass())
                .handler(new HttpRequestEncoder())
                .connect(host, port).addListener(f -> {
                    if (!f.isSuccess()) {
                        clientChannel.close();
                    }
                    remoteChannel.write(msg);
                    remoteChannel.pipeline().remove(HttpRequestEncoder.class);
                    clientChannel.config().setAutoRead(true);
                    remoteChannel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            clientChannel.writeAndFlush(msg);
                        }

                        @Override
                        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                            if (clientChannel != null && clientChannel.isActive()) {
                                clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    });
                });
            remoteChannel = cf.channel();
            clientChannel.pipeline().remove(HttpRequestDecoder.class);
            return;
        }
        if (!remoteChannel.isActive()) {
            Thread.sleep(5000);
        }
        remoteChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
