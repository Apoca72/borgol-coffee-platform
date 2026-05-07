# Lab 08: API Gateway, Redis Caching & VPC Security — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a proper HTTP reverse-proxy gateway with Redis caching, SOAP token caching, and full cache-invalidation so the project meets all Lab 08 SOA requirements.

**Architecture:** A new `GatewayProxyRouter` class registers Javalin `before()` filters for `/api/users/**` and `/api/soap/**`. When the env vars `JSON_SERVICE_URL` / `SOAP_SERVICE_URL` are set (private IPs on DigitalOcean), it proxies requests there with Redis caching on GET responses. When unset, filters are not registered and internal handlers remain active (local dev / single-droplet mode). `SoapAuthClient.validateToken()` is extended to cache results in Redis to avoid a SOAP round-trip on every authenticated request.

**Tech Stack:** Java 21, Javalin 6.3.0, Jedis 5.1.0 (Redis client), Java `HttpClient` (built-in), JUnit 5, Maven

> **Note on VPC & Cloud Firewalls (Task 3 of the lab):** This is DigitalOcean infrastructure configuration — it cannot be done in code. After deploying, go to DigitalOcean Dashboard → Networking → VPC to group all Droplets, then create a Firewall (`Internal-Service-Firewall`) that allows port 8080 only from the Gateway Droplet's private IP, and assign it to the JSON/SOAP service Droplets. The code changes in this plan make the gateway ready for that topology.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `src/main/java/borgol/ui/web/GatewayProxyRouter.java` | **CREATE** | HTTP reverse proxy + Redis Cache Hit/Miss logic |
| `src/main/java/borgol/infrastructure/cache/RedisClient.java` | **MODIFY** | Add `deleteByPattern(pattern)` for bulk cache eviction |
| `src/main/java/borgol/infrastructure/cache/CacheKeyBuilder.java` | **MODIFY** | Add `forSoapToken(hash)` and `NEARBY_PATTERN` constant |
| `src/main/java/borgol/infrastructure/security/SoapAuthClient.java` | **MODIFY** | Cache `validateToken()` results; invalidate on logout |
| `src/main/java/borgol/core/application/CafeService.java` | **MODIFY** | Evict `borgol:cafes:nearby:*` when a cafe is created or rated |
| `src/main/java/borgol/ui/web/BorgolApiServer.java` | **MODIFY** | Instantiate `GatewayProxyRouter` and register it before internal routes |
| `src/test/java/borgol/GatewayProxyRouterTest.java` | **CREATE** | Unit tests for cache-key builder, proxy mode detection |

---

## Task 1: Add `deleteByPattern()` to RedisClient

**Files:**
- Modify: `src/main/java/borgol/infrastructure/cache/RedisClient.java`

This method uses the Jedis `SCAN` command to iterate over all matching keys and delete them. It's needed by `CafeService` to evict all `borgol:cafes:nearby:*` entries when a new cafe is added.

- [ ] **Step 1: Open RedisClient.java and add the method after `del()`**

In `src/main/java/borgol/infrastructure/cache/RedisClient.java`, after the existing `del()` method (line ~118), add:

```java
/**
 * Устгах хэв загварт тохирох бүх түлхүүрийг устгана.
 * SCAN командаар давтан хайна — KEYS-г ашиглахгүй (production-safe).
 *
 * @param pattern Redis glob хэв загвар, жишээ нь "borgol:cafes:nearby:*"
 * @return        устгасан түлхүүрийн тоо
 */
public long deleteByPattern(String pattern) {
    long deleted = 0;
    try (Jedis jedis = pool.getResource()) {
        String cursor = "0";
        do {
            redis.clients.jedis.params.ScanParams params =
                new redis.clients.jedis.params.ScanParams().match(pattern).count(100);
            redis.clients.jedis.resps.ScanResult<String> result =
                jedis.scan(cursor, params);
            cursor = result.getCursor();
            if (!result.getResult().isEmpty()) {
                jedis.del(result.getResult().toArray(new String[0]));
                deleted += result.getResult().size();
            }
        } while (!"0".equals(cursor));
    } catch (Exception e) {
        // fail-open — cache eviction failure is non-fatal
        System.err.println("[Redis] deleteByPattern error: " + e.getMessage());
    }
    return deleted;
}
```

