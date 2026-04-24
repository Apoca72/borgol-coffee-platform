package borgol.infrastructure.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Жингүй JWT хэрэгжүүлэлт — HMAC-SHA256 гарын үсэг ашиглана.
 *
 * ════════════════════════════════════════════════════════════
 * Зарчим: Zero External Dependency — Java standard library-г ашиглана
 * (javax.crypto.Mac, java.util.Base64)
 * ════════════════════════════════════════════════════════════
 *
 * Токены бүтэц (RFC 7519 JWT стандарт):
 *   base64url(header) . base64url(payload) . base64url(signature)
 *
 * Header:  {"alg":"HS256","typ":"JWT"}
 * Payload: {"sub":<userId>,"username":"...","exp":<unix_timestamp>}
 * Signature: HMAC-SHA256(header.payload, SECRET_KEY)
 *
 * Аюулгүй байдал:
 *  - Constant-time comparison → timing attack-аас хамгаалсан
 *  - 7 хоногийн хугацаатай → нэвтрэлт дуусахад дахин нэвтрэх шаардлагатай
 */
public class JwtUtil {

    // SECRET: JWT_SECRET env var-аас уншина; тест горимд dev default ашиглана.
    // Production-д заавал JWT_SECRET тохируулах шаардлагатай.
    private static final String SECRET = resolveSecret();
    private static final long   EXPIRY_SECS = 86400L * 7; // 7 хоног (секундаар)

    private static String resolveSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && !env.isBlank()) return env;
        System.err.println("[JWT] WARNING: JWT_SECRET env var not set — using insecure dev default");
        return "borgol-coffee-platform-jwt-secret-2026-num";
    }

    // ── Токен үүсгэх ──────────────────────────────────────────────────────────

    public static String createToken(int userId, String username) {
        // Header: алгоритм мэдэгдэнэ (HS256 = HMAC-SHA256)
        String header  = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long   exp     = Instant.now().getEpochSecond() + EXPIRY_SECS;
        // JSON injection-оос хамгаалж нэрийг escape хийнэ
        String safeUser = username.replace("\\", "\\\\").replace("\"", "\\\"");
        // sub = subject (хэрэглэгчийн ID), exp = expiry (дуусах хугацаа)
        String payload = b64(
            String.format("{\"sub\":%d,\"username\":\"%s\",\"exp\":%d}", userId, safeUser, exp));
        // Гарын үсэг: header.payload-г SECRET-р хэш хийнэ
        String sig = sign(header + "." + payload);
        return header + "." + payload + "." + sig;
    }

    // ── Токен баталгаажуулах ──────────────────────────────────────────────────

    /**
     * Хүчинтэй бол claims map буцаана, хүчингүй/дууссан бол null.
     * Claims: sub (Long) → userId, username (String), exp (Long)
     */
    public static Map<String, Object> verify(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;   // JWT формат буруу

        // ── Гарын үсгийг шалгана ─────────────────────────────────────────────
        // Хэрэв токен өөрчлөгдсөн бол гарын үсэг таарахгүй → null буцаана
        String expectedSig = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expectedSig, parts[2])) return null;

        // ── Payload-г decode хийнэ ───────────────────────────────────────────
        String payloadJson;
        try {
            payloadJson = new String(
                Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }

        Map<String, Object> claims = parseSimpleJson(payloadJson);

        // Check expiry
        Object exp = claims.get("exp");
        if (exp instanceof Long && (Long) exp < Instant.now().getEpochSecond()) return null;

        return claims;
    }

    /**
     * Extracts userId from "Bearer <token>" header value. Returns null if invalid.
     */
    public static Integer getUserId(String authHeader) {
        if (authHeader == null) return null;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Map<String, Object> claims = verify(token);
        if (claims == null) return null;
        Object sub = claims.get("sub");
        if (sub instanceof Long)    return ((Long) sub).intValue();
        if (sub instanceof Integer) return (Integer) sub;
        return null;
    }

    public static String getUsername(String authHeader) {
        if (authHeader == null) return null;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Map<String, Object> claims = verify(token);
        if (claims == null) return null;
        Object u = claims.get("username");
        return u != null ? u.toString() : null;
    }

    // ── Туслах аргууд ─────────────────────────────────────────────────────────

    private static String b64(String data) {
        // URL-safe Base64 (+ → -, / → _, = padding-гүй) → JWT стандартын дагуу
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }

    /**
     * Тогтмол цагийн харьцуулалт — timing attack-аас хамгаална.
     *
     * Ердийн equals() нь эхний ялгаатай тэмдэгт дээр зогсдог → хугацааны ялгааг
     * хэмжиж халдагч нууц үгийг таамаглах боломжтой болно.
     * XOR + OR-ийн аргаар бүх тэмдэгтийг тэгш хугацаанд шалгана.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0; // result == 0 → бүх байт тааралдсан
    }

    /**
     * Very simple JSON parser for flat {"key":value} payloads.
     * Handles string values (quoted) and numeric values (unquoted).
     */
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new HashMap<>();
        // Remove outer braces and parse key:value pairs
        String inner = json.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}"))  inner = inner.substring(0, inner.length() - 1);

        // Walk character by character to handle quoted strings properly
        int i = 0;
        while (i < inner.length()) {
            // Skip whitespace and commas
            while (i < inner.length() && (inner.charAt(i) == ',' || inner.charAt(i) == ' ')) i++;
            if (i >= inner.length()) break;

            // Parse key (always quoted)
            if (inner.charAt(i) != '"') { i++; continue; }
            int keyStart = i + 1;
            int keyEnd   = inner.indexOf('"', keyStart);
            if (keyEnd < 0) break;
            String key = inner.substring(keyStart, keyEnd);
            i = keyEnd + 1;

            // Skip colon
            while (i < inner.length() && inner.charAt(i) != ':') i++;
            i++; // skip ':'
            while (i < inner.length() && inner.charAt(i) == ' ') i++;

            // Parse value
            if (i < inner.length() && inner.charAt(i) == '"') {
                // String value
                int valStart = i + 1;
                int valEnd   = i + 1;
                while (valEnd < inner.length()) {
                    if (inner.charAt(valEnd) == '"' && inner.charAt(valEnd - 1) != '\\') break;
                    valEnd++;
                }
                map.put(key, inner.substring(valStart, valEnd).replace("\\\"", "\""));
                i = valEnd + 1;
            } else {
                // Numeric value
                int valStart = i;
                while (i < inner.length() && inner.charAt(i) != ',' && inner.charAt(i) != '}') i++;
                String numStr = inner.substring(valStart, i).trim();
                try { map.put(key, Long.parseLong(numStr)); }
                catch (NumberFormatException e) { map.put(key, numStr); }
            }
        }
        return map;
    }
}
