package borgol;

import borgol.ui.web.GatewayProxyRouter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GatewayProxyRouter unit tests — no network, no Redis required.
 * Tests cache-key generation and proxy-mode detection logic only.
 */
class GatewayProxyRouterTest {

    @Test
    void proxyMode_falseWhenNoEnvVarsSet() {
        GatewayProxyRouter router = new GatewayProxyRouter(null, null);
        assertFalse(router.isProxyMode());
    }

    @Test
    void proxyMode_trueWhenJsonServiceUrlSet() {
        GatewayProxyRouter router = new GatewayProxyRouter("http://10.108.0.2:8080", null);
        assertTrue(router.isProxyMode());
    }

    @Test
    void proxyMode_trueWhenSoapServiceUrlSet() {
        GatewayProxyRouter router = new GatewayProxyRouter(null, "http://10.108.0.3:8080");
        assertTrue(router.isProxyMode());
    }

    @Test
    void buildCacheKey_pathOnly() {
        GatewayProxyRouter router = new GatewayProxyRouter(null, null);
        String key = router.buildCacheKey("/api/users/42", null);
        assertEquals("borgol:gw:/api/users/42", key);
    }

    @Test
    void buildCacheKey_pathWithQueryString() {
        GatewayProxyRouter router = new GatewayProxyRouter(null, null);
        String key = router.buildCacheKey("/api/cafes/nearby", "lat=47.9&lng=106.9");
        assertEquals("borgol:gw:/api/cafes/nearby?lat=47.9&lng=106.9", key);
    }

    @Test
    void buildCacheKey_emptyQueryStringIgnored() {
        GatewayProxyRouter router = new GatewayProxyRouter(null, null);
        String key = router.buildCacheKey("/api/users", "");
        assertEquals("borgol:gw:/api/users", key);
    }
}
