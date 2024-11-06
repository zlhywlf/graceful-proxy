package zlhywlf.proxy.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.core.ProxyState;
import zlhywlf.proxy.core.ProxyUtils;

import java.net.InetSocketAddress;

import static zlhywlf.proxy.core.ProxyState.*;

public class ClientToProxyAdapter extends ProxyAdapter<HttpRequest, HttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    public ClientToProxyAdapter(ProxyServer context, ChannelPipeline pipeline) {
        super(context, AWAITING_INITIAL);
        logger.info("Configuring ChannelPipeline");
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("ClientToProxyAdapter", this);
        logger.info("Created ClientToProxyAdapter");
    }

    @Override
    public ProxyState readHttpInitial(HttpRequest httpRequest) {
        String hostAndPortStr = identifyHostAndPort(httpRequest);
        logger.info("Finding ProxyToServer for: {}", hostAndPortStr);
        if (ProxyUtils.isCONNECT(httpRequest) || getTarget() == null) {
            String[] hostPortArray = hostAndPortStr.split(":");
            String host = hostPortArray[0];
            int port = 80;
            if (hostPortArray.length > 1) {
                port = Integer.parseInt(hostPortArray[1]);
            }
            setTarget(new ProxyToServerAdapter(getContext(), this, new InetSocketAddress(host, port)));
        }
        getTarget().write(httpRequest);
        if (ProxyUtils.isCONNECT(httpRequest)) {
            return NEGOTIATING_CONNECT;
        }
        return httpRequest instanceof LastHttpContent ? AWAITING_INITIAL : AWAITING_CHUNK;
    }

    private String identifyHostAndPort(HttpRequest httpRequest) {
        String hostAndPort = ProxyUtils.parseHostAndPort(httpRequest.uri());
        if (StringUtils.isBlank(hostAndPort)) {
            return httpRequest.headers().get(HttpHeaderNames.HOST);
        }
        return hostAndPort;
    }
}