- [ ] **Step 2: Compile to verify no errors**

```bash
cd /c/Users/thatu/OneDrive/Desktop/cafe-project
mvn compile -q
```
Expected: BUILD SUCCESS (no errors).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/cache/RedisClient.java
git commit -m "feat: add deleteByPattern() to RedisClient for bulk cache eviction"
```

---

## Task 2: Extend CacheKeyBuilder with SOAP and pattern constants

**Files:**
- Modify: `src/main/java/borgol/infrastructure/cache/CacheKeyBuilder.java`

- [ ] **Step 1: Add two new entries to CacheKeyBuilder**

Open `src/main/java/borgol/infrastructure/cache/CacheKeyBuilder.java`. After the existing `forCafesNearby()` method, add:

```java
/**
 * SOAP token баталгаажуулалтын кэш түлхүүр.
 * Бодит token-г хадгалахгүй — hashCode()-г ашиглана (богино, аюулгүй).
 * TTL: 300 секунд (5 минут)
 *
 * @param tokenHash token.hashCode()-ийн утга (hex)
 * @return жишээ: "borgol:soap:token:1a2b3c"
 */
public static String forSoapToken(String tokenHash) {
    return PREFIX + "soap:token:" + tokenHash;
}

/**
 * Ойролцоох кафены кэшийн бүх түлхүүрийг олох хэв загвар.
 * RedisClient.deleteByPattern()-д шилжүүлнэ.
 */
public static final String NEARBY_PATTERN = PREFIX + "cafes:nearby:*";

/**
 * Gateway proxy кэшийн бүх түлхүүрийг олох хэв загвар.
 * Тодорхой path-д кэш хүчингүй болгох шаардлагагүй — TTL хангалттай.
 */
public static final String GATEWAY_CACHE_PATTERN = PREFIX + "gw:*";
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/borgol/infrastructure/cache/CacheKeyBuilder.java
git commit -m "feat: add forSoapToken() and pattern constants to CacheKeyBuilder"
```

---

## Task 3: Create GatewayProxyRouter

**Files:**
- Create: `src/main/java/borgol/ui/web/GatewayProxyRouter.java`
- Create: `src/test/java/borgol/GatewayProxyRouterTest.java`

This is the core Lab 08 requirement. The router:
- Reads `JSON_SERVICE_URL` and `SOAP_SERVICE_URL` from env
- Registers Javalin `before()` filters when those vars are set
- GET: checks Redis → Cache Hit returns immediately; Cache Miss proxies + stores with 60s TTL
- Non-GET: proxies directly (no caching — data mutates)
- Calls `ctx.skipRemainingHandlers()` so internal route handlers never fire in proxy mode

- [ ] **Step 1: Write the failing test first**

Create `src/test/java/borgol/GatewayProxyRouterTest.java`:

```java
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
        // When no JSON_SERVICE_URL / SOAP_SERVICE_URL are set,
        // the router should report internal/dev mode.
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
```

- [ ] **Step 2: Run test to confirm it fails (class not found)**

```bash
mvn test -pl . -Dtest=GatewayProxyRouterTest -q 2>&1 | tail -5
```
Expected: FAIL with compilation error — `GatewayProxyRouter` not found.

- [ ] **Step 3: Create GatewayProxyRouter.java**

Create `src/main/java/borgol/ui/web/GatewayProxyRouter.java`:

```java
package borgol.ui.web;

import borgol.infrastructure.cache.RedisClient;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP Reverse Proxy with Redis Caching — Lab 08 Core Requirement.
 *
 * ════════════════════════════════════════════════════════════
 * Архитектур (Lab 08 spec):
 *   Client → GatewayProxyRouter → Redis (Cache Hit?)
 *                              ↘ Backend Service (Cache Miss → Redis-д хадгална)
 * ════════════════════════════════════════════════════════════
 *
 * Тохиргоо (орчны хувьсагч):
 *   JSON_SERVICE_URL → жишээ: http://10.108.0.2:8080  (private IP)
 *   SOAP_SERVICE_URL → жишээ: http://10.108.0.3:8080  (private IP)
 *
 * Тохируулаагүй үед → дотоод routing (орон нутаг / нэг-droplet горим).
 *
 * Кэш стратеги (зөвхөн GET):
 *   Cache Hit  → Redis-ээс хурдан буцаана (backend дуудахгүй)
 *   Cache Miss → backend руу proxy → Redis-д 60 секунд хадгална
 * GET биш хүсэлт → шууд proxy (кэш байхгүй — өгөгдөл өөрчлөгдөж болно)
 */
