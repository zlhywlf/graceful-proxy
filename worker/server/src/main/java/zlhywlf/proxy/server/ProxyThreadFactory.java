package zlhywlf.proxy.server;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;

public class ProxyThreadFactory extends DefaultThreadFactory {
    private final String prefix;
    private final String delimiter = "-";

    public ProxyThreadFactory(Class<?> poolType, int uniqueProxyThreadPoolGroupId, String name, String category) {
        super(poolType);
        prefix = StringUtils.joinWith(delimiter, name, uniqueProxyThreadPoolGroupId, category);
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = super.newThread(r);
        t.setName(prefix + delimiter + t.getName());
        return t;
    }
}
