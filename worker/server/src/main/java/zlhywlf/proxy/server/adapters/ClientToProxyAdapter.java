package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyServer;

public class ClientToProxyAdapter extends ProxyServerAdapter<HttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final ProxyServer server;

    public ClientToProxyAdapter(ProxyServer server, ChannelPipeline pipeline) {
        this.server = server;
        logger.info("Initialization channel pipeline{}", this);
        pipeline.addLast("bytesReadAdapter", new BytesReadAdapter());
        pipeline.addLast("bytesWrittenAdapter", new BytesWrittenAdapter());
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("proxyAdapter", this);
    }
}
