package zlhywlf.proxy.server;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zlhywlf.proxy.core.ProxyServer;
import zlhywlf.proxy.models.ResponseInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class SimpleProxyTest {
    private static final Logger logger = LoggerFactory.getLogger(SimpleProxyTest.class);

    static CloseableHttpClient httpClientSimple;
    static CloseableHttpClient httpClientProxy;
    static Server webServer;
    static int webServerPort;
    static HttpHost webHost;
    static ProxyServer proxyServer;

    @BeforeAll
    static void init() {
        proxyServer = new ProxyBootstrap(new String[]{"-p=0"}).start();
        httpClientSimple = buildHttpClient(false);
        httpClientProxy = buildHttpClient(true);
        webServer = createWebServer();
        for (Connector connector : webServer.getConnectors()) {
            if (!Objects.equals(connector.getDefaultConnectionFactory().getProtocol(), "SSL")) {
                webServerPort = ((ServerConnector) connector).getLocalPort();
            }
        }
        webHost = new HttpHost("localhost", webServerPort);
    }

    @AfterAll
    static void clear() {
        try {
            httpClientSimple.close();
            httpClientProxy.close();
            proxyServer.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSimpleGetRequest() {
        compareProxiedAndNoProxiedGet(webHost, "/");
    }

    void compareProxiedAndNoProxiedGet(HttpHost host, String resourceUrl) {
        ResponseInfo noProxied = httpGet(host, resourceUrl, false);
        ResponseInfo proxied = httpGet(host, resourceUrl, true);
        logger.info(noProxied.toString());
        Assertions.assertEquals(noProxied, proxied);
    }

    ResponseInfo httpGet(HttpHost host, String resourceUrl, boolean isProxied) {
        try {
            CloseableHttpClient httpClient = isProxied ? httpClientProxy : httpClientSimple;
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get().setHttpHost(host).setPath(resourceUrl).build();
            return httpClient.execute(httpGet, response -> new ResponseInfo(response.getCode(), EntityUtils.toString(response.getEntity())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static CloseableHttpClient buildHttpClient(boolean isProxied) {
        PoolingHttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder
            .create()
            .setConnectionConfigResolver(httpRoute -> ConnectionConfig
                .custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .build())
            .build();
        HttpClientBuilder builder = HttpClients
            .custom()
            .setConnectionManager(cm);
        if (isProxied) {
            HttpHost proxy = new HttpHost("localhost", proxyServer.getBoundAddress().getPort());
            builder.setProxy(proxy);
        }
        return builder.build();
    }

    static Server createWebServer() {
        Server server = new Server(0);
        server.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                response.write(true, ByteBuffer.wrap("Hello World".getBytes()), callback);
                return true;
            }
        });
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return server;
    }
}
