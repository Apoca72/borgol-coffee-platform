package mn.edu.num.cafe.infrastructure.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing with SHA-256 + random salt.
 * Format stored in DB: "<saltHex>:<hashHex>"
 *
 * Not production-grade (use bcrypt in production) but correct for educational use.
 */
public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        String hashHex = sha256(saltHex + password);
        return saltHex + ":" + hashHex;
    }

    public static boolean verify(String password, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        int colon = stored.indexOf(':');
        String saltHex  = stored.substring(0, colon);
        String expected = stored.substring(colon + 1);
        String actual   = sha256(saltHex + password);
        return constantTimeEquals(actual, expected);
    }

    private static String sha256(String data) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] hash = d.digest(data.getBytes("UTF-8"));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }
}
