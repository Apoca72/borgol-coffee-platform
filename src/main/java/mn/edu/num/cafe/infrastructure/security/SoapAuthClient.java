package mn.edu.num.cafe.infrastructure.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP client that communicates with the SOAP Authentication Service (port 8081).
 *
 * Architecture:
 *   JSON Service (this)  →  SOAP Service
 *                              ├── RegisterUser
 *                              ├── LoginUser
 *                              └── ValidateToken
 *
 * The client sends raw SOAP/XML over HTTP and parses the text response
 * without JAXB, keeping the JSON service dependency-free of SOAP libraries.
 *
 * If the SOAP service is unreachable, all methods return a "failed" result
 * so the JSON service can fall back to its own JWT validation gracefully.
 */
public class SoapAuthClient {

    private static final String SOAP_URL  = "http://localhost:8081/ws";
    private static final String NAMESPACE = "http://num.edu.mn/soapauth";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient http = HttpClient.newHttpClient();

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Result of a register or login call. */
    public record AuthResult(boolean success, String message,
                             String token,    Integer userId,  String username) {}

    /** Result of a validateToken call. */
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
     * Calls SOAP ValidateToken.
     * Returns valid=false if the token is bad or SOAP is unreachable.
     */
    public ValidationResult validateToken(String token) {
        if (token == null || token.isBlank())
            return new ValidationResult(false, null, null);

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
            return new ValidationResult(valid, userId, username);
        } catch (Exception e) {
            // SOAP unavailable – fall back to local JWT (handled in BorgolApiServer)
            return new ValidationResult(false, null, null);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String post(String soapBody) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SOAP_URL))
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
