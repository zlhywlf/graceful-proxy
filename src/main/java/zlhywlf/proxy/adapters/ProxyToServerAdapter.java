package zlhywlf.proxy.adapters;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;
import zlhywlf.proxy.core.ProxyUtils;

import java.net.InetSocketAddress;

public class ProxyToServerAdapter extends ProxyAdapter<HttpResponse, HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ProxyToServerAdapter.class);

    private final InetSocketAddress remoteAddress;
    private final Object connectLock = new Object();

    public ProxyToServerAdapter(ProxyServer context, ProxyAdapter<HttpRequest, HttpResponse> target, InetSocketAddress remoteAddress) {
        super(context, ProxyState.DISCONNECTED, target);
        this.remoteAddress = remoteAddress;
    }

    @Override
    public ChannelFuture write(Object msg) {
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
            .group(getContext().getProxyThreadPoolGroup().getProxyToServerPool())
            .channel(getTarget().getChannel().getClass())
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(@NonNull Channel channel) {
                    channel.pipeline().addLast("encoder", new HttpRequestEncoder());
                    channel.pipeline().addLast("decoder", new HttpResponseDecoder());
                    channel.pipeline().addLast("proxyToServerAdapter", ProxyToServerAdapter.this);
                }
            })
            .connect(remoteAddress).addListener(f -> {
                synchronized (connectLock) {
                    if (!f.isSuccess()) {
                        getTarget().getChannel().close();
                    }
                    become(ProxyState.AWAITING_INITIAL);
                    if (ProxyUtils.isCONNECT(initialRequest)) {
                        if (isRelay()) {
                            write(initialRequest);
                        }
                        getCtx().pipeline().remove("encoder");
                        getCtx().pipeline().remove("decoder");
                        setTunneling(true);
                        if (!isRelay()) {
                            logger.info("Responding with CONNECT successful");
                            FullHttpResponse response = ProxyUtils.createFullHttpResponse(HttpVersion.HTTP_1_1, new HttpResponseStatus(200, "Connection established"));
                            getTarget().writeToChannel(response);
                        }
                        getTarget().getCtx().pipeline().remove("decoder");
                        getTarget().getCtx().pipeline().remove("encoder");
                        getTarget().setTunneling(true);
                    } else {
                        write(initialRequest);
                    }
                    getTarget().getChannel().config().setAutoRead(true);
                    connectLock.notifyAll();
                }
            });
    }

    @Override
    public ProxyState readHttpInitial(HttpResponse msg) {
        getTarget().write(msg);
        return ProxyState.AWAITING_CHUNK;
    }
}
