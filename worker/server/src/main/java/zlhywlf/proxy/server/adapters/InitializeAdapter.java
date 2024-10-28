package zlhywlf.proxy.server.adapters;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

@ChannelHandler.Sharable
public class InitializeAdapter<T extends Channel> extends ChannelInitializer<T> {
    @Override
    protected void initChannel(T sc) {
        sc.pipeline().addLast(new TimeAdapter());
    }
}
