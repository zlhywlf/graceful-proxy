package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCounted;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyServer;
import zlhywlf.proxy.server.ProxyState;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ClientToProxyAdapter extends ProxyServerAdapter<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);
    private static final Pattern HTTP_PREFIX = Pattern.compile("^(http|ws)s?://.*", Pattern.CASE_INSENSITIVE);

    private volatile HttpRequest currentRequest;
    private volatile ProxyToServerAdapter currentProxyToServerAdapter;
    private final Map<String, ProxyToServerAdapter> proxyToServerAdaptersByHostAndPort = new ConcurrentHashMap<>();

    public ClientToProxyAdapter(ProxyServer server, ChannelPipeline pipeline) {
        super(server, ProxyState.AWAITING_INITIAL);
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("ClientToProxyAdapter", this);
    }

    @Override
    public ProxyState readHttpInitial(HttpRequest msg) {
        return readHttpInitial0(msg);
    }

    @Override
    public void readHTTPChunk(HttpContent chunk) {
        currentProxyToServerAdapter.write(chunk);
    }


    public ProxyState readHttpInitial0(HttpRequest msg) {
        resetCurrentRequest();
        currentRequest = copy(msg);
        String serverHostAndPort = parseHostAndPort(msg);
        currentProxyToServerAdapter = proxyToServerAdaptersByHostAndPort.get(serverHostAndPort);
        if (currentProxyToServerAdapter == null) {
            currentProxyToServerAdapter = new ProxyToServerAdapter(this, serverHostAndPort, getServer());
            proxyToServerAdaptersByHostAndPort.put(serverHostAndPort, currentProxyToServerAdapter);
        }
        currentProxyToServerAdapter.write(msg);
        return ProxyState.AWAITING_CHUNK;
    }

    private void resetCurrentRequest() {
        if (currentRequest != null && currentRequest instanceof ReferenceCounted) {
            ((ReferenceCounted) currentRequest).release();
        }
        currentRequest = null;
    }

    private HttpRequest copy(HttpRequest original) {
        if (original instanceof FullHttpRequest) {
            return ((FullHttpRequest) original).copy();
        }
        HttpRequest request = new DefaultHttpRequest(
            original.protocolVersion(),
            original.method(),
            original.uri());
        request.headers().set(original.headers());
        return request;
    }

    public String parseHostAndPort(final HttpRequest httpRequest) {
        String uri = httpRequest.uri();
        String tempUri;
        if (!HTTP_PREFIX.matcher(uri).matches()) {
            tempUri = uri;
        } else {
            tempUri = StringUtils.substringAfter(uri, "://");
        }
        if (tempUri.contains("/")) {
            tempUri = tempUri.substring(0, tempUri.indexOf("/"));
        }
        if (StringUtils.isBlank(tempUri)) {
            List<String> hosts = httpRequest.headers().getAll(HttpHeaderNames.HOST);
            if (hosts != null && !hosts.isEmpty()) {
                tempUri = hosts.get(0);
            }
        }
        return tempUri;
    }
}
