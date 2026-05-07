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
 *   JSON_SERVICE_URL → жишээ: http://10.108.0.2:8080  (VPC private IP)
 *   SOAP_SERVICE_URL → жишээ: http://10.108.0.3:8080  (VPC private IP)
 *
 * Тохируулаагүй үед → дотоод routing (орон нутаг / нэг-droplet горим).
 *
 * Routing:
 *   /api/users/** → JSON_SERVICE_URL (JSON сервис)
 *   /api/soap/**  → SOAP_SERVICE_URL (SOAP auth сервис)
 *
 * Кэш стратеги (GET хүсэлт):
 *   Cache Hit  → Redis-ээс буцаана (backend дуудахгүй)
 *   Cache Miss → backend руу proxy → Redis-д 60 секунд хадгална
 * GET биш хүсэлт → шууд proxy, кэш байхгүй (өгөгдөл өөрчлөгдөж болно)
 *
 * Загвар: Gateway (Enterprise Integration Patterns) +
 *          Cache-Aside (Cloud Design Patterns)
 */
public class GatewayProxyRouter {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyRouter.class);

    /** Lab 08 pseudocode-д зааснаар 60 секунд TTL */
    public static final int    CACHE_TTL    = 60;
    public static final String CACHE_PREFIX = "borgol:gw:";

    private final String jsonServiceUrl;
    private final String soapServiceUrl;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Production constructor — орчны хувьсагчаас унших. */
    public GatewayProxyRouter() {
        this(System.getenv("JSON_SERVICE_URL"), System.getenv("SOAP_SERVICE_URL"));
    }

    /**
     * Testable constructor — URL-уудыг шууд оруулна (dependency injection).
     * Production code-д ашиглахгүй.
     */
    public GatewayProxyRouter(String jsonServiceUrl, String soapServiceUrl) {
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
     * Proxy mode идэвхтэй үед skipRemainingHandlers()-г дуудаж
     * дотоод handler-г тойрно.
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
     * Backend сервис рүү HTTP хүсэлт дамжуулна.
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
     * Кэш түлхүүр үүсгэнэ.
     * Формат: "borgol:gw:{path}" эсвэл "borgol:gw:{path}?{queryString}"
     * public — тестэд шууд дуудагдана.
     */
    public String buildCacheKey(String path, String queryString) {
        String key = CACHE_PREFIX + path;
        if (queryString != null && !queryString.isEmpty()) key += "?" + queryString;
        return key;
    }

    private record ProxyResult(int status, String body) {}
}