public class GatewayProxyRouter {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyRouter.class);

    /** Lab 08 pseudocode-д зааснаар 60 секунд TTL */
    static final int    CACHE_TTL    = 60;
    static final String CACHE_PREFIX = "borgol:gw:";

    private final String jsonServiceUrl;
    private final String soapServiceUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Production constructor — reads env vars. */
    public GatewayProxyRouter() {
        this(System.getenv("JSON_SERVICE_URL"), System.getenv("SOAP_SERVICE_URL"));
    }

    /** Testable constructor — URLs injected directly. */
    GatewayProxyRouter(String jsonServiceUrl, String soapServiceUrl) {
        this.jsonServiceUrl = jsonServiceUrl;
        this.soapServiceUrl = soapServiceUrl;
    }

    /** True when at least one upstream private-IP service is configured. */
    public boolean isProxyMode() {
        return jsonServiceUrl != null || soapServiceUrl != null;
    }

    /**
     * Javalin before()-filter бүртгэнэ.
     * Дотоод route handler-уудаас ӨМНӨ дуудагдах ёстой.
     * Proxy mode идэвхтэй үед skipRemainingHandlers() дуудаж
     * дотоод handler-г зогсооно.
     */
    public void register(Javalin app) {
        if (jsonServiceUrl != null) {
            app.before("/api/users",   ctx -> handleProxy(ctx, jsonServiceUrl));
            app.before("/api/users/*", ctx -> handleProxy(ctx, jsonServiceUrl));
            log.info("[GW-Proxy] /api/users/** → {}", jsonServiceUrl);
        }
        if (soapServiceUrl != null) {
            app.before("/api/soap",   ctx -> handleProxy(ctx, soapServiceUrl));
            app.before("/api/soap/*", ctx -> handleProxy(ctx, soapServiceUrl));
            log.info("[GW-Proxy] /api/soap/**  → {}", soapServiceUrl);
        }
        if (!isProxyMode()) {
            log.info("[GW-Proxy] Upstream URL тохируулаагүй — дотоод routing идэвхтэй (dev горим)");
        }
    }

    // ── Core proxy logic ─────────────────────────────────────────────────────

    private void handleProxy(Context ctx, String upstream) {
        // CORS preflight-г ApiGateway шийдэж байна — энд алгасна
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) return;

        boolean isGet = "GET".equalsIgnoreCase(ctx.method().name());

        if (isGet) {
            String cacheKey = buildCacheKey(ctx.path(), ctx.queryString());

            // ── Cache Hit ────────────────────────────────────────────────────
            try {
                String cached = RedisClient.get().get(cacheKey);
                if (cached != null) {
                    log.info("[Gateway] Cache HIT  — {}", cacheKey);
                    ctx.status(200).contentType("application/json").result(cached);
                    ctx.skipRemainingHandlers();
                    return;
                }
            } catch (Exception e) {
                log.warn("[Gateway] Redis уншихад алдаа (fail-open): {}", e.getMessage());
            }

            // ── Cache Miss — proxy + store ────────────────────────────────────
            log.info("[Gateway] Cache MISS — {}", cacheKey);
            ProxyResult result = doProxy(ctx, upstream);
            if (result == null) return;

            if (result.status() >= 200 && result.status() < 300) {
                try {
                    RedisClient.get().setex(cacheKey, CACHE_TTL, result.body());
                } catch (Exception e) {
                    log.warn("[Gateway] Redis бичихэд алдаа: {}", e.getMessage());
                }
            }
            ctx.status(result.status()).contentType("application/json").result(result.body());
            ctx.skipRemainingHandlers();

        } else {
            // ── Non-GET: proxy directly, no caching ─────────────────────────
            ProxyResult result = doProxy(ctx, upstream);
            if (result == null) return;
            ctx.status(result.status()).contentType("application/json").result(result.body());
            ctx.skipRemainingHandlers();
        }
    }

    /**
     * Backend сервис рүү HTTP хүсэлт илгээнэ.
     * Path, query string, Authorization header болон body-г дамжуулна.
     * Сүлжээний алдаа гарвал 502 буцаана.
     */
    private ProxyResult doProxy(Context ctx, String upstream) {
        try {
            String target = upstream + ctx.path();
            String qs     = ctx.queryString();
            if (qs != null && !qs.isEmpty()) target += "?" + qs;

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");

            String auth = ctx.header("Authorization");
            if (auth != null) builder.header("Authorization", auth);

            String method = ctx.method().name().toUpperCase();
            String body   = ctx.body();

            HttpRequest req = switch (method) {
                case "POST"   -> builder.POST  (HttpRequest.BodyPublishers.ofString(body)).build();
                case "PUT"    -> builder.PUT   (HttpRequest.BodyPublishers.ofString(body)).build();
                case "DELETE" -> builder.DELETE().build();
                default       -> builder.GET   ().build();
            };

            long t0 = System.currentTimeMillis();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            log.debug("[GW-Proxy] {} {} → {} ({}ms)",
                      method, target, resp.statusCode(), System.currentTimeMillis() - t0);
            return new ProxyResult(resp.statusCode(), resp.body());

        } catch (Exception e) {
            log.error("[GW-Proxy] Proxy алдаа → {}{}: {}", upstream, ctx.path(), e.getMessage());
            ctx.status(502).contentType("application/json")
               .result("{\"error\":\"Gateway upstream error: " + e.getMessage() + "\"}");
            ctx.skipRemainingHandlers();
            return null;
        }
    }

    /**
     * Кэш түлхүүр үүсгэнэ — package-private for testing.
     * Формат: "borgol:gw:{path}" эсвэл "borgol:gw:{path}?{queryString}"
     */
    String buildCacheKey(String path, String queryString) {
        String key = CACHE_PREFIX + path;
        if (queryString != null && !queryString.isEmpty()) key += "?" + queryString;
        return key;
    }

    private record ProxyResult(int status, String body) {}
}
```

- [ ] **Step 4: Run tests**

```bash
mvn test -Dtest=GatewayProxyRouterTest -q
```
Expected: All 6 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/borgol/ui/web/GatewayProxyRouter.java \
        src/test/java/borgol/GatewayProxyRouterTest.java
git commit -m "feat: add GatewayProxyRouter — HTTP reverse proxy with Redis caching (Lab 08)"
```

