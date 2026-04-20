package com.example.soapauth.service;

import com.example.soapauth.dto.*;
import com.example.soapauth.model.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core authentication business logic.
 *
 * Storage: two ConcurrentHashMaps (in-memory, resets on restart).
 *
 * JWT tokens use HMAC-SHA256 with the same secret as the JSON service,
 * so both services can verify tokens locally – but the JSON service's
 * middleware always calls SOAP ValidateToken to demonstrate integration.
 */
@Service
public class AuthService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiry-days:7}")
    private long jwtExpiryDays;

    // ── In-memory auth store ───────────────────────────────────────────────────
    private final Map<Integer, AuthUser> byId    = new ConcurrentHashMap<>();
    private final Map<String,  AuthUser> byEmail = new ConcurrentHashMap<>();
    private final AtomicInteger          idGen   = new AtomicInteger(1);

    // ── Operations ────────────────────────────────────────────────────────────

    public RegisterUserResponse register(String username, String email, String password) {
        if (username == null || username.isBlank())
            return new RegisterUserResponse(false, "Username is required", null);
        if (email == null || !email.contains("@"))
            return new RegisterUserResponse(false, "Valid email is required", null);
        if (password == null || password.length() < 6)
            return new RegisterUserResponse(false, "Password must be at least 6 characters", null);

        String key = email.toLowerCase();
        if (byEmail.containsKey(key))
            return new RegisterUserResponse(false, "Email already registered", null);

        String hash = hashPassword(password);
        int    id   = idGen.getAndIncrement();
        AuthUser user = new AuthUser(id, username.trim(), key, hash);
        byId.put(id, user);
        byEmail.put(key, user);

        System.out.printf("[SOAP] Registered user: id=%d username=%s email=%s%n", id, username, email);

        RegisterUserResponse resp = new RegisterUserResponse();
        resp.setSuccess(true);
        resp.setMessage("Registration successful");
        resp.setUserId(id);
        return resp;
    }

    public LoginUserResponse login(String email, String password) {
        if (email == null || password == null) {
            LoginUserResponse resp = new LoginUserResponse();
            resp.setSuccess(false);
            resp.setMessage("Email and password are required");
            return resp;
        }

        AuthUser user = byEmail.get(email.toLowerCase());
        if (user == null || !verifyPassword(password, user.getPasswordHash())) {
            LoginUserResponse resp = new LoginUserResponse();
            resp.setSuccess(false);
            resp.setMessage("Invalid email or password");
            return resp;
        }

        String token = createToken(user.getId(), user.getUsername());
        System.out.printf("[SOAP] Login success: userId=%d username=%s%n",
                          user.getId(), user.getUsername());

        LoginUserResponse resp = new LoginUserResponse();
        resp.setSuccess(true);
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setMessage("Login successful");
        return resp;
    }

    public ValidateTokenResponse validateToken(String token) {
        ValidateTokenResponse resp = new ValidateTokenResponse();
        if (token == null || token.isBlank()) {
            resp.setValid(false);
            return resp;
        }
        Map<String, Object> claims = verifyToken(token);
        if (claims == null) {
            resp.setValid(false);
            return resp;
        }
        resp.setValid(true);
        Object sub = claims.get("sub");
        if      (sub instanceof Long)    resp.setUserId(((Long) sub).intValue());
        else if (sub instanceof Integer) resp.setUserId((Integer) sub);
        Object username = claims.get("username");
        if (username != null) resp.setUsername(username.toString());
        return resp;
    }

    // ── JWT helpers (HMAC-SHA256, same format as JwtUtil.java) ────────────────

    private String createToken(int userId, String username) {
        String header  = b64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long   exp     = Instant.now().getEpochSecond() + jwtExpiryDays * 86400L;
        String safeUser = username.replace("\\", "\\\\").replace("\"", "\\\"");
        String payload = b64url(
            String.format("{\"sub\":%d,\"username\":\"%s\",\"exp\":%d}", userId, safeUser, exp));
        String sig = hmacSign(header + "." + payload);
        return header + "." + payload + "." + sig;
    }

    private Map<String, Object> verifyToken(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        String expected = hmacSign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(expected, parts[2])) return null;

        String json;
        try {
            json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }

        Map<String, Object> claims = parseJson(json);
        Object exp = claims.get("exp");
        if (exp instanceof Long && (Long) exp < Instant.now().getEpochSecond()) return null;
        return claims;
    }

    private String b64url(String data) {
        return Base64.getUrlEncoder().withoutPadding()
               .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                   .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("JWT signing failed", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }

    /** Minimal JSON parser for flat {key:value} objects (same as JwtUtil). */
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new HashMap<>();
        String inner = json.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}"))  inner = inner.substring(0, inner.length() - 1);
        int i = 0;
        while (i < inner.length()) {
            while (i < inner.length() && (inner.charAt(i) == ',' || inner.charAt(i) == ' ')) i++;
            if (i >= inner.length() || inner.charAt(i) != '"') { i++; continue; }
            int ks = i + 1, ke = inner.indexOf('"', ks);
            if (ke < 0) break;
            String key = inner.substring(ks, ke);
            i = ke + 1;
            while (i < inner.length() && inner.charAt(i) != ':') i++;
            i++;
            while (i < inner.length() && inner.charAt(i) == ' ') i++;
            if (i < inner.length() && inner.charAt(i) == '"') {
                int vs = i + 1, ve = i + 1;
                while (ve < inner.length()) {
                    if (inner.charAt(ve) == '"' && inner.charAt(ve - 1) != '\\') break;
                    ve++;
                }
                map.put(key, inner.substring(vs, ve).replace("\\\"", "\""));
                i = ve + 1;
            } else {
                int vs = i;
                while (i < inner.length() && inner.charAt(i) != ',' && inner.charAt(i) != '}') i++;
                try { map.put(key, Long.parseLong(inner.substring(vs, i).trim())); }
                catch (NumberFormatException e) { map.put(key, inner.substring(vs, i).trim()); }
            }
        }
        return map;
    }

    // ── Password helpers (SHA-256 + random salt) ──────────────────────────────

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltHex = HexFormat.of().formatHex(salt);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            String hashHex = HexFormat.of().formatHex(
                md.digest(password.getBytes(StandardCharsets.UTF_8)));
            return saltHex + ":" + hashHex;
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    private boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            String hashHex = HexFormat.of().formatHex(
                md.digest(password.getBytes(StandardCharsets.UTF_8)));
            return hashHex.equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }
}
