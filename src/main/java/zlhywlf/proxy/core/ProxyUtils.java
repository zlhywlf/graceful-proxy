package zlhywlf.proxy.core;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class ProxyUtils {
    private static final Pattern HTTP_PREFIX = Pattern.compile("^(http|ws)s?://.*", Pattern.CASE_INSENSITIVE);

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

    public static String parseHostAndPort(final String uri) {
        final String tempUri;
        if (!HTTP_PREFIX.matcher(uri).matches()) {
            tempUri = uri;
        } else {
            tempUri = StringUtils.substringAfter(uri, "://");
        }
        final String hostAndPort;
        if (tempUri.contains("/")) {
            hostAndPort = tempUri.substring(0, tempUri.indexOf("/"));
        } else {
            hostAndPort = tempUri;
        }
        return hostAndPort;
    }

    public static boolean isCONNECT(HttpObject httpObject) {
        return httpObject instanceof HttpRequest && HttpMethod.CONNECT.equals(((HttpRequest) httpObject).method());
    }

    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion,
                                                          HttpResponseStatus status) {
        return createFullHttpResponse(httpVersion, status, null, null, 0);
    }

    public static FullHttpResponse createFullHttpResponse(HttpVersion httpVersion, HttpResponseStatus status, String contentType, ByteBuf content, int contentLength) {
        DefaultFullHttpResponse response;
        if (content != null) {
            response = new DefaultFullHttpResponse(httpVersion, status, content);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentLength);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        } else {
            response = new DefaultFullHttpResponse(httpVersion, status);
        }
        return response;
    }
}
