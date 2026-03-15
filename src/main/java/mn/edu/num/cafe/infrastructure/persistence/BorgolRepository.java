package mn.edu.num.cafe.infrastructure.persistence;

import mn.edu.num.cafe.core.domain.*;
import mn.edu.num.cafe.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Single repository for all Borgol platform data access.
 * Handles: Users, Recipes, Cafes, Social (follows, likes, comments).
 *
 * Also initializes all required database tables on construction.
 */
public class BorgolRepository {

    private final DatabaseConnection db;

    public BorgolRepository(DatabaseConnection db) {
        this.db = db;
        initSchema();
    }

    // ── Schema initialization ─────────────────────────────────────────────────

    private void initSchema() {
        try (Statement s = conn().createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS borgol_users (
                    id              INT PRIMARY KEY AUTO_INCREMENT,
                    username        VARCHAR(50)  UNIQUE NOT NULL,
                    email           VARCHAR(100) UNIQUE NOT NULL,
                    password_hash   VARCHAR(255) NOT NULL,
                    bio             VARCHAR(500) DEFAULT '',
                    avatar_url      VARCHAR(255) DEFAULT '',
                    expertise_level VARCHAR(20)  DEFAULT 'BEGINNER',
                    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS user_flavor_prefs (
                    user_id INT,
                    flavor  VARCHAR(30),
                    PRIMARY KEY (user_id, flavor),
                    FOREIGN KEY (user_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS user_follows (
                    follower_id  INT,
                    following_id INT,
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (follower_id, following_id),
                    FOREIGN KEY (follower_id)  REFERENCES borgol_users(id) ON DELETE CASCADE,
                    FOREIGN KEY (following_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS recipes (
                    id            INT PRIMARY KEY AUTO_INCREMENT,
                    author_id     INT          NOT NULL,
                    title         VARCHAR(100) NOT NULL,
                    description   VARCHAR(2000) DEFAULT '',
                    drink_type    VARCHAR(30)  DEFAULT 'COFFEE',
                    ingredients   TEXT         DEFAULT '',
                    instructions  TEXT         DEFAULT '',
                    brew_time     INT          DEFAULT 0,
                    difficulty    VARCHAR(20)  DEFAULT 'MEDIUM',
                    image_url     VARCHAR(255) DEFAULT '',
                    likes_count   INT          DEFAULT 0,
                    comment_count INT          DEFAULT 0,
                    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (author_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS recipe_flavor_tags (
                    recipe_id INT,
                    flavor    VARCHAR(30),
                    PRIMARY KEY (recipe_id, flavor),
                    FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS recipe_likes (
                    user_id    INT,
                    recipe_id  INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, recipe_id),
                    FOREIGN KEY (user_id)   REFERENCES borgol_users(id) ON DELETE CASCADE,
                    FOREIGN KEY (recipe_id) REFERENCES recipes(id)      ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS recipe_comments (
                    id         INT PRIMARY KEY AUTO_INCREMENT,
                    recipe_id  INT  NOT NULL,
                    author_id  INT  NOT NULL,
                    content    TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (recipe_id) REFERENCES recipes(id)      ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS cafes (
                    id             INT PRIMARY KEY AUTO_INCREMENT,
                    name           VARCHAR(100)  NOT NULL,
                    address        VARCHAR(255)  DEFAULT '',
                    district       VARCHAR(100)  DEFAULT '',
                    city           VARCHAR(100)  DEFAULT 'Ulaanbaatar',
                    phone          VARCHAR(50)   DEFAULT '',
                    description    VARCHAR(2000) DEFAULT '',
                    hours          VARCHAR(255)  DEFAULT '',
                    avg_rating     DOUBLE        DEFAULT 0,
                    rating_count   INT           DEFAULT 0,
                    submitted_by   INT,
                    image_url      VARCHAR(255)  DEFAULT '',
                    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (submitted_by) REFERENCES borgol_users(id) ON DELETE SET NULL
                )""");
            // Migration: add GPS columns if they don't exist yet (safe on existing DBs)
            s.execute("ALTER TABLE cafes ADD COLUMN IF NOT EXISTS lat DOUBLE DEFAULT NULL");
            s.execute("ALTER TABLE cafes ADD COLUMN IF NOT EXISTS lng DOUBLE DEFAULT NULL");
            s.execute("""
                CREATE TABLE IF NOT EXISTS cafe_ratings (
                    user_id    INT,
                    cafe_id    INT,
                    rating     INT  NOT NULL,
                    review     VARCHAR(500) DEFAULT '',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id, cafe_id),
                    FOREIGN KEY (user_id)  REFERENCES borgol_users(id) ON DELETE CASCADE,
                    FOREIGN KEY (cafe_id)  REFERENCES cafes(id)        ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS brew_journal (
                    id               INT PRIMARY KEY AUTO_INCREMENT,
                    user_id          INT    NOT NULL,
                    coffee_bean      VARCHAR(100) DEFAULT '',
                    origin           VARCHAR(100) DEFAULT '',
                    roast_level      VARCHAR(30)  DEFAULT '',
                    brew_method      VARCHAR(50)  DEFAULT '',
                    grind_size       VARCHAR(30)  DEFAULT '',
                    water_temp_c     INT          DEFAULT 0,
                    dose_grams       DOUBLE       DEFAULT 0,
                    yield_grams      DOUBLE       DEFAULT 0,
                    brew_time_sec    INT          DEFAULT 0,
                    rating_aroma     INT          DEFAULT 5,
                    rating_flavor    INT          DEFAULT 5,
                    rating_acidity   INT          DEFAULT 5,
                    rating_body      INT          DEFAULT 5,
                    rating_sweetness INT          DEFAULT 5,
                    rating_finish    INT          DEFAULT 5,
                    notes            TEXT         DEFAULT '',
                    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS brew_guides (
                    id            INT PRIMARY KEY AUTO_INCREMENT,
                    method_name   VARCHAR(50)  NOT NULL,
                    description   VARCHAR(500) DEFAULT '',
                    difficulty    VARCHAR(20)  DEFAULT 'MEDIUM',
                    brew_time_min INT          DEFAULT 5,
                    parameters    TEXT         DEFAULT '',
                    steps         TEXT         DEFAULT '',
                    icon          VARCHAR(10)  DEFAULT '☕',
                    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS learn_articles (
                    id            INT PRIMARY KEY AUTO_INCREMENT,
                    title         VARCHAR(100) NOT NULL,
                    category      VARCHAR(50)  DEFAULT '',
                    content       TEXT         DEFAULT '',
                    icon          VARCHAR(10)  DEFAULT '📖',
                    read_time_min INT          DEFAULT 3,
                    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
                )""");
            s.execute("""
                CREATE TABLE IF NOT EXISTS user_equipment (
                    id         INT PRIMARY KEY AUTO_INCREMENT,
                    user_id    INT          NOT NULL,
                    category   VARCHAR(30)  DEFAULT 'OTHER',
                    name       VARCHAR(100) NOT NULL,
                    brand      VARCHAR(100) DEFAULT '',
                    notes      TEXT         DEFAULT '',
                    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES borgol_users(id) ON DELETE CASCADE
                )""");
        } catch (SQLException e) {
            throw new RuntimeException("Schema initialization failed", e);
        }
    }

    // ── User operations ───────────────────────────────────────────────────────

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

    public void updateUser(int id, String bio, String avatarUrl, String expertiseLevel) {
        // Only update avatar_url when a non-blank value is provided, to avoid clearing it
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

    public int getFollowerCount(int userId) {
        return count("SELECT COUNT(*) FROM user_follows WHERE following_id = ?", userId);
    }

    public int getFollowingCount(int userId) {
        return count("SELECT COUNT(*) FROM user_follows WHERE follower_id = ?", userId);
    }

    public int getUserRecipeCount(int userId) {
        return count("SELECT COUNT(*) FROM recipes WHERE author_id = ?", userId);
    }

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

    public void followUser(int followerId, int followingId) {
        String sql = "MERGE INTO user_follows (follower_id, following_id) KEY(follower_id, following_id) VALUES (?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void unfollowUser(int followerId, int followingId) {
        String sql = "DELETE FROM user_follows WHERE follower_id=? AND following_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, followerId);
            ps.setInt(2, followingId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

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

    // ── Recipe operations ─────────────────────────────────────────────────────

    public List<Recipe> findAllRecipes(int currentUserId, String search, String drinkType, String sort) {
        StringBuilder sql = new StringBuilder("""
            SELECT r.*, u.username AS author_username
            FROM recipes r
            JOIN borgol_users u ON r.author_id = u.id
            WHERE 1=1
            """);
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(r.title) LIKE ? OR LOWER(r.description) LIKE ?)");
            params.add("%" + search.toLowerCase() + "%");
            params.add("%" + search.toLowerCase() + "%");
        }
        if (drinkType != null && !drinkType.isBlank() && !drinkType.equals("ALL")) {
            sql.append(" AND r.drink_type = ?");
            params.add(drinkType);
        }
        sql.append("TRENDING".equals(sort)
            ? " ORDER BY r.likes_count DESC, r.created_at DESC"
            : " ORDER BY r.created_at DESC");
        sql.append(" LIMIT 50");

        List<Recipe> recipes = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) recipes.add(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return recipes;
    }

    public List<Recipe> getFeedRecipes(int userId, int limit) {
        // Recipes from followed users + own recipes, ordered by date
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM recipes r
            JOIN borgol_users u ON r.author_id = u.id
            WHERE r.author_id = ?
               OR r.author_id IN (SELECT following_id FROM user_follows WHERE follower_id = ?)
            ORDER BY r.created_at DESC
            LIMIT ?
            """;
        List<Recipe> recipes = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) recipes.add(mapRecipe(rs, userId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return recipes;
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM recipes r
            JOIN borgol_users u ON r.author_id = u.id
            WHERE r.author_id = ?
            ORDER BY r.created_at DESC
            """;
        List<Recipe> recipes = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, authorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) recipes.add(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return recipes;
    }

    public Optional<Recipe> findRecipeById(int id, int currentUserId) {
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM recipes r
            JOIN borgol_users u ON r.author_id = u.id
            WHERE r.id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public Recipe createRecipe(Recipe r) {
        String sql = """
            INSERT INTO recipes
              (author_id, title, description, drink_type, ingredients, instructions,
               brew_time, difficulty, image_url)
            VALUES (?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getAuthorId());
            ps.setString(2, r.getTitle());
            ps.setString(3, nvl(r.getDescription()));
            ps.setString(4, nvl(r.getDrinkType(), "COFFEE"));
            ps.setString(5, nvl(r.getIngredients()));
            ps.setString(6, nvl(r.getInstructions()));
            ps.setInt(7, r.getBrewTime());
            ps.setString(8, nvl(r.getDifficulty(), "MEDIUM"));
            ps.setString(9, nvl(r.getImageUrl()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    saveFlavorTags(newId, r.getFlavorTags());
                    return findRecipeById(newId, r.getAuthorId()).orElseThrow();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Recipe creation failed");
    }

    public Recipe updateRecipe(Recipe r) {
        String sql = """
            UPDATE recipes SET title=?, description=?, drink_type=?,
              ingredients=?, instructions=?, brew_time=?, difficulty=?, image_url=?
            WHERE id=? AND author_id=?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, r.getTitle());
            ps.setString(2, nvl(r.getDescription()));
            ps.setString(3, nvl(r.getDrinkType(), "COFFEE"));
            ps.setString(4, nvl(r.getIngredients()));
            ps.setString(5, nvl(r.getInstructions()));
            ps.setInt(6, r.getBrewTime());
            ps.setString(7, nvl(r.getDifficulty(), "MEDIUM"));
            ps.setString(8, nvl(r.getImageUrl()));
            ps.setInt(9, r.getId());
            ps.setInt(10, r.getAuthorId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Recipe not found or not owner");
            saveFlavorTags(r.getId(), r.getFlavorTags());
        } catch (SQLException e) { throw new RuntimeException(e); }
        return findRecipeById(r.getId(), r.getAuthorId()).orElseThrow();
    }

    public boolean deleteRecipe(int id, int userId) {
        String sql = "DELETE FROM recipes WHERE id=? AND author_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean likeRecipe(int userId, int recipeId) {
        String check = "SELECT COUNT(*) FROM recipe_likes WHERE user_id=? AND recipe_id=?";
        try (PreparedStatement ps = conn().prepareStatement(check)) {
            ps.setInt(1, userId);
            ps.setInt(2, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return false; // already liked
            }
        } catch (SQLException e) { throw new RuntimeException(e); }

        String ins = "INSERT INTO recipe_likes (user_id, recipe_id) VALUES (?,?)";
        String upd = "UPDATE recipes SET likes_count = likes_count + 1 WHERE id=?";
        try (PreparedStatement ps1 = conn().prepareStatement(ins);
             PreparedStatement ps2 = conn().prepareStatement(upd)) {
            ps1.setInt(1, userId); ps1.setInt(2, recipeId); ps1.executeUpdate();
            ps2.setInt(1, recipeId); ps2.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return true;
    }

    public boolean unlikeRecipe(int userId, int recipeId) {
        String del = "DELETE FROM recipe_likes WHERE user_id=? AND recipe_id=?";
        String upd = "UPDATE recipes SET likes_count = GREATEST(0, likes_count - 1) WHERE id=?";
        try (PreparedStatement ps1 = conn().prepareStatement(del);
             PreparedStatement ps2 = conn().prepareStatement(upd)) {
            ps1.setInt(1, userId); ps1.setInt(2, recipeId);
            int deleted = ps1.executeUpdate();
            if (deleted > 0) { ps2.setInt(1, recipeId); ps2.executeUpdate(); }
            return deleted > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Comment operations ────────────────────────────────────────────────────

    public List<RecipeComment> findCommentsByRecipeId(int recipeId) {
        String sql = """
            SELECT c.*, u.username AS author_username
            FROM recipe_comments c
            JOIN borgol_users u ON c.author_id = u.id
            WHERE c.recipe_id = ?
            ORDER BY c.created_at ASC
            """;
        List<RecipeComment> comments = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) comments.add(mapComment(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return comments;
    }

    public RecipeComment addComment(int recipeId, int authorId, String content) {
        String ins = "INSERT INTO recipe_comments (recipe_id, author_id, content) VALUES (?,?,?)";
        String upd = "UPDATE recipes SET comment_count = comment_count + 1 WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement ps2 = conn().prepareStatement(upd)) {
            ps.setInt(1, recipeId);
            ps.setInt(2, authorId);
            ps.setString(3, content);
            ps.executeUpdate();
            ps2.setInt(1, recipeId); ps2.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int commentId = keys.getInt(1);
                    return findCommentById(commentId);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Comment creation failed");
    }

    private RecipeComment findCommentById(int id) {
        String sql = """
            SELECT c.*, u.username AS author_username
            FROM recipe_comments c
            JOIN borgol_users u ON c.author_id = u.id
            WHERE c.id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapComment(rs);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Comment not found: " + id);
    }

    // ── Cafe operations ───────────────────────────────────────────────────────

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

    public void updateCafeCoordinates(int cafeId, double lat, double lng) {
        String sql = "UPDATE cafes SET lat = ?, lng = ? WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lng);
            ps.setInt(3, cafeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Returns cafes within a bounding box (lat/lng range). Cafes with no coordinates are excluded. */
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

    public boolean rateCafe(int userId, int cafeId, int rating, String review) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1-5");

        String merge = """
            MERGE INTO cafe_ratings (user_id, cafe_id, rating, review)
            KEY(user_id, cafe_id) VALUES (?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(merge)) {
            ps.setInt(1, userId);
            ps.setInt(2, cafeId);
            ps.setInt(3, rating);
            ps.setString(4, nvl(review));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }

        // Recalculate avg rating
        String recalc = """
            UPDATE cafes SET
                avg_rating   = (SELECT AVG(CAST(rating AS DOUBLE)) FROM cafe_ratings WHERE cafe_id = ?),
                rating_count = (SELECT COUNT(*) FROM cafe_ratings WHERE cafe_id = ?)
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(recalc)) {
            ps.setInt(1, cafeId); ps.setInt(2, cafeId); ps.setInt(3, cafeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        return true;
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

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

    private Recipe mapRecipe(ResultSet rs, int currentUserId) throws SQLException {
        Recipe r = new Recipe();
        r.setId(rs.getInt("id"));
        r.setAuthorId(rs.getInt("author_id"));
        r.setAuthorUsername(rs.getString("author_username"));
        r.setTitle(rs.getString("title"));
        r.setDescription(nullToEmpty(rs.getString("description")));
        r.setDrinkType(nullToEmpty(rs.getString("drink_type")));
        r.setIngredients(nullToEmpty(rs.getString("ingredients")));
        r.setInstructions(nullToEmpty(rs.getString("instructions")));
        r.setBrewTime(rs.getInt("brew_time"));
        r.setDifficulty(nullToEmpty(rs.getString("difficulty")));
        r.setImageUrl(nullToEmpty(rs.getString("image_url")));
        r.setLikesCount(rs.getInt("likes_count"));
        r.setCommentCount(rs.getInt("comment_count"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime().toString());

        // Load flavor tags
        r.setFlavorTags(getRecipeFlavorTags(r.getId()));

        // Check if liked by current user
        if (currentUserId > 0) {
            r.setLikedByCurrentUser(isRecipeLikedBy(currentUserId, r.getId()));
        }
        return r;
    }

    private List<String> getRecipeFlavorTags(int recipeId) {
        String sql = "SELECT flavor FROM recipe_flavor_tags WHERE recipe_id = ?";
        List<String> tags = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) tags.add(rs.getString("flavor"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return tags;
    }

    private boolean isRecipeLikedBy(int userId, int recipeId) {
        String sql = "SELECT COUNT(*) FROM recipe_likes WHERE user_id=? AND recipe_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, recipeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void saveFlavorTags(int recipeId, List<String> tags) throws SQLException {
        try (PreparedStatement del = conn().prepareStatement(
                "DELETE FROM recipe_flavor_tags WHERE recipe_id=?")) {
            del.setInt(1, recipeId);
            del.executeUpdate();
        }
        if (tags != null && !tags.isEmpty()) {
            try (PreparedStatement ins = conn().prepareStatement(
                    "INSERT INTO recipe_flavor_tags (recipe_id, flavor) VALUES (?,?)")) {
                for (String t : tags) {
                    ins.setInt(1, recipeId);
                    ins.setString(2, t);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        }
    }

    private RecipeComment mapComment(ResultSet rs) throws SQLException {
        RecipeComment c = new RecipeComment();
        c.setId(rs.getInt("id"));
        c.setRecipeId(rs.getInt("recipe_id"));
        c.setAuthorId(rs.getInt("author_id"));
        c.setAuthorUsername(rs.getString("author_username"));
        c.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) c.setCreatedAt(ts.toLocalDateTime().toString());
        return c;
    }

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
        // GPS coordinates (nullable)
        double lat = rs.getDouble("lat"); if (!rs.wasNull()) c.setLat(lat);
        double lng = rs.getDouble("lng"); if (!rs.wasNull()) c.setLng(lng);
        // User's rating
        int userRating = rs.getInt("user_rating");
        if (!rs.wasNull()) {
            c.setCurrentUserRating(userRating);
            c.setCurrentUserReview(nullToEmpty(rs.getString("user_review")));
        }
        return c;
    }

    // ── Extra user/recipe queries ─────────────────────────────────────────────

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

    public List<Recipe> getLikedRecipes(int userId, int currentUserId) {
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM recipe_likes rl
            JOIN recipes r ON rl.recipe_id = r.id
            JOIN borgol_users u ON r.author_id = u.id
            WHERE rl.user_id = ?
            ORDER BY rl.created_at DESC
            """;
        List<Recipe> recipes = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) recipes.add(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return recipes;
    }

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

    // ── Brew Journal operations ───────────────────────────────────────────────

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

    public BrewJournalEntry createJournalEntry(BrewJournalEntry e) {
        String sql = """
            INSERT INTO brew_journal
              (user_id, coffee_bean, origin, roast_level, brew_method, grind_size,
               water_temp_c, dose_grams, yield_grams, brew_time_sec,
               rating_aroma, rating_flavor, rating_acidity, rating_body,
               rating_sweetness, rating_finish, notes)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findJournalEntry(keys.getInt(1), e.getUserId()).orElseThrow();
                }
            }
        } catch (SQLException ex) { throw new RuntimeException(ex); }
        throw new RuntimeException("Journal entry creation failed");
    }

    public BrewJournalEntry updateJournalEntry(BrewJournalEntry e) {
        String sql = """
            UPDATE brew_journal SET
              coffee_bean=?, origin=?, roast_level=?, brew_method=?, grind_size=?,
              water_temp_c=?, dose_grams=?, yield_grams=?, brew_time_sec=?,
              rating_aroma=?, rating_flavor=?, rating_acidity=?, rating_body=?,
              rating_sweetness=?, rating_finish=?, notes=?
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
            ps.setInt(17, e.getId());
            ps.setInt(18, e.getUserId());
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalArgumentException("Entry not found or not authorized");
        } catch (SQLException ex) { throw new RuntimeException(ex); }
        return findJournalEntry(e.getId(), e.getUserId()).orElseThrow();
    }

    public boolean deleteJournalEntry(int id, int userId) {
        String sql = "DELETE FROM brew_journal WHERE id=? AND user_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Brew Guides operations ────────────────────────────────────────────────

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

    public boolean isStaticContentSeeded() {
        return countNoParam("SELECT COUNT(*) FROM brew_guides") > 0;
    }

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

    // ── Mapping helpers (new) ─────────────────────────────────────────────────

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
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime().toString());
        return e;
    }

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

    private Connection conn() { return db.getConnection(); }

    private int count(String sql, int param) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private int countNoParam(String sql) {
        try (Statement s = conn().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(10, v)); }
    private static String nvl(String s)           { return s != null ? s : ""; }
    private static String nvl(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }
    private static String nullToEmpty(String s)    { return s != null ? s : ""; }

    // ── Equipment ──────────────────────────────────────────────────────────────

    public List<Equipment> getEquipmentByUser(int userId) {
        String sql = "SELECT * FROM user_equipment WHERE user_id = ? ORDER BY created_at DESC";
        List<Equipment> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEquipment(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public Equipment addEquipment(int userId, String category, String name, String brand, String notes) {
        String sql = "INSERT INTO user_equipment (user_id, category, name, brand, notes) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, nvl(category, "OTHER"));
            ps.setString(3, name);
            ps.setString(4, nvl(brand));
            ps.setString(5, nvl(notes));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return getEquipmentById(id).orElseThrow();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new RuntimeException("Failed to create equipment");
    }

    public Optional<Equipment> getEquipmentById(int id) {
        String sql = "SELECT * FROM user_equipment WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapEquipment(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void deleteEquipment(int id, int userId) {
        String sql = "DELETE FROM user_equipment WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Equipment mapEquipment(ResultSet rs) throws SQLException {
        Equipment eq = new Equipment();
        eq.setId(rs.getInt("id"));
        eq.setUserId(rs.getInt("user_id"));
        eq.setCategory(nullToEmpty(rs.getString("category")));
        eq.setName(nullToEmpty(rs.getString("name")));
        eq.setBrand(nullToEmpty(rs.getString("brand")));
        eq.setNotes(nullToEmpty(rs.getString("notes")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) eq.setCreatedAt(ts.toLocalDateTime().toString());
        return eq;
    }
}
