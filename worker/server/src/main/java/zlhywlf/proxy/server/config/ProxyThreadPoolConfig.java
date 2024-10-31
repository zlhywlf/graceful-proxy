package zlhywlf.proxy.server.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ClassUtils;

@Getter
@Setter
public class ProxyThreadPoolConfig {
    private String name = "unknown";
    private Class<? extends EventLoopGroup> eventLoopClazz = NioEventLoopGroup.class;

    @SuppressWarnings("unchecked")
    public void setEventLoopClazzByName(String setEventLoopClassName) {
        Class<?> eventLoopClazz = null;
        try {
            eventLoopClazz = ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), setEventLoopClassName, false);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (!ClassUtils.isAssignable(eventLoopClazz, EventLoopGroup.class)) {
            throw new IllegalArgumentException("eventLoopClass");
        }
        this.eventLoopClazz = (Class<? extends EventLoopGroup>) eventLoopClazz;
    }
}
