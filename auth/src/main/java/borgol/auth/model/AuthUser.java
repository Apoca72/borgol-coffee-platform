package borgol.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Auth-service user record (in-memory storage).
 *
 * Only stores auth credentials – NOT profile data.
 * Profile data (bio, phone, avatarUrl, etc.) belongs to the JSON service.
 *
 * Demonstrates Option 2 – Independent Databases:
 *   SOAP service  → Auth DB  (credentials + tokens)
 *   JSON service  → Profile DB (user profiles)
 */
@Data
@AllArgsConstructor
public class AuthUser {
    private int    id;
    private String username;
    private String email;          // stored lower-cased
    private String passwordHash;   // "saltHex:hashHex" (SHA-256 + random salt)
}
