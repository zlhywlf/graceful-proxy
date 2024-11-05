package zlhywlf.proxy.server;

import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.ClassUtils;

public class ProxyUtils {
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClazz(String className) {
        Class<T> eventLoopClazz;
        try {
            eventLoopClazz = (Class<T>) ClassUtils.getClass(PlatformDependent.getSystemClassLoader(), className, false);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new RuntimeException(e);
        }
        return eventLoopClazz;
    }
}
