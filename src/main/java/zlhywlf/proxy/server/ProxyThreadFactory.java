package zlhywlf.proxy.server;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import zlhywlf.proxy.core.ProxyUtils;

import java.lang.reflect.InvocationTargetException;

public class ProxyThreadFactory extends DefaultThreadFactory {
    private final String category;

    public ProxyThreadFactory(String category, Class<?> poolType) {
        super(poolType);
        this.category = category;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = super.newThread(r);
        t.setName(StringUtils.joinWith("-", category, t.getName()));
        return t;
    }

    public static EventLoopGroup create(String category, Class<? extends EventLoopGroup> eventLoopClazz, int threads) {
        try {
            return ConstructorUtils.invokeConstructor(eventLoopClazz, threads, new ProxyThreadFactory(category, eventLoopClazz));
        } catch (NoSuchMethodException |
                 IllegalAccessException |
                 InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static EventLoopGroup create(String category, String eventLoopClassName, int threads) {
        return create(category, ProxyUtils.getClazz(eventLoopClassName), threads);
    }
}
