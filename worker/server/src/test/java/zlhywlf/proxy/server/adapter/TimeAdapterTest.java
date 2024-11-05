package zlhywlf.proxy.server.adapter;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import zlhywlf.proxy.server.adapters.ClientToProxyAdapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TimeAdapterTest {
    @Test
    void testSuccess() {
        EmbeddedChannel channel = new EmbeddedChannel();
        new ClientToProxyAdapter(null, channel.pipeline(), null);
        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", Unpooled.wrappedBuffer(TimeAdapterTest.class.getName().getBytes()));
        channel.writeInbound(req);
        channel.flushInbound();
        assertNotNull(channel.readOutbound());
    }
}
