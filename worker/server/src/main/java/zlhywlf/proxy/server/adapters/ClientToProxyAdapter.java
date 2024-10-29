package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyThreadPoolGroup;

public class ClientToProxyAdapter extends ChannelInitializer<Channel> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final ProxyThreadPoolGroup group;
    private final ChannelHandler proxyAdapter = new ProxyAdapter();
    private final ChannelHandler bytesReadAdapter = new BytesReadAdapter();
    private final ChannelHandler bytesWrittenAdapter = new BytesWrittenAdapter();

    public ClientToProxyAdapter(ProxyThreadPoolGroup group) {
        this.group = group;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        logger.info("Initialization channel pipeline{}", this);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("bytesReadAdapter", bytesReadAdapter);
        pipeline.addLast("bytesWrittenAdapter", bytesWrittenAdapter);
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("proxyAdapter", proxyAdapter);
    }
}
