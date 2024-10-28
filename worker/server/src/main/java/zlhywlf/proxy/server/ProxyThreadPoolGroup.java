package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;

import java.lang.reflect.InvocationTargetException;

@Getter
public class ProxyThreadPoolGroup {

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Class<? extends ServerChannel> channelClazz;

    @SuppressWarnings("unchecked")
    public ProxyThreadPoolGroup() {
        String eventLoopClassName = SystemPropertyUtil.get("proxy.eventLoopClass", "io.netty.channel.nio.NioEventLoopGroup");
        String channelClassName = SystemPropertyUtil.get("proxy.channelClass", "io.netty.channel.socket.nio.NioServerSocketChannel");
        try {
            Class<?> eventLoopClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), eventLoopClassName, false);
            Class<?> channelClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), channelClassName, false);
            if (!ClassUtils.isAssignable(eventLoopClazz, EventLoopGroup.class)) {
                throw new IllegalArgumentException("proxy.eventLoopClass");
            }
            bossGroup = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz, 1);
            workerGroup = (EventLoopGroup) ConstructorUtils.invokeConstructor(eventLoopClazz);
            if (!ClassUtils.isAssignable(channelClazz, ServerChannel.class)) {
                throw new IllegalArgumentException("proxy.channelClass");
            }
            this.channelClazz = (Class<? extends ServerChannel>) channelClazz;
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