---

## Task 4: Wire GatewayProxyRouter into BorgolApiServer

**Files:**
- Modify: `src/main/java/borgol/ui/web/BorgolApiServer.java` (constructor area)

The proxy router must be registered **before** `registerRoutes()` so its `before()` filters fire first.

- [ ] **Step 1: Add field and wire in constructor**

In `BorgolApiServer.java`, add the field after `private final RedisEventBus eventBus;` (around line 43):

```java
private final GatewayProxyRouter proxyRouter;
```

Then in the constructor body, add `proxyRouter` instantiation and registration **between** `gateway.registerFilters(app)` and `registerRoutes()`:

Find this block (lines 57–58):
```java
gateway.registerFilters(app);
registerRoutes();
```

Replace with:
```java
gateway.registerFilters(app);
this.proxyRouter = new GatewayProxyRouter();
proxyRouter.register(app);   // proxy before() filters intercept BEFORE internal routes
registerRoutes();
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Run all tests**

```bash
mvn test -q
```
Expected: All tests PASS (proxy mode off in test env — no env vars set).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/ui/web/BorgolApiServer.java
git commit -m "feat: wire GatewayProxyRouter into BorgolApiServer before internal routes"
```

---

## Task 5: SOAP Cache Management — cache validateToken() in Redis

**Files:**
- Modify: `src/main/java/borgol/infrastructure/security/SoapAuthClient.java`

