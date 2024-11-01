package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyServer;
import zlhywlf.proxy.server.ProxyState;

public class ClientToProxyAdapter extends ProxyServerAdapter<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private volatile HttpRequest currentRequest;

    public ClientToProxyAdapter(ProxyServer server, ChannelPipeline pipeline) {
        super(server, ProxyState.AWAITING_INITIAL);
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("proxyAdapter", this);
    }

    @Override
    public ProxyState readHttpInitial(HttpRequest msg) {
        return readHttpInitial0(msg);
    }


    public ProxyState readHttpInitial0(HttpRequest msg) {
        resetCurrentRequest();
        currentRequest = copy(msg);
        return ProxyState.AWAITING_INITIAL;
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
}
