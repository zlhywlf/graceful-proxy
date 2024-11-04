package zlhywlf.proxy.server;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;

public class ProxyThreadFactory extends DefaultThreadFactory {
    private final String proxyName;
    private final String category;
    private final int uniqueProxyThreadPoolGroupId;

    public ProxyThreadFactory(String category, Class<?> poolType, int uniqueProxyThreadPoolGroupId, String proxyName) {
        super(poolType);
        this.uniqueProxyThreadPoolGroupId = uniqueProxyThreadPoolGroupId;
        this.proxyName = proxyName;
        this.category = category;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = super.newThread(r);
        t.setName(StringUtils.joinWith("-", proxyName, uniqueProxyThreadPoolGroupId, category, t.getName()));
        return t;
    }
}
