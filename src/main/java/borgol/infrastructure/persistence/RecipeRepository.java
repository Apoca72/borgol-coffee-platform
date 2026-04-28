package borgol.infrastructure.persistence;

import borgol.core.domain.*;
import borgol.core.ports.RecipeRepositoryPort;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.LinkedHashMap;

/**
 * Жор, коммент, like, хадгалах, collections, тоног төхөөрөмж, мэдээлэл.
 * Загвар: Repository (GoF) — RecipeRepositoryPort хэрэгжүүлнэ.
 */
public class RecipeRepository implements RecipeRepositoryPort {

    private final DatabaseConnection db;

    public RecipeRepository(DatabaseConnection db) {
        this.db = db;
    }

    private Connection conn() { return db.getConnection(); }

    // ── Recipe operations ─────────────────────────────────────────────────────

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public boolean deleteRecipe(int id, int userId) {
        String sql = "DELETE FROM recipes WHERE id=? AND author_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    // ── Saved Recipes ─────────────────────────────────────────────────────────

    @Override
    public void saveRecipe(int userId, int recipeId) {
        String sql = """
            INSERT INTO saved_recipes (user_id, recipe_id)
            VALUES (?, ?)
            ON CONFLICT (user_id, recipe_id) DO NOTHING
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, recipeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void unsaveRecipe(int userId, int recipeId) {
        String sql = "DELETE FROM saved_recipes WHERE user_id = ? AND recipe_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, recipeId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public boolean isRecipeSaved(int userId, int recipeId) {
        return count("SELECT COUNT(*) FROM saved_recipes WHERE user_id = ? AND recipe_id = ?",
                     userId, recipeId) > 0;
    }

    @Override
    public List<Recipe> getSavedRecipes(int userId, int currentUserId) {
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM saved_recipes sr
            JOIN recipes r ON r.id = sr.recipe_id
            JOIN borgol_users u ON u.id = r.author_id
            WHERE sr.user_id = ?
            ORDER BY sr.saved_at DESC
            """;
        List<Recipe> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    @Override
    public List<Recipe> getRecipesByHashtag(int currentUserId, String tag) {
        String sql = """
            SELECT r.*, u.username AS author_username
            FROM recipe_flavor_tags rft
            JOIN recipes r ON r.id = rft.recipe_id
            JOIN borgol_users u ON u.id = r.author_id
            WHERE LOWER(rft.flavor) = LOWER(?)
            ORDER BY r.created_at DESC
            """;
        List<Recipe> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, tag);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRecipe(rs, currentUserId));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTrendingHashtags(int limit) {
        String sql = """
            SELECT flavor AS tag, COUNT(*) AS recipe_count
            FROM recipe_flavor_tags
            GROUP BY flavor
            ORDER BY recipe_count DESC
            LIMIT ?
            """;
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("tag", rs.getString("tag"));
                    row.put("recipeCount", rs.getInt("recipe_count"));
                    result.add(row);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    @Override
    public void createReport(int reporterId, String contentType, int contentId, String reason, String description) {
        String sql = """
            INSERT INTO reports (reporter_id, content_type, content_id, reason, description)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            ps.setString(2, contentType);
            ps.setInt(3, contentId);
            ps.setString(4, reason);
            ps.setString(5, description != null ? description : "");
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Map<String, Object>> getAllReports(String status) {
        String where = (status != null && !status.equals("all")) ? "WHERE r.status = ?" : "";
        String sql = """
            SELECT r.*, u.username AS reporter_username
            FROM reports r
            LEFT JOIN borgol_users u ON u.id = r.reporter_id
            """ + where + " ORDER BY r.created_at DESC";
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (!where.isEmpty()) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id",               rs.getInt("id"));
                    row.put("reporterUsername",  rs.getString("reporter_username"));
                    row.put("contentType",       rs.getString("content_type"));
                    row.put("contentId",         rs.getInt("content_id"));
                    row.put("reason",            rs.getString("reason"));
                    row.put("description",       rs.getString("description"));
                    row.put("status",            rs.getString("status"));
                    row.put("createdAt",         rs.getString("created_at"));
                    result.add(row);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return result;
    }

    @Override
    public void resolveReport(int reportId, int resolvedBy, String status) {
        String sql = """
            UPDATE reports SET status = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, resolvedBy);
            ps.setInt(3, reportId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public int getPendingReportCount() {
        return countNoParam("SELECT COUNT(*) FROM reports WHERE status = 'pending'");
    }

    // ── Recipe Collections ────────────────────────────────────────────────────

    @Override
    public List<Map<String, Object>> getCollections(int userId) {
        String sql = "SELECT c.id, c.user_id, c.name, c.description, c.is_public, c.created_at, " +
            "u.username, (SELECT COUNT(*) FROM collection_recipes cr WHERE cr.collection_id=c.id) AS recipe_count " +
            "FROM recipe_collections c JOIN borgol_users u ON u.id=c.user_id " +
            "WHERE c.user_id=? ORDER BY c.created_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("userId", rs.getInt("user_id"));
                    m.put("username", nullToEmpty(rs.getString("username")));
                    m.put("name", nullToEmpty(rs.getString("name")));
                    m.put("description", nullToEmpty(rs.getString("description")));
                    m.put("isPublic", rs.getBoolean("is_public"));
                    m.put("recipeCount", rs.getInt("recipe_count"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    m.put("createdAt", ts != null ? ts.toLocalDateTime().toString() : "");
                    list.add(m);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Map<String, Object> createCollection(int userId, String name, String description, boolean isPublic) {
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO recipe_collections (user_id, name, description, is_public) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId); ps.setString(2, name);
            ps.setString(3, nvl(description)); ps.setBoolean(4, isPublic);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", keys.getInt(1)); m.put("name", name);
                    return m;
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Map.of();
    }

    @Override
    public void deleteCollection(int id, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM recipe_collections WHERE id=? AND user_id=?")) {
            ps.setInt(1, id); ps.setInt(2, userId); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void addRecipeToCollection(int collectionId, int recipeId, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT id FROM recipe_collections WHERE id=? AND user_id=?")) {
            ps.setInt(1, collectionId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Collection not found");
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        try (PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO collection_recipes (collection_id, recipe_id) VALUES (?,?) ON CONFLICT DO NOTHING")) {
            ps.setInt(1, collectionId); ps.setInt(2, recipeId); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void removeRecipeFromCollection(int collectionId, int recipeId, int userId) {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT id FROM recipe_collections WHERE id=? AND user_id=?")) {
            ps.setInt(1, collectionId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Collection not found");
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM collection_recipes WHERE collection_id=? AND recipe_id=?")) {
            ps.setInt(1, collectionId); ps.setInt(2, recipeId); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Map<String, Object>> getCollectionRecipes(int collectionId) {
        String sql = "SELECT r.id, r.title, r.drink_type, r.difficulty, r.image_url, r.brew_time, " +
            "u.username, (SELECT COUNT(*) FROM recipe_likes l WHERE l.recipe_id=r.id) AS likes_count " +
            "FROM collection_recipes cr JOIN recipes r ON r.id=cr.recipe_id " +
            "JOIN borgol_users u ON u.id=r.author_id WHERE cr.collection_id=? ORDER BY cr.added_at DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, collectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    m.put("title", nullToEmpty(rs.getString("title")));
                    m.put("drinkType", nullToEmpty(rs.getString("drink_type")));
                    m.put("difficulty", nullToEmpty(rs.getString("difficulty")));
                    m.put("imageUrl", nullToEmpty(rs.getString("image_url")));
                    m.put("brewTime", rs.getInt("brew_time"));
                    m.put("username", nullToEmpty(rs.getString("username")));
                    m.put("likesCount", rs.getInt("likes_count"));
                    list.add(m);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    // ── Equipment ─────────────────────────────────────────────────────────────

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void deleteEquipment(int id, int userId) {
        String sql = "DELETE FROM user_equipment WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
            r.setSavedByCurrentUser(isRecipeSaved(currentUserId, r.getId()));
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

    // ── Utilities ─────────────────────────────────────────────────────────────

    private int count(String sql, int p1, int p2) {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, p1);
            ps.setInt(2, p2);
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

    private static String nvl(String s)           { return s != null ? s : ""; }
    private static String nvl(String s, String d) { return (s != null && !s.isBlank()) ? s : d; }
    private static String nullToEmpty(String s)    { return s != null ? s : ""; }
}