Every authenticated HTTP request calls `validateToken()` → a SOAP XML round-trip. Caching valid results avoids this. Only VALID responses are cached (invalid tokens are cheap to reject again). TTL is 5 minutes — short enough to catch token revocations promptly.

Cache key: `borgol:soap:token:{hex(token.hashCode())}` — uses hash to keep key short.
Cache value format: `{valid}|{userId}|{username}` (pipe-delimited, simple to parse without Gson).

- [ ] **Step 1: Add Redis cache to validateToken()**

Open `SoapAuthClient.java`. Replace the entire `validateToken()` method (lines 136–156) with:

```java
/**
 * Calls SOAP ValidateToken with Redis caching.
 *
 * Cache strategy:
 *   Hit  → parse cached "valid|userId|username" string, return immediately
 *   Miss → call SOAP, cache valid results for 300s (5 min TTL)
 *
 * Only caches valid=true results.
 * Rationale: invalid tokens fail fast even without caching.
 */
public ValidationResult validateToken(String token) {
    if (token == null || token.isBlank())
        return new ValidationResult(false, null, null);

    // ── Cache Hit ────────────────────────────────────────────────────────────
    String cacheKey = "borgol:soap:token:" + Integer.toHexString(token.hashCode());
    try {
        borgol.infrastructure.cache.RedisClient redis =
            borgol.infrastructure.cache.RedisClient.get();
        String cached = redis.get(cacheKey);
        if (cached != null) {
            // Format: "true|42|coffee_master"  or  never stored for false
            String[] parts = cached.split("\\|", 3);
            boolean  valid    = Boolean.parseBoolean(parts[0]);
            Integer  userId   = parts[1].isEmpty() ? null : Integer.parseInt(parts[1]);
            String   username = parts[2].isEmpty() ? null : parts[2];
            System.out.println("  [SOAP Cache HIT] token hash=" + Integer.toHexString(token.hashCode()));
            return new ValidationResult(valid, userId, username);
        }
    } catch (Exception ignored) {
        // Redis unavailable — fall through to SOAP call
    }

    // ── Cache Miss — call SOAP ───────────────────────────────────────────────
    String body = soapEnvelope("""
        <auth:ValidateTokenRequest xmlns:auth="%s">
          <auth:token>%s</auth:token>
        </auth:ValidateTokenRequest>
        """.formatted(NAMESPACE, escapeXml(token)));
    try {
        String  xml      = post(body);
        boolean valid    = extractBool(xml, "valid");
        String  uidStr   = extractTag(xml,  "userId");
        String  username = extractTag(xml,  "username");
        Integer userId   = uidStr != null ? Integer.parseInt(uidStr) : null;

        // Cache only valid tokens — invalid ones are rejected quickly anyway
        if (valid) {
            try {
                String value = valid + "|" + (userId != null ? userId : "") +
                               "|" + (username != null ? username : "");
                borgol.infrastructure.cache.RedisClient.get().setex(cacheKey, 300, value);
                System.out.println("  [SOAP Cache MISS→stored] token hash=" +
                    Integer.toHexString(token.hashCode()));
            } catch (Exception ignored) { }
        }
        return new ValidationResult(valid, userId, username);
    } catch (Exception e) {
        return new ValidationResult(false, null, null);
    }
}
```

- [ ] **Step 2: Add `invalidateSoapToken()` helper for logout/revocation (bonus requirement)**

In `SoapAuthClient.java`, add this method after `validateToken()`:

```java
/**
 * SOAP token кэшийг устгана — хэрэглэгч гарсан үед дуудна.
 * Cache Invalidation стратеги: токен устгавал дараагийн хүсэлт
 * Redis-ийн оронд SOAP сервист шинээр баталгаажуулна.
 *
 * @param token устгах токен
 */
public void invalidateSoapToken(String token) {
    if (token == null || token.isBlank()) return;
    String cacheKey = "borgol:soap:token:" + Integer.toHexString(token.hashCode());
    try {
        borgol.infrastructure.cache.RedisClient.get().del(cacheKey);
        System.out.println("  [SOAP Cache EVICT] token hash=" +
            Integer.toHexString(token.hashCode()));
    } catch (Exception ignored) { }
}
```

- [ ] **Step 3: Compile**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Run all tests**

