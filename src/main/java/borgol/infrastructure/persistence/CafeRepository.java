package borgol.infrastructure.persistence;

import borgol.core.domain.CafeListing;
import borgol.core.ports.CafeRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Кафе жагсаалт, үнэлгээ, check-in.
 * Загвар: Repository — CafeRepositoryPort хэрэгжүүлнэ.
 */
public class CafeRepository implements CafeRepositoryPort {

    private final DatabaseConnection db;

    public CafeRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    @Override
    public List<CafeListing> findAllCafes(int currentUserId, String search, String district) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.*,
                   u.username AS submitted_by_username,
                   cr.rating  AS user_rating,
                   cr.review  AS user_review
            FROM cafes c
            LEFT JOIN borgol_users u ON c.submitted_by = u.id
            LEFT JOIN cafe_ratings cr ON cr.cafe_id = c.id AND cr.user_id = ?
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        params.add(currentUserId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(c.name) LIKE ? OR LOWER(c.description) LIKE ? OR LOWER(c.address) LIKE ?)");
            String p = "%" + search.toLowerCase() + "%";
            params.add(p); params.add(p); params.add(p);
        }
        if (district != null && !district.isBlank() && !district.equals("ALL")) {
            sql.append(" AND LOWER(c.district) LIKE ?");
            params.add("%" + district.toLowerCase() + "%");
        }
        sql.append(" ORDER BY c.avg_rating DESC, c.rating_count DESC");

        List<CafeListing> cafes = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) cafes.add(mapCafe(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return cafes;
    }

    @Override
    public Optional<CafeListing> findCafeById(int id, int currentUserId) {
        String sql = """
            SELECT c.*,
                   u.username AS submitted_by_username,
                   cr.rating  AS user_rating,
                   cr.review  AS user_review
            FROM cafes c
            LEFT JOIN borgol_users u ON c.submitted_by = u.id
            LEFT JOIN cafe_ratings cr ON cr.cafe_id = c.id AND cr.user_id = ?
            WHERE c.id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapCafe(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override
    public CafeListing createCafe(CafeListing c) {
        String sql = """
            INSERT INTO cafes (name, address, district, city, phone, description, hours, image_url, submitted_by, lat, lng)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, nvl(c.getAddress()));
            ps.setString(3, nvl(c.getDistrict()));
            ps.setString(4, nvl(c.getCity(), "Ulaanbaatar"));
            ps.setString(5, nvl(c.getPhone()));
            ps.setString(6, nvl(c.getDescription()));
            ps.setString(7, nvl(c.getHours()));
            ps.setString(8, nvl(c.getImageUrl()));
            if (c.getSubmittedBy() > 0) ps.setInt(9, c.getSubmittedBy());
            else ps.setNull(9, Types.INTEGER);
            if (c.getLat() != null) ps.setDouble(10, c.getLat()); else ps.setNull(10, Types.DOUBLE);
            if (c.getLng() != null) ps.setDouble(11, c.getLng()); else ps.setNull(11, Types.DOUBLE);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findCafeById(keys.getInt(1), c.getSubmittedBy()).orElseThrow();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Cafe creation failed");
    }

    @Override
    public void updateCafeCoordinates(int cafeId, double lat, double lng) {
        String sql = "UPDATE cafes SET lat = ?, lng = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lng);
            ps.setInt(3, cafeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<CafeListing> findCafesNearby(int currentUserId,
                                              double minLat, double maxLat,
                                              double minLng, double maxLng) {
        String sql = """
            SELECT c.*,
                   u.username AS submitted_by_username,
                   cr.rating  AS user_rating,
                   cr.review  AS user_review
            FROM cafes c
            LEFT JOIN borgol_users u ON c.submitted_by = u.id
            LEFT JOIN cafe_ratings cr ON cr.cafe_id = c.id AND cr.user_id = ?
            WHERE c.lat IS NOT NULL AND c.lng IS NOT NULL
              AND c.lat BETWEEN ? AND ?
              AND c.lng BETWEEN ? AND ?
            ORDER BY c.avg_rating DESC
            """;
        List<CafeListing> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setDouble(2, minLat);
            ps.setDouble(3, maxLat);
            ps.setDouble(4, minLng);
            ps.setDouble(5, maxLng);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapCafe(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    @Override
    public boolean rateCafe(int userId, int cafeId, int rating, String review) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");

        String merge = """
            INSERT INTO cafe_ratings (user_id, cafe_id, rating, review)
            VALUES (?,?,?,?)
            ON CONFLICT (user_id, cafe_id) DO UPDATE SET
                rating = EXCLUDED.rating,
                review = EXCLUDED.review
            """;
        try (PreparedStatement ps = conn().prepareStatement(merge)) {
            ps.setInt(1, userId);
            ps.setInt(2, cafeId);
            ps.setInt(3, rating);
            ps.setString(4, nvl(review));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }

        String recalc = """
            UPDATE cafes SET
                avg_rating   = (SELECT AVG(CAST(rating AS DOUBLE PRECISION)) FROM cafe_ratings WHERE cafe_id = ?),
                rating_count = (SELECT COUNT(*) FROM cafe_ratings WHERE cafe_id = ?)
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(recalc)) {
            ps.setInt(1, cafeId); ps.setInt(2, cafeId); ps.setInt(3, cafeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return true;
    }

    @Override
    public boolean isCafesSeeded() {
        return countNoParam("SELECT COUNT(*) FROM cafes") > 0;
    }

    @Override
    public Map<String, Object> checkIn(int cafeId, int userId, String note) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO cafe_checkins (cafe_id, user_id, note) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, cafeId); ps.setInt(2, userId); ps.setString(3, nvl(note));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return Map.of("id", keys.getInt(1), "message", "Checked in!");
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Map.of();
    }

    @Override
    public List<Map<String, Object>> getCheckins(int cafeId) {
        String sql = "SELECT ci.*, u.username, u.avatar_url FROM cafe_checkins ci " +
            "JOIN borgol_users u ON u.id=ci.user_id WHERE ci.cafe_id=? ORDER BY ci.created_at DESC LIMIT 20";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, cafeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("username", nullToEmpty(rs.getString("username")));
                    m.put("avatarUrl", nullToEmpty(rs.getString("avatar_url")));
                    m.put("note", nullToEmpty(rs.getString("note")));
                    Timestamp ts = rs.getTimestamp("created_at");
                    m.put("createdAt", ts != null ? ts.toLocalDateTime().toString() : "");
                    list.add(m);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private CafeListing mapCafe(ResultSet rs) throws SQLException {
        CafeListing c = new CafeListing();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setAddress(nullToEmpty(rs.getString("address")));
        c.setDistrict(nullToEmpty(rs.getString("district")));
        c.setCity(nullToEmpty(rs.getString("city")));
        c.setPhone(nullToEmpty(rs.getString("phone")));
        c.setDescription(nullToEmpty(rs.getString("description")));
        c.setHours(nullToEmpty(rs.getString("hours")));
        c.setAvgRating(rs.getDouble("avg_rating"));
        c.setRatingCount(rs.getInt("rating_count"));
        c.setSubmittedBy(rs.getInt("submitted_by"));
        c.setSubmittedByUsername(nullToEmpty(rs.getString("submitted_by_username")));
        c.setImageUrl(nullToEmpty(rs.getString("image_url")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime().toString());
        double lat = rs.getDouble("lat"); if (!rs.wasNull()) c.setLat(lat);
        double lng = rs.getDouble("lng"); if (!rs.wasNull()) c.setLng(lng);
        int userRating = rs.getInt("user_rating");
        if (!rs.wasNull()) {
            c.setCurrentUserRating(userRating);
            c.setCurrentUserReview(nullToEmpty(rs.getString("user_review")));
        }
        return c;
    }

    private int countNoParam(String sql) {
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static String nvl(String s)           { return s != null ? s : ""; }
    private static String nvl(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }
    private static String nullToEmpty(String s)    { return s != null ? s : ""; }
}
