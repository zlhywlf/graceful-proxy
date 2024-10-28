package zlhywlf.proxy.server.adapters;

import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.server.ProxyThreadPoolGroup;

public class ClientToProxyAdapter extends ChannelInitializer<Channel> {
    private static final Logger logger = LoggerFactory.getLogger(ClientToProxyAdapter.class);

    private final ProxyThreadPoolGroup group;
    private final ChannelHandler timeAdapter;

    public ClientToProxyAdapter(ProxyThreadPoolGroup group) {
        this.group = group;
        timeAdapter = new TimeAdapter();
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        logger.info("Initialization channel pipeline{}", this);
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("timeAdapter", timeAdapter);
    }
}
