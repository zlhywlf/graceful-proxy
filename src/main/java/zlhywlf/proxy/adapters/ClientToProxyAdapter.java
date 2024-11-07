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
import java.util.Base64;

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
        if (ProxyUtils.isCONNECT(httpRequest) || getTarget() == null) {
            String host = null;
            int port = 80;
            if (httpRequest.headers().contains(HttpHeaderNames.PROXY_AUTHORIZATION)) {
                String fullValue = httpRequest.headers().get(HttpHeaderNames.PROXY_AUTHORIZATION);
                String value = StringUtils.substringAfter(fullValue, "Basic ").trim();
                String decodedValue = new String(Base64.getDecoder().decode(value));
                String[] arr = decodedValue.split(":");
                if (arr.length == 4) {
                    setRelay(StringUtils.isNumeric(arr[3]));
                    String username = arr[0];
                    String password = arr[1];
                    host = arr[2];
                    port = isRelay() ? Integer.parseInt(arr[3]) : port;
                    String auth = username + ":" + password;
                    httpRequest.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
                }
            }
            if (!isRelay()) {
                String hostAndPortStr = identifyHostAndPort(httpRequest);
                logger.info("Finding ProxyToServer for: {}", hostAndPortStr);
                String[] hostPortArray = hostAndPortStr.split(":");
                host = hostPortArray[0];
                if (hostPortArray.length > 1) {
                    port = Integer.parseInt(hostPortArray[1]);
                }
            }
            setTarget(new ProxyToServerAdapter(getContext(), this, new InetSocketAddress(host, port)));
            getTarget().setRelay(isRelay());
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
