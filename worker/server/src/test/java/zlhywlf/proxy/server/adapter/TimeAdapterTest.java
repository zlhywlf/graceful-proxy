package zlhywlf.proxy.server.adapter;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import zlhywlf.proxy.server.adapters.ProxyAdapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TimeAdapterTest {
    @Test
    void testSuccess() {
        ProxyAdapter adapter = new ProxyAdapter();
        EmbeddedChannel channel = new EmbeddedChannel(
            new HttpServerCodec(),
            adapter);
        DefaultFullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/", Unpooled.wrappedBuffer(TimeAdapterTest.class.getName().getBytes()));
        channel.writeInbound(req);
        channel.flushInbound();
        assertNotNull(channel.readOutbound());
    }
}
