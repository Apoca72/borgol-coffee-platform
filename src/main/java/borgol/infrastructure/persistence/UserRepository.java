package borgol.infrastructure.persistence;

import borgol.core.domain.User;
import borgol.core.ports.UserRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.LinkedHashMap;

/**
 * Хэрэглэгч, нийгмийн граф, мэдэгдлийн SQL үйлдлүүд.
 * Загвар: Repository (GoF Data Access) — UserRepositoryPort-г хэрэгжүүлнэ.
 */
public class UserRepository implements UserRepositoryPort {

    private final DatabaseConnection db;

    public UserRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── User CRUD ─────────────────────────────────────────────────────────────

    @Override
    public Optional<User> findUserById(int id) {
        String sql = "SELECT * FROM borgol_users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        String sql = "SELECT * FROM borgol_users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        String sql = "SELECT * FROM borgol_users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public User createUser(String username, String email, String passwordHash) {
        String sql = "INSERT INTO borgol_users (username, email, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findUserById(keys.getInt(1)).orElseThrow();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("User creation failed");
    }

    @Override
    public void deleteUser(int id) {
        String sql = "DELETE FROM borgol_users WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void updateUser(int id, String bio, String avatarUrl, String expertiseLevel) {
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            String sql = "UPDATE borgol_users SET bio=?, avatar_url=?, expertise_level=? WHERE id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, bio);
                ps.setString(2, avatarUrl);
                ps.setString(3, expertiseLevel);
                ps.setInt(4, id);
                ps.executeUpdate();
            } catch (SQLException e) { throw new RuntimeException(e); }
        } else {
            String sql = "UPDATE borgol_users SET bio=?, expertise_level=? WHERE id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, bio);
                ps.setString(2, expertiseLevel);
                ps.setInt(3, id);
                ps.executeUpdate();
            } catch (SQLException e) { throw new RuntimeException(e); }
        }
    }

    // ── Flavor preferences ────────────────────────────────────────────────────

    @Override
    public List<String> getUserFlavorPrefs(int userId) {
        String sql = "SELECT flavor FROM user_flavor_prefs WHERE user_id = ?";
        List<String> prefs = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) prefs.add(rs.getString("flavor"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return prefs;
    }

    @Override
    public void setUserFlavorPrefs(int userId, List<String> flavors) {
        try {
            try (PreparedStatement del = conn().prepareStatement(
                    "DELETE FROM user_flavor_prefs WHERE user_id = ?")) {
                del.setInt(1, userId);
                del.executeUpdate();
            }
            if (flavors != null) {
                try (PreparedStatement ins = conn().prepareStatement(
                        "INSERT INTO user_flavor_prefs (user_id, flavor) VALUES (?, ?)")) {
                    for (String f : flavors) {
                        ins.setInt(1, userId);
                        ins.setString(2, f);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Social graph counts ───────────────────────────────────────────────────

    @Override
    public int getFollowerCount(int userId) {
        return count("SELECT COUNT(*) FROM user_follows WHERE following_id = ?", userId);
    }

    @Override
    public int getFollowingCount(int userId) {
        return count("SELECT COUNT(*) FROM user_follows WHERE follower_id = ?", userId);
    }

    @Override
    public int getUserRecipeCount(int userId) {
        return count("SELECT COUNT(*) FROM recipes WHERE author_id = ?", userId);
    }

    // ── Follow / unfollow ─────────────────────────────────────────────────────

    @Override
    public boolean isFollowing(int followerId, int followingId) {
        String sql = "SELECT COUNT(*) FROM user_follows WHERE follower_id=? AND following_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void followUser(int followerId, int followingId) {
        String sql = "INSERT INTO user_follows (follower_id, following_id) VALUES (?,?) ON CONFLICT (follower_id, following_id) DO NOTHING";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void unfollowUser(int followerId, int followingId) {
        String sql = "DELETE FROM user_follows WHERE follower_id=? AND following_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── User search / listing ─────────────────────────────────────────────────

    @Override
    public List<User> searchUsers(String query) {
        String sql = "SELECT * FROM borgol_users WHERE LOWER(username) LIKE ? OR LOWER(bio) LIKE ? LIMIT 20";
        String pattern = "%" + query.toLowerCase() + "%";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return users;
    }

    @Override
    public List<User> findAllUsers(int limit) {
        String sql = """
            SELECT * FROM borgol_users
            ORDER BY (SELECT COUNT(*) FROM user_follows WHERE following_id = borgol_users.id) DESC,
                     created_at DESC
            LIMIT ?
            """;
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return users;
    }

    @Override
    public List<User> getFollowingUsers(int userId) {
        String sql = """
            SELECT u.* FROM borgol_users u
            JOIN user_follows f ON f.following_id = u.id
            WHERE f.follower_id = ?
            ORDER BY f.created_at DESC
            """;
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return users;
    }

    @Override
    public List<User> getFollowerUsers(int userId) {
        String sql = """
            SELECT u.* FROM borgol_users u
            JOIN user_follows f ON f.follower_id = u.id
            WHERE f.following_id = ?
            ORDER BY f.created_at DESC
            """;
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapUser(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return users;
    }

    // ── Block / unblock ───────────────────────────────────────────────────────

    @Override
    public void blockUser(int blockerId, int blockedId) {
        String sql = "INSERT INTO blocked_users (blocker_id, blocked_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        // Also unfollow both ways
        String unfollow = "DELETE FROM user_follows WHERE (follower_id = ? AND following_id = ?) OR (follower_id = ? AND following_id = ?)";
        try (PreparedStatement ps = conn().prepareStatement(unfollow)) {
            ps.setInt(1, blockerId); ps.setInt(2, blockedId);
            ps.setInt(3, blockedId); ps.setInt(4, blockerId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void unblockUser(int blockerId, int blockedId) {
        String sql = "DELETE FROM blocked_users WHERE blocker_id = ? AND blocked_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean isBlocked(int blockerId, int blockedId) {
        return count("SELECT COUNT(*) FROM blocked_users WHERE blocker_id = ? AND blocked_id = ?",
                     blockerId, blockedId) > 0;
    }

    // ── Hashtag following ─────────────────────────────────────────────────────

    @Override
    public void followHashtag(int userId, String tag) {
        String sql = "INSERT INTO user_hashtags (user_id, tag) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, tag.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void unfollowHashtag(int userId, String tag) {
        String sql = "DELETE FROM user_hashtags WHERE user_id = ? AND tag = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, tag.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<String> getUserHashtags(int userId) {
        List<String> tags = new ArrayList<>();
        String sql = "SELECT tag FROM user_hashtags WHERE user_id = ? ORDER BY tag";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) tags.add(rs.getString("tag"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return tags;
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @Override
    public void createNotification(int userId, String type, int fromUserId, int contentId, String message) {
        if (userId == fromUserId) return; // don't notify yourself
        String sql = """
            INSERT INTO notifications (user_id, type, from_user_id, content_id, message)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setInt(3, fromUserId);
            ps.setInt(4, contentId);
            ps.setString(5, message);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Map<String, Object>> getNotifications(int userId, int limit) {
        String sql = """
            SELECT n.*, u.username AS from_username, u.avatar_url AS from_avatar
            FROM notifications n
            LEFT JOIN borgol_users u ON u.id = n.from_user_id
            WHERE n.user_id = ?
            ORDER BY n.created_at DESC
            LIMIT ?
            """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("id",          rs.getInt("id"));
                    n.put("type",        rs.getString("type"));
                    n.put("fromUserId",  rs.getInt("from_user_id"));
                    n.put("fromUsername",rs.getString("from_username"));
                    n.put("fromAvatar",  rs.getString("from_avatar"));
                    n.put("contentId",   rs.getInt("content_id"));
                    n.put("message",     rs.getString("message"));
                    n.put("isRead",      rs.getBoolean("is_read"));
                    n.put("createdAt",   rs.getString("created_at"));
                    result.add(n);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    @Override
    public void markNotificationsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public int getUnreadNotificationCount(int userId) {
        return count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE", userId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash")
        );
        u.setBio(nullToEmpty(rs.getString("bio")));
        u.setAvatarUrl(nullToEmpty(rs.getString("avatar_url")));
        u.setExpertiseLevel(nullToEmpty(rs.getString("expertise_level")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime().toString());
        return u;
    }

    private int count(String sql, int param) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int count(String sql, int p1, int p2) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p1);
            ps.setInt(2, p2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static String nullToEmpty(String s) { return s != null ? s : ""; }
}