```bash
mvn test -q
```
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/borgol/infrastructure/security/SoapAuthClient.java
git commit -m "feat: cache SOAP validateToken() in Redis with 5min TTL (Lab 08 bonus)"
```

---

## Task 6: Fix CafeService — evict nearby-cafe cache on write

**Files:**
- Modify: `src/main/java/borgol/core/application/CafeService.java`

Currently `getCafesNearby()` caches results but `createCafe()` and `rateCafe()` never evict those entries. A new cafe appears in the DB but stale cached coordinates are returned for up to 120s.

- [ ] **Step 1: Add eviction helper and call it from createCafe() and rateCafe()**

In `CafeService.java`, add a `cacheEvictNearby()` helper method after `cachePut()` (around line 51):

```java
/**
 * Ойролцоох кафены бүх кэш оруулгыг устгана.
 * Шинэ кафе нэмэх эсвэл үнэлгээ өөрчлөхөд дуудагдана.
 * RedisClient.deleteByPattern()-г ашиглан SCAN-аар хайна (KEYS-г биш).
 */
private void cacheEvictNearby() {
    try {
        long n = RedisClient.get().deleteByPattern(CacheKeyBuilder.NEARBY_PATTERN);
        if (n > 0) log.debug("[Cache EVICT] {} borgol:cafes:nearby:* entries cleared", n);
    } catch (Exception e) {
        log.debug("[Cache] Nearby eviction error: {}", e.getMessage());
    }
}
```

Then in `createCafe()`, after `return repo.createCafe(c);` on line 79, change to:

```java
CafeListing created = repo.createCafe(c);
cacheEvictNearby();   // new cafe → nearby results are stale
return created;
```

And in `rateCafe()`, after `achievements.checkAndAwardAchievements(userId);` (line 84), add:

```java
cacheEvictNearby();   // rating change → nearby list may reorder
```

- [ ] **Step 2: Compile**

```bash
mvn compile -q
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Run all tests**

```bash
mvn test -q
```
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/borgol/core/application/CafeService.java
git commit -m "fix: evict borgol:cafes:nearby:* cache on createCafe() and rateCafe()"
```

---

## Task 7: Full test run & final verification

- [ ] **Step 1: Run the full test suite**

```bash
mvn test
```
Expected output (all tests pass):
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 2: Verify compilation with full build**

```bash
mvn package -DskipTests -q
```
Expected: `target/cafe-project-1.0-SNAPSHOT.jar` created, BUILD SUCCESS.

- [ ] **Step 3: Smoke-check Cache Hit/Miss logs appear**

Start with `MODE=web`:
```bash
java -DMODE=web -Dfile.encoding=UTF-8 -jar target/cafe-project-1.0-SNAPSHOT.jar &
sleep 5
# First request — should log Cache MISS
curl -s http://localhost:7000/api/recipes | head -c 100
# Second request — should log Cache HIT
curl -s http://localhost:7000/api/recipes | head -c 100
```

In the server log you should see:
```
[Cache MISS] borgol:recipe:...
[Cache HIT]  borgol:recipe:...
```

Kill the server:
```bash
pkill -f cafe-project
```

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: Lab 08 complete — gateway proxy, Redis caching, SOAP cache, eviction"
```

---

## Summary of what each task fixes

| Lab 08 Requirement | Fixed by |
|---|---|
| Task 1: Redis at `localhost:6379` | Already working — `RedisClient` existed |
| Task 2: Gateway proxy routing (`/api/users/**`, `/api/soap/**`) | Task 3 (`GatewayProxyRouter`) + Task 4 (wiring) |
| Task 2: Cache Hit / Cache Miss with 60s TTL | Task 3 (`GatewayProxyRouter.handleProxy()`) |
| Task 3: VPC & Firewalls | **Infrastructure only** — configure on DigitalOcean Dashboard after deploy |
| Task 4: Frontend → single gateway URL | Already done — `api.js` uses relative paths |
| Bonus: SOAP Cache Management + Invalidation | Task 5 (`SoapAuthClient.validateToken()` + `invalidateSoapToken()`) |
| Bug: Nearby cafe cache never invalidated | Task 6 (`CafeService.cacheEvictNearby()`) |
