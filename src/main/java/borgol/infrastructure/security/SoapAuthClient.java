package borgol.infrastructure.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * SOAP Authentication Service-тэй харилцах HTTP клиент.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Proxy / Adapter (GoF)
 * ════════════════════════════════════════════════════════════
 * Зорилго: JSON сервис нь SOAP протоколыг шууд мэдэхгүйгээр
 * auth сервисийг ашиглах боломж олгоно.
 * Java Record-оор type-safe хариу буцаана.
 *
 * SOA урсгал:
 *   Frontend (JSON) → JSON Service → SoapAuthClient → SOAP Service
 *                                                        ├── RegisterUser
 *                                                        ├── LoginUser
 *                                                        └── ValidateToken
 *
 * Хэрэгжүүлэлтийн онцлог:
 *  - Raw SOAP/XML over HTTP — JAXB/Spring-WS-г ашиглахгүй
 *    → JSON сервис SOAP dependency-гүй хөнгөн байна
 *  - Regex-гүй XML парсинг → tag нэрээр хайна (хялбар боловч хангалттай)
 *  - SOAP унасан үед "failed" буцаана → JSON сервис local JWT-р fallback хийнэ
 *
 * Зарчим: Graceful Degradation — нэг сервис унасан ч систем ажиллана
 */
public class SoapAuthClient {

    /**
     * SOAP service base URL — configurable via environment variable.
     *
     * Local development : SOAP_SERVICE_URL not set → http://localhost:8081/ws
     * Render deployment : set SOAP_SERVICE_URL=https://your-soap-service.onrender.com
     *                     in the borgol-api service's Render environment variables.
     */
    private final String soapUrl;
    // NAMESPACE → SOAP XML namespace, SOAP сервисийн конфигтой таарах ёстой
    private static final String NAMESPACE = "http://num.edu.mn/soapauth";
    // TIMEOUT 5 сек → SOAP сервис хойрго байвал хэрэглэгчийг удаан хүлээлгэхгүй
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // Java 11-с нэмэгдсэн стандарт HTTP клиент — гадны library хэрэггүй
    private final HttpClient http = HttpClient.newHttpClient();

    public SoapAuthClient() {
        String base = System.getenv().getOrDefault("SOAP_SERVICE_URL", "http://localhost:8081");
        // Accept both "https://host" and "https://host/ws"
        this.soapUrl = base.endsWith("/ws") ? base : base + "/ws";
        System.out.println("  [SOAP] Client → " + soapUrl);
    }

    // ── Нийтийн API ───────────────────────────────────────────────────────────

    // Java Record → immutable value object, getter автоматаар үүснэ
    // Бүтэц тодорхой, null-safe хариу буцаана

    /** Бүртгэх / нэвтрэх дуудлагын хариу */
    public record AuthResult(boolean success, String message,
                             String token,    Integer userId,  String username) {}

    /** Токен баталгаажуулалтын хариу */
    public record ValidationResult(boolean valid, Integer userId, String username) {}

    // ── RegisterUser ───────────────────────────────────────────────────────────

    /**
     * Calls SOAP RegisterUser.
     * Returns success=false with a message if SOAP service is down.
     */
    public AuthResult register(String username, String email, String password) {
        String body = soapEnvelope("""
            <auth:RegisterUserRequest xmlns:auth="%s">
              <auth:username>%s</auth:username>
              <auth:email>%s</auth:email>
              <auth:password>%s</auth:password>
            </auth:RegisterUserRequest>
            """.formatted(NAMESPACE,
                          escapeXml(username),
                          escapeXml(email),
                          escapeXml(password)));
        try {
            String xml = post(body);
            boolean success = extractBool(xml, "success");
            String  message = extractTag(xml,  "message");
            String  userIdStr = extractTag(xml, "userId");
            Integer userId  = userIdStr != null ? Integer.parseInt(userIdStr) : null;
            return new AuthResult(success, message, null, userId, username);
        } catch (Exception e) {
            return new AuthResult(false, "SOAP service unavailable: " + e.getMessage(),
                                  null, null, null);
        }
    }

    // ── LoginUser ──────────────────────────────────────────────────────────────

