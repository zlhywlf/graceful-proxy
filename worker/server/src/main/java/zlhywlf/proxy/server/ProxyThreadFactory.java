package zlhywlf.proxy.server;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class ProxyThreadFactory extends DefaultThreadFactory {
    private static final AtomicInteger poolId = new AtomicInteger();

    private final AtomicInteger nextId;
    private final String prefix;
    private final String delimiter = "-";

    public ProxyThreadFactory(Class<?> poolType, int uniqueProxyThreadPoolGroupId, String name, String category) {
        super(poolType);
        nextId = new AtomicInteger();
        prefix = StringUtils.joinWith(delimiter, name, uniqueProxyThreadPoolGroupId, category, ClassUtils.getSimpleName(poolType), poolId.incrementAndGet());
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = super.newThread(r, prefix + delimiter + nextId.incrementAndGet());
        t.setPriority(5);
        return t;
    }
}
