package zlhywlf.proxy.server.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import zlhywlf.proxy.server.ProxyServer;
import zlhywlf.proxy.server.ProxyState;

import java.net.InetSocketAddress;

public class ProxyToServerAdapter extends ProxyServerAdapter<HttpResponse> {
    private final ClientToProxyAdapter clientToProxyAdapter;
    private final String serverHostAndPort;
    private volatile InetSocketAddress remoteAddress;
    private volatile InetSocketAddress localAddress;


    public ProxyToServerAdapter(ClientToProxyAdapter clientToProxyAdapter, String serverHostAndPort, ProxyServer server) {
        super(server, ProxyState.DISCONNECTED);
        this.clientToProxyAdapter = clientToProxyAdapter;
        this.serverHostAndPort = serverHostAndPort;
        String[] split = serverHostAndPort.split(":");
        remoteAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        localAddress = null;
    }

    @Override
    public ProxyState readHttpInitial(HttpResponse msg) {
        return null;
    }

    @Override
    public void readHTTPChunk(HttpContent chunk) {

    }

    public void write(HttpRequest msg) {
        if (getCurrentState() == ProxyState.DISCONNECTED) {
            new Bootstrap()
                .group(getServer().getContext().getProxyThreadPoolGroup().getProxyToServerPool())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 40000).handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("httpServerCodec", new HttpServerCodec());
                        pipeline.addLast("ProxyToServerAdapter", ProxyToServerAdapter.this);
                    }
                }).connect(new InetSocketAddress(remoteAddress.getPort())).awaitUninterruptibly();
        }
    }

    public void write(HttpContent msg) {
        getChannel().write(msg);
        getChannel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(l -> {
            System.out.println(l.isSuccess() ? "success" : l.cause());
        });
    }
}