    /**
     * Calls SOAP LoginUser.
     * Returns success=false if credentials are wrong or SOAP is down.
     */
    public AuthResult login(String email, String password) {
        String body = soapEnvelope("""
            <auth:LoginUserRequest xmlns:auth="%s">
              <auth:email>%s</auth:email>
              <auth:password>%s</auth:password>
            </auth:LoginUserRequest>
            """.formatted(NAMESPACE,
                          escapeXml(email),
                          escapeXml(password)));
        try {
            String  xml      = post(body);
            boolean success  = extractBool(xml, "success");
            String  token    = extractTag(xml,  "token");
            String  message  = extractTag(xml,  "message");
            String  username = extractTag(xml,  "username");
            String  uidStr   = extractTag(xml,  "userId");
            Integer userId   = uidStr != null ? Integer.parseInt(uidStr) : null;
            return new AuthResult(success, message, token, userId, username);
        } catch (Exception e) {
            return new AuthResult(false, "SOAP service unavailable: " + e.getMessage(),
                                  null, null, null);
        }
    }

    // ── ValidateToken ──────────────────────────────────────────────────────────

    /**
     * Calls SOAP ValidateToken with Redis caching.
     *
     * Cache strategy (SOAP Cache Management — Lab 08 Bonus):
     *   Cache Hit  → "valid|userId|username" string parse хийж буцаана (SOAP дуудахгүй)
     *   Cache Miss → SOAP дуудаж, valid=true бол Redis-д 300 секунд хадгална
     *
     * Зөвхөн valid=true хариуг кэшэлнэ.
     * Шалтгаан: invalid token-уудыг дахин хурдан татгалздаг тул кэш хэрэггүй.
     *
     * Кэш түлхүүр: "borgol:soap:token:{hex(hashCode)}" — token-г хадгалахгүй.
     */
    public ValidationResult validateToken(String token) {
        if (token == null || token.isBlank())
            return new ValidationResult(false, null, null);

        // ── Cache Hit ────────────────────────────────────────────────────────
        String cacheKey = borgol.infrastructure.cache.CacheKeyBuilder.forSoapToken(
                Integer.toHexString(token.hashCode()));
        try {
            String cached = borgol.infrastructure.cache.RedisClient.get().get(cacheKey);
            if (cached != null) {
                // Формат: "true|42|coffee_master"
                String[] parts    = cached.split("\\|", 3);
                boolean  valid    = Boolean.parseBoolean(parts[0]);
                Integer  userId   = parts[1].isEmpty() ? null : Integer.parseInt(parts[1]);
                String   username = parts[2].isEmpty() ? null : parts[2];
                System.out.println("  [SOAP Cache HIT] token hash=" +
                    Integer.toHexString(token.hashCode()));
                return new ValidationResult(valid, userId, username);
            }
        } catch (Exception ignored) {
            // Redis унасан → SOAP дуудлага руу унана
        }

        // ── Cache Miss — SOAP дуудах ─────────────────────────────────────────
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

            // Зөвхөн valid token-г кэшэлнэ
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
            // SOAP unavailable — fall back to local JWT (handled in ApiGateway)
            return new ValidationResult(false, null, null);
        }
    }

    /**
     * SOAP token кэшийг устгана — Cache Invalidation (Lab 08 Bonus).
     * Хэрэглэгч гарсан эсвэл token хүчингүй болсон үед дуудна.
     * Дараагийн хүсэлт Redis-г тойрч SOAP сервист баталгаажуулна.
     *
     * @param token устгах токен
     */
    public void invalidateSoapToken(String token) {
        if (token == null || token.isBlank()) return;
        String cacheKey = borgol.infrastructure.cache.CacheKeyBuilder.forSoapToken(
                Integer.toHexString(token.hashCode()));
        try {
            borgol.infrastructure.cache.RedisClient.get().del(cacheKey);
            System.out.println("  [SOAP Cache EVICT] token hash=" +
                Integer.toHexString(token.hashCode()));
        } catch (Exception ignored) { }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String post(String soapBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(soapUrl))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                .timeout(TIMEOUT)
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    /** Wraps the payload in a SOAP 1.1 Envelope. */
    private String soapEnvelope(String payload) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    %s
                  </soap:Body>
                </soap:Envelope>
                """.formatted(payload.strip());
    }

    /**
     * Extracts the text content of the first matching XML tag.
     * Handles both prefixed (ns2:tag) and unprefixed (tag) element names.
     * Returns null if the tag is absent.
     */
    private String extractTag(String xml, String tagName) {
        // Try prefixed variants first, then plain
        for (String pattern : new String[]{":" + tagName + ">", "<" + tagName + ">"}) {
            int start = xml.indexOf(pattern);
            if (start >= 0) {
                int contentStart = start + pattern.length();
                int end = xml.indexOf("<", contentStart);
                if (end > contentStart) return xml.substring(contentStart, end).trim();
            }
        }
        return null;
    }

    private boolean extractBool(String xml, String tagName) {
        String val = extractTag(xml, tagName);
        return "true".equalsIgnoreCase(val);
    }

    /** Escapes the five XML predefined entities. */
    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&apos;");
    }
}
