package zlhywlf.proxy.server.adapter;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import zlhywlf.proxy.server.adapters.TimeAdapter;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TimeAdapterTest {
    @Test
    void testSuccess() {
        TimeAdapter adapter = new TimeAdapter();
        EmbeddedChannel channel = new EmbeddedChannel(adapter);
        channel.writeInbound(Unpooled.copiedBuffer(TimeAdapterTest.class.getName().getBytes()));
        channel.flushInbound();
        assertNotNull(channel.readOutbound());
    }
}
