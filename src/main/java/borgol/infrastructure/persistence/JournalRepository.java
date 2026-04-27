package borgol.infrastructure.persistence;

import borgol.core.domain.BeanBag;
import borgol.core.domain.BrewJournalEntry;
import borgol.core.ports.JournalRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Дарлагын тэмдэглэл, шош уут, статистик.
 * Загвар: Repository — JournalRepositoryPort хэрэгжүүлнэ.
 */
public class JournalRepository implements JournalRepositoryPort {

    private final DatabaseConnection db;

    public JournalRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── Journal entries ───────────────────────────────────────────────────────

    @Override
    public List<BrewJournalEntry> getJournalEntries(int userId) {
        String sql = "SELECT * FROM brew_journal WHERE user_id=? ORDER BY created_at DESC";
        List<BrewJournalEntry> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapJournal(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<BrewJournalEntry> findJournalEntry(int id, int userId) {
        String sql = "SELECT * FROM brew_journal WHERE id=? AND user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapJournal(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public BrewJournalEntry createJournalEntry(BrewJournalEntry e) {
        String sql = """
            INSERT INTO brew_journal
              (user_id, coffee_bean, origin, roast_level, brew_method, grind_size,
               water_temp_c, dose_grams, yield_grams, brew_time_sec,
               rating_aroma, rating_flavor, rating_acidity, rating_body,
               rating_sweetness, rating_finish, notes, weather_data)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, e.getUserId());
            ps.setString(2, nvl(e.getCoffeeBean()));
            ps.setString(3, nvl(e.getOrigin()));
            ps.setString(4, nvl(e.getRoastLevel()));
            ps.setString(5, nvl(e.getBrewMethod()));
            ps.setString(6, nvl(e.getGrindSize()));
            ps.setInt(7, e.getWaterTempC());
            ps.setDouble(8, e.getDoseGrams());
            ps.setDouble(9, e.getYieldGrams());
            ps.setInt(10, e.getBrewTimeSec());
            ps.setInt(11, clamp(e.getRatingAroma()));
            ps.setInt(12, clamp(e.getRatingFlavor()));
            ps.setInt(13, clamp(e.getRatingAcidity()));
            ps.setInt(14, clamp(e.getRatingBody()));
            ps.setInt(15, clamp(e.getRatingSweetness()));
            ps.setInt(16, clamp(e.getRatingFinish()));
            ps.setString(17, nvl(e.getNotes()));
            ps.setString(18, nvl(e.getWeatherData()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findJournalEntry(keys.getInt(1), e.getUserId()).orElseThrow();
                }
            }
        } catch (SQLException ex) { throw new RuntimeException(ex); }
        throw new RuntimeException("Journal entry creation failed");
    }

    @Override
    public BrewJournalEntry updateJournalEntry(BrewJournalEntry e) {
        String sql = """
            UPDATE brew_journal SET
              coffee_bean=?, origin=?, roast_level=?, brew_method=?, grind_size=?,
              water_temp_c=?, dose_grams=?, yield_grams=?, brew_time_sec=?,
              rating_aroma=?, rating_flavor=?, rating_acidity=?, rating_body=?,
              rating_sweetness=?, rating_finish=?, notes=?, weather_data=?
            WHERE id=? AND user_id=?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, nvl(e.getCoffeeBean()));
            ps.setString(2, nvl(e.getOrigin()));
            ps.setString(3, nvl(e.getRoastLevel()));
            ps.setString(4, nvl(e.getBrewMethod()));
            ps.setString(5, nvl(e.getGrindSize()));
            ps.setInt(6, e.getWaterTempC());
            ps.setDouble(7, e.getDoseGrams());
            ps.setDouble(8, e.getYieldGrams());
            ps.setInt(9, e.getBrewTimeSec());
            ps.setInt(10, clamp(e.getRatingAroma()));
            ps.setInt(11, clamp(e.getRatingFlavor()));
            ps.setInt(12, clamp(e.getRatingAcidity()));
            ps.setInt(13, clamp(e.getRatingBody()));
            ps.setInt(14, clamp(e.getRatingSweetness()));
            ps.setInt(15, clamp(e.getRatingFinish()));
            ps.setString(16, nvl(e.getNotes()));
            ps.setString(17, nvl(e.getWeatherData()));
            ps.setInt(18, e.getId());
            ps.setInt(19, e.getUserId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Entry not found or not authorized");
        } catch (SQLException ex) { throw new RuntimeException(ex); }
        return findJournalEntry(e.getId(), e.getUserId()).orElseThrow();
    }

    @Override
    public boolean deleteJournalEntry(int id, int userId) {
        String sql = "DELETE FROM brew_journal WHERE id=? AND user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Bean bags ─────────────────────────────────────────────────────────────

    @Override
    public List<BeanBag> getBeanBags(int userId) {
        String sql = "SELECT * FROM bean_bags WHERE user_id=? ORDER BY created_at DESC";
        List<BeanBag> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBeanBag(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public BeanBag createBeanBag(BeanBag b) {
        String sql = """
            INSERT INTO bean_bags (user_id, name, roaster, origin, roast_level,
              roast_date, remaining_grams, rating, notes)
            VALUES (?,?,?,?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getUserId());
            ps.setString(2, nvl(b.getName()));
            ps.setString(3, nvl(b.getRoaster()));
            ps.setString(4, nvl(b.getOrigin()));
            ps.setString(5, nvl(b.getRoastLevel()));
            ps.setObject(6, b.getRoastDate() != null && !b.getRoastDate().isBlank()
                ? java.sql.Date.valueOf(b.getRoastDate()) : null);
            ps.setDouble(7, b.getRemainingGrams());
            ps.setInt(8, b.getRating());
            ps.setString(9, nvl(b.getNotes()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setId(keys.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return b;
    }

    @Override
    public BeanBag updateBeanBag(BeanBag b) {
        String sql = """
            UPDATE bean_bags SET name=?, roaster=?, origin=?, roast_level=?,
              roast_date=?, remaining_grams=?, rating=?, notes=?
            WHERE id=? AND user_id=?""";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, nvl(b.getName()));
            ps.setString(2, nvl(b.getRoaster()));
            ps.setString(3, nvl(b.getOrigin()));
            ps.setString(4, nvl(b.getRoastLevel()));
            ps.setObject(5, b.getRoastDate() != null && !b.getRoastDate().isBlank()
                ? java.sql.Date.valueOf(b.getRoastDate()) : null);
            ps.setDouble(6, b.getRemainingGrams());
            ps.setInt(7, b.getRating());
            ps.setString(8, nvl(b.getNotes()));
            ps.setInt(9, b.getId());
            ps.setInt(10, b.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return b;
    }

    @Override
    public void deleteBeanBag(int id, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM bean_bags WHERE id=? AND user_id=?")) {
            ps.setInt(1, id); ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Journal stats ─────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getJournalStats(int userId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Statement s = conn().createStatement()) {
            try (ResultSet rs = s.executeQuery(
                    "SELECT COUNT(*) AS cnt, " +
                    "AVG((rating_aroma+rating_flavor+rating_acidity+rating_body+rating_sweetness+rating_finish)/6.0) AS avg_rating " +
                    "FROM brew_journal WHERE user_id=" + userId)) {
                if (rs.next()) {
                    stats.put("totalEntries", rs.getInt("cnt"));
                    stats.put("avgRating", rs.getDouble("avg_rating"));
                }
            }
            List<Map<String,Object>> methods = new ArrayList<>();
            try (ResultSet rs = s.executeQuery(
                    "SELECT brew_method, COUNT(*) AS cnt FROM brew_journal " +
                    "WHERE user_id=" + userId + " AND brew_method IS NOT NULL AND brew_method<>'' " +
                    "GROUP BY brew_method ORDER BY cnt DESC")) {
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("method", rs.getString("brew_method"));
                    row.put("count", rs.getInt("cnt"));
                    methods.add(row);
                }
            }
            stats.put("brewMethods", methods);
            List<Map<String,Object>> monthly = new ArrayList<>();
            try {
                try (ResultSet rs = s.executeQuery(
                        "SELECT TO_CHAR(created_at,'YYYY-MM') AS month, " +
                        "AVG((rating_aroma+rating_flavor+rating_acidity+rating_body+rating_sweetness+rating_finish)/6.0) AS avg_rating, " +
                        "COUNT(*) AS cnt FROM brew_journal WHERE user_id=" + userId +
                        " AND created_at >= NOW() - INTERVAL '12 months' " +
                        "GROUP BY month ORDER BY month")) {
                    while (rs.next()) {
                        Map<String,Object> row = new LinkedHashMap<>();
                        row.put("month", rs.getString("month"));
                        row.put("avgRating", rs.getDouble("avg_rating"));
                        row.put("count", rs.getInt("cnt"));
                        monthly.add(row);
                    }
                }
            } catch (Exception ignored) { /* H2 fallback: skip monthly */ }
            stats.put("monthly", monthly);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return stats;
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private BrewJournalEntry mapJournal(ResultSet rs) throws SQLException {
        BrewJournalEntry e = new BrewJournalEntry();
        e.setId(rs.getInt("id"));
        e.setUserId(rs.getInt("user_id"));
        e.setCoffeeBean(nullToEmpty(rs.getString("coffee_bean")));
        e.setOrigin(nullToEmpty(rs.getString("origin")));
        e.setRoastLevel(nullToEmpty(rs.getString("roast_level")));
        e.setBrewMethod(nullToEmpty(rs.getString("brew_method")));
        e.setGrindSize(nullToEmpty(rs.getString("grind_size")));
        e.setWaterTempC(rs.getInt("water_temp_c"));
        e.setDoseGrams(rs.getDouble("dose_grams"));
        e.setYieldGrams(rs.getDouble("yield_grams"));
        e.setBrewTimeSec(rs.getInt("brew_time_sec"));
        e.setRatingAroma(rs.getInt("rating_aroma"));
        e.setRatingFlavor(rs.getInt("rating_flavor"));
        e.setRatingAcidity(rs.getInt("rating_acidity"));
        e.setRatingBody(rs.getInt("rating_body"));
        e.setRatingSweetness(rs.getInt("rating_sweetness"));
        e.setRatingFinish(rs.getInt("rating_finish"));
        e.setNotes(nullToEmpty(rs.getString("notes")));
        e.setWeatherData(nullToEmpty(rs.getString("weather_data")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime().toString());
        return e;
    }

    private BeanBag mapBeanBag(ResultSet rs) throws SQLException {
        BeanBag b = new BeanBag();
        b.setId(rs.getInt("id"));
        b.setUserId(rs.getInt("user_id"));
        b.setName(nullToEmpty(rs.getString("name")));
        b.setRoaster(nullToEmpty(rs.getString("roaster")));
        b.setOrigin(nullToEmpty(rs.getString("origin")));
        b.setRoastLevel(nullToEmpty(rs.getString("roast_level")));
        java.sql.Date d = rs.getDate("roast_date");
        b.setRoastDate(d != null ? d.toString() : "");
        b.setRemainingGrams(rs.getDouble("remaining_grams"));
        b.setRating(rs.getInt("rating"));
        b.setNotes(nullToEmpty(rs.getString("notes")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) b.setCreatedAt(ts.toLocalDateTime().toString());
        return b;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(10, v)); }
    private static String nvl(String s)           { return s != null ? s : ""; }
    private static String nvl(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }
    private static String nullToEmpty(String s)    { return s != null ? s : ""; }
}
