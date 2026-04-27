package borgol.infrastructure.persistence;

import borgol.core.domain.BrewGuide;
import borgol.core.domain.LearnArticle;
import borgol.core.ports.BrewGuideRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Хөрөнгийн удирдлага: дархалгааны заавар, суралцах нийтлэл, seed.
 * Загвар: Repository — BrewGuideRepositoryPort хэрэгжүүлнэ.
 */
public class BrewGuideRepository implements BrewGuideRepositoryPort {

    private final DatabaseConnection db;

    public BrewGuideRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── Brew Guides operations ────────────────────────────────────────────────

    @Override
    public List<BrewGuide> findAllBrewGuides() {
        String sql = "SELECT * FROM brew_guides ORDER BY id ASC";
        List<BrewGuide> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBrewGuide(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<BrewGuide> findBrewGuideById(int id) {
        String sql = "SELECT * FROM brew_guides WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapBrewGuide(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    // ── Learn Articles operations ─────────────────────────────────────────────

    @Override
    public List<LearnArticle> findAllLearnArticles() {
        String sql = "SELECT * FROM learn_articles ORDER BY id ASC";
        List<LearnArticle> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLearnArticle(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<LearnArticle> findLearnArticleById(int id) {
        String sql = "SELECT * FROM learn_articles WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapLearnArticle(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    // ── Static content seeding ────────────────────────────────────────────────

    @Override
    public boolean isStaticContentSeeded() {
        return countNoParam("SELECT COUNT(*) FROM brew_guides") > 0;
    }

    @Override
    public void seedBrewGuide(BrewGuide g) {
        String sql = """
            INSERT INTO brew_guides (method_name, description, difficulty, brew_time_min, parameters, steps, icon)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, g.getMethodName());
            ps.setString(2, nvl(g.getDescription()));
            ps.setString(3, nvl(g.getDifficulty(), "MEDIUM"));
            ps.setInt(4, g.getBrewTimeMin());
            ps.setString(5, nvl(g.getParameters()));
            ps.setString(6, nvl(g.getSteps()));
            ps.setString(7, nvl(g.getIcon(), "☕"));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void seedLearnArticle(LearnArticle a) {
        String sql = """
            INSERT INTO learn_articles (title, category, content, icon, read_time_min)
            VALUES (?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, nvl(a.getCategory()));
            ps.setString(3, nvl(a.getContent()));
            ps.setString(4, nvl(a.getIcon(), "📖"));
            ps.setInt(5, a.getReadTimeMin());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean isBeanArticlesSeeded() {
        return countNoParam("SELECT COUNT(*) FROM learn_articles WHERE category = 'Beans'") > 0;
    }

    @Override
    public boolean isDrinkArticlesSeeded() {
        return countNoParam("SELECT COUNT(*) FROM learn_articles WHERE category = 'Drinks'") > 0;
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private BrewGuide mapBrewGuide(ResultSet rs) throws SQLException {
        BrewGuide g = new BrewGuide();
        g.setId(rs.getInt("id"));
        g.setMethodName(rs.getString("method_name"));
        g.setDescription(nullToEmpty(rs.getString("description")));
        g.setDifficulty(nullToEmpty(rs.getString("difficulty")));
        g.setBrewTimeMin(rs.getInt("brew_time_min"));
        g.setParameters(nullToEmpty(rs.getString("parameters")));
        g.setSteps(nullToEmpty(rs.getString("steps")));
        g.setIcon(nullToEmpty(rs.getString("icon")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) g.setCreatedAt(ts.toLocalDateTime().toString());
        return g;
    }

    private LearnArticle mapLearnArticle(ResultSet rs) throws SQLException {
        LearnArticle a = new LearnArticle();
        a.setId(rs.getInt("id"));
        a.setTitle(rs.getString("title"));
        a.setCategory(nullToEmpty(rs.getString("category")));
        a.setContent(nullToEmpty(rs.getString("content")));
        a.setIcon(nullToEmpty(rs.getString("icon")));
        a.setReadTimeMin(rs.getInt("read_time_min"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) a.setCreatedAt(ts.toLocalDateTime().toString());
        return a;
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private int countNoParam(String sql) {
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static String nvl(String s)           { return s != null ? s : ""; }
    private static String nvl(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }
    private static String nullToEmpty(String s)    { return s != null ? s : ""; }
}
