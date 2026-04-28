package borgol.infrastructure.persistence;

import borgol.core.ports.AchievementRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Амжилтын badge-уудын SQL үйлдлүүд.
 * Загвар: Repository — AchievementRepositoryPort хэрэгжүүлнэ.
 */
public class AchievementRepository implements AchievementRepositoryPort {

    private final DatabaseConnection db;

    public AchievementRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    @Override
    public List<Map<String, Object>> getAchievements(int userId, Map<String, String[]> meta) {
        Set<String> earned = new HashSet<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT badge_id FROM user_achievements WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) earned.add(rs.getString("badge_id"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return meta.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            String[] v = e.getValue();
            m.put("id", e.getKey()); m.put("icon", v[0]); m.put("name", v[1]); m.put("description", v[2]);
            m.put("earned", earned.contains(e.getKey()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> checkAndAwardAchievements(int userId, Map<String, String[]> meta) {
        List<String> newlyEarned = new ArrayList<>();
        Set<String> alreadyHas = new HashSet<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT badge_id FROM user_achievements WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) alreadyHas.add(rs.getString("badge_id")); }
        } catch (SQLException e) { throw new RuntimeException(e); }

        Map<String, Integer> counts = new HashMap<>();
        String[][] countSqls = {
            {"first_brew",       "SELECT COUNT(*) FROM brew_journal WHERE user_id=" + userId},
            {"brew_10",          "SELECT COUNT(*) FROM brew_journal WHERE user_id=" + userId},
            {"brew_50",          "SELECT COUNT(*) FROM brew_journal WHERE user_id=" + userId},
            {"recipe_author",    "SELECT COUNT(*) FROM recipes WHERE author_id=" + userId},
            {"cafe_explorer",    "SELECT COUNT(DISTINCT cafe_id) FROM cafe_ratings WHERE user_id=" + userId},
            {"social_butterfly", "SELECT COUNT(*) FROM user_follows WHERE follower_id=" + userId},
            {"bean_collector",   "SELECT COUNT(*) FROM bean_bags WHERE user_id=" + userId},
            {"pour_over_pro",    "SELECT COUNT(*) FROM brew_journal WHERE user_id=" + userId + " AND LOWER(brew_method) LIKE '%pour%'"}
        };
        try (Statement s = conn().createStatement()) {
            for (String[] entry : countSqls) {
                try (ResultSet rs = s.executeQuery(entry[1])) {
                    if (rs.next()) counts.put(entry[0], rs.getInt(1));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }

        Map<String, Integer> thresholds = Map.of(
            "first_brew", 1, "brew_10", 10, "brew_50", 50, "recipe_author", 1,
            "cafe_explorer", 3, "social_butterfly", 5, "bean_collector", 5, "pour_over_pro", 5);

        for (Map.Entry<String, Integer> t : thresholds.entrySet()) {
            String badgeId = t.getKey();
            if (!alreadyHas.contains(badgeId) && counts.getOrDefault(badgeId, 0) >= t.getValue()) {
                try (PreparedStatement ps = conn().prepareStatement(
                        "INSERT INTO user_achievements (user_id, badge_id) VALUES (?,?) ON CONFLICT DO NOTHING")) {
                    ps.setInt(1, userId); ps.setString(2, badgeId); ps.executeUpdate();
                } catch (SQLException e) { throw new RuntimeException(e); }
                newlyEarned.add(badgeId);
            }
        }
        return newlyEarned;
    }
}
