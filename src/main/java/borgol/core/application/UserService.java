package borgol.core.application;

import borgol.core.domain.User;
import borgol.core.ports.UserRepositoryPort;
import borgol.infrastructure.cache.CacheKeyBuilder;
import borgol.infrastructure.cache.RedisClient;
import borgol.infrastructure.email.EmailService;
import borgol.infrastructure.messaging.RedisEventBus;
import borgol.infrastructure.security.JwtUtil;
import borgol.infrastructure.security.PasswordUtil;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepositoryPort repo;
    private final RedisEventBus      eventBus;
    private final Gson               gson = new Gson();

    public record AuthResult(String token, UserView user) {}

    public record UserView(
        int id, String username, String email, String bio, String avatarUrl,
        String expertiseLevel, List<String> flavorPrefs,
        int followerCount, int followingCount, int recipeCount,
        boolean isFollowing, String createdAt
    ) {}

    public UserService(UserRepositoryPort repo, RedisEventBus eventBus) {
        this.repo     = repo;
        this.eventBus = eventBus;
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    private void cacheEvict(String key) {
        try {
            RedisClient.get().del(key);
            log.debug("[Cache EVICT] {}", key);
        } catch (Exception e) {
            log.debug("[Cache] Redis устгахад алдаа: {} — {}", key, e.getMessage());
        }
    }

    private <T> T cacheGet(String key, Class<T> type) {
        try {
            String json = RedisClient.get().get(key);
            if (json != null) {
                log.debug("[Cache HIT] {}", key);
                return gson.fromJson(json, type);
            }
        } catch (Exception e) {
            log.debug("[Cache] Redis уншихад алдаа: {} — {}", key, e.getMessage());
        }
        log.debug("[Cache MISS] {}", key);
        return null;
    }

    private void cachePut(String key, Object value, int ttlSeconds) {
        try {
            RedisClient.get().setex(key, ttlSeconds, gson.toJson(value));
        } catch (Exception e) {
            log.debug("[Cache] Redis бичихэд алдаа: {} — {}", key, e.getMessage());
        }
    }

    private void cacheUserHash(int userId, UserView view) {
        try (Jedis jedis = RedisClient.get().pool().getResource()) {
            String key = CacheKeyBuilder.forUser(userId);
            jedis.hset(key, Map.of(
                "id",            String.valueOf(view.id()),
                "username",      view.username() != null ? view.username() : "",
                "bio",           view.bio() != null ? view.bio() : "",
                "avatarUrl",     view.avatarUrl() != null ? view.avatarUrl() : "",
                "expertiseLevel",view.expertiseLevel() != null ? view.expertiseLevel() : "BEGINNER",
                "followerCount", String.valueOf(view.followerCount()),
                "followingCount",String.valueOf(view.followingCount()),
                "recipeCount",   String.valueOf(view.recipeCount())
            ));
            jedis.expire(key, 600L);
            log.debug("[Cache HASH SET] borgol:user:{}", userId);
        } catch (Exception e) {
            log.debug("[Cache] Hash бичихэд алдаа: {}", e.getMessage());
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public AuthResult register(String username, String email, String password) {
        if (username == null || username.isBlank())  throw new IllegalArgumentException("Username is required");
        if (email == null    || email.isBlank())     throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be ≥ 6 characters");
        if (!email.contains("@"))                    throw new IllegalArgumentException("Invalid email address");
        if (username.length() < 3 || username.length() > 50) throw new IllegalArgumentException("Username must be 3–50 characters");
        if (!username.matches("[a-zA-Z0-9_]+"))     throw new IllegalArgumentException("Username: letters, numbers, underscores only");

        if (repo.findUserByEmail(email).isPresent())      throw new IllegalArgumentException("Email already registered");
        if (repo.findUserByUsername(username).isPresent()) throw new IllegalArgumentException("Username already taken");

        String hash  = PasswordUtil.hash(password);
        User   user  = repo.createUser(username, email, hash);
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        EmailService.get().sendWelcomeEmail(email, username);
        return new AuthResult(token, toView(user, 0, false));
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank())       throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        User user = repo.findUserByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!PasswordUtil.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");

        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    // ── User queries ──────────────────────────────────────────────────────────

    public UserView getMe(int userId) {
        String key = CacheKeyBuilder.forUser(userId);
        UserView cached = cacheGet(key, UserView.class);
        if (cached != null) return cached;

        User user = repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserView view = toView(user, 0, false);
        cachePut(key, view, 600);
        cacheUserHash(userId, view);
        return view;
    }

    public void deleteUser(int userId) {
        repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        repo.deleteUser(userId);
    }

    public UserView getUserProfile(int targetId, int currentUserId) {
        User user = repo.findUserById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isFollowing = currentUserId > 0 && repo.isFollowing(currentUserId, targetId);
        return toView(user, currentUserId, isFollowing);
    }

    public UserView updateProfile(int userId, String bio, String avatarUrl, String expertiseLevel,
                                  List<String> flavorPrefs) {
        List<String> validLevels = List.of("BEGINNER", "ENTHUSIAST", "BARISTA", "EXPERT");
        if (expertiseLevel != null && !validLevels.contains(expertiseLevel.toUpperCase()))
            expertiseLevel = "BEGINNER";

        repo.updateUser(userId,
            bio != null ? bio : "",
            avatarUrl != null ? avatarUrl : "",
            expertiseLevel != null ? expertiseLevel.toUpperCase() : "BEGINNER");

        if (flavorPrefs != null) repo.setUserFlavorPrefs(userId, flavorPrefs);
        cacheEvict(CacheKeyBuilder.forUser(userId));
        return getMe(userId);
    }

    public void followUser(int followerId, int followingId) {
        if (followerId == followingId) throw new IllegalArgumentException("Cannot follow yourself");
        repo.followUser(followerId, followingId);
        repo.createNotification(followingId, "follow", followerId, 0, "started following you");
        eventBus.publish(followingId, "follow", "Someone started following you");
    }

    public void unfollowUser(int followerId, int followingId) {
        repo.unfollowUser(followerId, followingId);
    }

    public List<UserView> searchUsers(String query, int currentUserId) {
        if (query == null || query.isBlank()) return List.of();
        return repo.searchUsers(query).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<UserView> getAllUsers(int currentUserId) {
        return repo.findAllUsers(60).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<UserView> getFollowingUsers(int userId, int currentUserId) {
        return repo.getFollowingUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<UserView> getFollowerUsers(int userId, int currentUserId) {
        return repo.getFollowerUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getNotifications(int userId) {
        return repo.getNotifications(userId, 50);
    }

    public void markNotificationsRead(int userId) {
        repo.markNotificationsRead(userId);
    }

    public Map<String, Object> getNotificationCount(int userId) {
        return Map.of("unread", repo.getUnreadNotificationCount(userId));
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    public void blockUser(int blockerId, int blockedId) {
        if (blockerId == blockedId) throw new IllegalArgumentException("Cannot block yourself");
        repo.blockUser(blockerId, blockedId);
    }

    public void unblockUser(int blockerId, int blockedId) {
        repo.unblockUser(blockerId, blockedId);
    }

    public boolean isBlocked(int blockerId, int blockedId) {
        return repo.isBlocked(blockerId, blockedId);
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    public void followHashtag(int userId, String tag) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("Tag is required");
        repo.followHashtag(userId, tag.toLowerCase().replaceAll("[^a-z0-9_]", ""));
    }

    public void unfollowHashtag(int userId, String tag) {
        repo.unfollowHashtag(userId, tag.toLowerCase());
    }

    public List<String> getUserHashtags(int userId) {
        return repo.getUserHashtags(userId);
    }

    // ── Admin checks ──────────────────────────────────────────────────────────

    public boolean isAdmin(int userId) {
        if (userId == 1) return true;
        String adminEmail = System.getenv("ADMIN_EMAIL");
        if (adminEmail != null && !adminEmail.isBlank()) {
            return repo.findUserById(userId)
                .map(u -> adminEmail.equalsIgnoreCase(u.getEmail()))
                .orElse(false);
        }
        return false;
    }

    // ── View mapping ──────────────────────────────────────────────────────────

    public UserView toView(User u, int currentUserId, boolean isFollowing) {
        List<String> prefs = repo.getUserFlavorPrefs(u.getId());
        return new UserView(
            u.getId(), u.getUsername(), u.getEmail(),
            u.getBio(), u.getAvatarUrl(), u.getExpertiseLevel(),
            prefs,
            repo.getFollowerCount(u.getId()),
            repo.getFollowingCount(u.getId()),
            repo.getUserRecipeCount(u.getId()),
            isFollowing,
            u.getCreatedAt()
        );
    }
}
