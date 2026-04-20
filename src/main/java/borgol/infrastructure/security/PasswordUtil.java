package borgol.infrastructure.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Нууц үг хэш хийх — SHA-256 + санамсаргүй давс (random salt).
 *
 * ════════════════════════════════════════════════════════════
 * Аюулгүй байдлын загвар: Salted Hash
 * ════════════════════════════════════════════════════════════
 * DB-д хадгалах формат: "<saltHex>:<hashHex>"
 * Жишээ: "a3f2...:<sha256_hex>"
 *
 * Яагаад давс (salt) хэрэглэх вэ?
 *  - Rainbow table / dictionary attack-аас хамгаална
 *  - Хоёр хэрэглэгч нэг нууц үг ашиглавал хэш нь ялгаатай байна
 *
 * Тайлбар: Production-д bcrypt/Argon2 ашиглахыг зөвлөдөг,
 * гэхдээ боловсролын зорилгоор SHA-256 зөв хэрэгжүүлэлттэй.
 */
public class PasswordUtil {

    // SecureRandom → cryptographically secure санамсаргүй тоо үүсгэгч
    // Math.random() эсвэл Random() ашиглахаас аюулгүй
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String password) {
        // 16 byte (128 bit) санамсаргүй давс → хангалттай энтропи
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String saltHex = HexFormat.of().formatHex(salt);
        // Нууц үгийн өмнө давс нэмж хэш хийнэ → salt+password → SHA-256
        String hashHex = sha256(saltHex + password);
        // "salt:hash" форматаар хадгална → шалгахдаа salt-г салгаж авна
        return saltHex + ":" + hashHex;
    }

    public static boolean verify(String password, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        int colon = stored.indexOf(':');
        // DB-ийн хадгалсан salt-г гаргаж авна
        String saltHex  = stored.substring(0, colon);
        String expected = stored.substring(colon + 1);
        // Оролтын нууц үгийг ижил salt-тай хэш хийж харьцуулна
        String actual   = sha256(saltHex + password);
        // constantTimeEquals → timing attack-аас хамгаалсан харьцуулалт
        return constantTimeEquals(actual, expected);
    }

    private static String sha256(String data) {
        // MessageDigest → Java-ийн криптографийн стандарт API
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
