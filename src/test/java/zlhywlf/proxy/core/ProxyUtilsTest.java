package zlhywlf.proxy.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ProxyUtilsTest {
    @ParameterizedTest
    @CsvSource({
        "http://www.test.com:80/test,www.test.com:80",
        "https://www.test.com:80/test,www.test.com:80",
        "https://www.test.com:443/test,www.test.com:443",
        "www.test.com:80/test,www.test.com:80",
        "http://www.test.com,www.test.com",
        "www.test.com,www.test.com",
    })
    public void testParseHostAndPort(String uri, String expected) {
        Assertions.assertEquals(ProxyUtils.parseHostAndPort(uri), expected);
    }
}
