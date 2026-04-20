package mn.edu.num.cafe.ui.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import mn.edu.num.cafe.infrastructure.security.JwtUtil;
import mn.edu.num.cafe.infrastructure.security.SoapAuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

/**
 * API Gateway — HTTP давхаргын нийтлэг үүрэг хариуцлагыг нэг газар төвлөрүүлнэ.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Gateway (Enterprise Integration Patterns) + Chain of Responsibility
 * ════════════════════════════════════════════════════════════
 * Зорилго: BorgolApiServer нь бизнесийн логик дээр анхаарлаа хандуулна.
 * Аюулгүй байдал, хурд хязгаарлалт, нэвтрэлт баталгаажуулалт Gateway-д байна.
 *
 * Архитектурын зааг ("private subnet"):
 *   Web Layer (BorgolApiServer) → ApiGateway → Domain (BorgolService)
 *
 * BorgolApiServer нь JwtUtil болон SoapAuthClient-г шууд дуудахгүй —
 * тэдгээр нь ApiGateway-ийн "дотоод subnet"-д байна.
 *
 * Gateway хариуцах зүйлс:
 *   1. CORS header — ямар ч origin-с хандах боломжтой
 *   2. Rate Limiting — Redis INCR+EXPIRE аргаар login/register хамгаалах
 *   3. Auth — SOAP→JWT fallback нэвтрэлт баталгаажуулалт
 *   4. Хүсэлт логлох — audit trail
 */
public class ApiGateway {

    private static final Logger log = LoggerFactory.getLogger(ApiGateway.class);

    /** Rate limit: нэг IP-с 60 секундэд 5-аас илүү оролдлогыг блоклоно */
    private static final int    RATE_LIMIT_MAX     = 5;
    private static final long   RATE_LIMIT_WINDOW  = 60L; // seconds
    private static final String RATE_KEY_PREFIX    = "borgol:ratelimit:";

    // "Private subnet" — зөвхөн ApiGateway эдгээрт хандана
    private final SoapAuthClient soap;
    private final JedisPool      pool;

    public ApiGateway(SoapAuthClient soap, JedisPool pool) {
        this.soap = soap;
        this.pool = pool;
    }

    // ── Javalin filter бүртгэл ───────────────────────────────────────────────

    /**
     * Javalin апп дээр бүх before-filter бүртгэнэ.
     * BorgolApiServer конструктороос нэг удаа дуудагдана.
     */
    public void registerFilters(Javalin app) {
        // 1. CORS — бүх endpoint-д
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin",  "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });
        app.options("/*", ctx -> ctx.status(200));

        // 2. Хурд хязгаарлалт — зөвхөн нэвтрэх/бүртгэх endpoint-д
        app.before("/api/auth/login",    this::rateLimitAuth);
        app.before("/api/auth/register", this::rateLimitAuth);
        app.before("/api/soap/login",    this::rateLimitAuth);
        app.before("/api/soap/register", this::rateLimitAuth);

        // 3. Хүсэлт аудит лог
        app.before(ctx -> log.debug("[GW] {} {}", ctx.method(), ctx.path()));
    }

    // ── Нэвтрэлт баталгаажуулалт ─────────────────────────────────────────────

    /**
     * SOAP → JWT fallback нэвтрэлт.
     * BorgolApiServer handler-с дуудагдана — authRequired/authOptional-г орлоно.
     *
     * @param ctx      Javalin context
     * @param required true бол нэвтрээгүй тохиолдолд 401 илгээнэ
     * @return         userId эсвэл null (required=false бол 0)
     */
    public Integer authenticate(Context ctx, boolean required) {
        String authHeader = ctx.header("Authorization");

        // query param fallback — EventSource браузер custom header дэмжихгүй
        if (authHeader == null || authHeader.isBlank()) {
            String tokenParam = ctx.queryParam("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                authHeader = "Bearer " + tokenParam;
            }
        }

        if (authHeader == null || authHeader.isBlank()) {
            if (required) ctx.status(401).json(Map.of("error", "Authentication required"));
            return required ? null : 0;
        }

        // ── Step 1: SOAP баталгаажуулалт ────────────────────────────────────
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        try {
            SoapAuthClient.ValidationResult r = soap.validateToken(token);
            if (r.valid() && r.userId() != null) return r.userId();
        } catch (Exception ignored) {
            // SOAP унасан → local JWT-р fallback хийнэ
        }

        // ── Step 2: Local JWT fallback ───────────────────────────────────────
        Integer userId = JwtUtil.getUserId(authHeader);
        if (userId != null) return userId;

        if (required) ctx.status(401).json(Map.of("error", "Invalid or expired token"));
        return required ? null : 0;
    }

    // ── Хурд хязгаарлалт (Rate Limiting) ─────────────────────────────────────

    /**
     * Redis INCR+EXPIRE аргын хурд хязгаарлалт.
     *
     * Алгоритм (Fixed Window Counter):
     *   key = "borgol:ratelimit:{ip}"
     *   INCR key → count++
     *   count == 1 → EXPIRE key 60  (цонх нээнэ)
     *   count > 5  → 429 буцаана, handler-г зогсооно
     *
     * Fail-open: Redis алдаа гарвал хүсэлтийг нэвтрүүлнэ.
     * Шалтгаан: Availability > Security (course project scale).
     */
    private void rateLimitAuth(Context ctx) {
        String key = RATE_KEY_PREFIX + ctx.ip();
        try (Jedis jedis = pool.getResource()) {
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, RATE_LIMIT_WINDOW);
            }
            if (count > RATE_LIMIT_MAX) {
                log.warn("[GW] Rate limit hit: ip={}, count={}", ctx.ip(), count);
                ctx.status(429).json(Map.of(
                        "error", "Too many attempts. Please try again in 60 seconds."
                ));
                ctx.skipRemainingHandlers();
            }
        } catch (Exception e) {
            log.warn("[GW] Rate limit Redis error — fail-open: {}", e.getMessage());
        }
    }
}
