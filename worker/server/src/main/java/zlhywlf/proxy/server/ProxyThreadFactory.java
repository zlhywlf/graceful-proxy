package zlhywlf.proxy.server;

import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang3.StringUtils;

public class ProxyThreadFactory extends DefaultThreadFactory {
    private final String groupCategory;
    private final String category;

    public ProxyThreadFactory(String category, Class<?> poolType, String groupCategory) {
        super(poolType);
        this.groupCategory = groupCategory;
        this.category = category;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = super.newThread(r);
        t.setName(StringUtils.joinWith("-", groupCategory, category, t.getName()));
        return t;
    }
}
