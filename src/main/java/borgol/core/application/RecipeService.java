package borgol.core.application;

import borgol.core.domain.Equipment;
import borgol.core.domain.Recipe;
import borgol.core.domain.RecipeComment;
import borgol.core.ports.RecipeRepositoryPort;
import borgol.core.ports.UserRepositoryPort;
import borgol.infrastructure.cache.CacheKeyBuilder;
import borgol.infrastructure.cache.RedisClient;
import borgol.infrastructure.messaging.RedisEventBus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    private static final String TRENDING_KEY = "borgol:trending";

    private final RecipeRepositoryPort recipeRepo;
    private final UserRepositoryPort   userRepo;
    private final AchievementService   achievements;
    private final RedisEventBus        eventBus;
    private final Gson                 gson = new Gson();

    public RecipeService(RecipeRepositoryPort recipeRepo, UserRepositoryPort userRepo,
                         AchievementService achievements, RedisEventBus eventBus) {
        this.recipeRepo   = recipeRepo;
        this.userRepo     = userRepo;
        this.achievements = achievements;
        this.eventBus     = eventBus;
    }

    // ── Cache helpers ─────────────────────────────────────────────────────────

    private <T> T cacheGet(String key, Type type) {
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

    private void cacheEvict(String key) {
        try {
            RedisClient.get().del(key);
            log.debug("[Cache EVICT] {}", key);
        } catch (Exception e) {
            log.debug("[Cache] Redis устгахад алдаа: {} — {}", key, e.getMessage());
        }
    }

    private void adjustTagScores(List<String> tags, double delta) {
        try (Jedis jedis = RedisClient.get().pool().getResource()) {
            for (String tag : tags) {
                if (tag != null && !tag.isBlank()) {
                    double newScore = jedis.zincrby(TRENDING_KEY, delta, tag.toLowerCase());
                    if (newScore <= 0) jedis.zrem(TRENDING_KEY, tag.toLowerCase());
                }
            }
        } catch (Exception e) {
            log.debug("[Trending] adjustTagScores алдаа: {}", e.getMessage());
        }
    }

    // ── Recipes ───────────────────────────────────────────────────────────────

    public List<Recipe> getRecipes(int currentUserId, String search, String drinkType, String sort) {
        return recipeRepo.findAllRecipes(currentUserId, search, drinkType, sort);
    }

    public List<Recipe> getFeed(int userId) {
        String key = CacheKeyBuilder.forFeed(userId);
        Type listType = new TypeToken<List<Recipe>>(){}.getType();
        List<Recipe> cached = cacheGet(key, listType);
        if (cached != null) return cached;

        List<Recipe> feed = recipeRepo.getFeedRecipes(userId, 30);
        cachePut(key, feed, 60);
        return feed;
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        return recipeRepo.getUserRecipes(authorId, currentUserId);
    }

    public Recipe getRecipe(int id, int currentUserId) {
        String key = CacheKeyBuilder.forRecipe(id);
        Recipe cached = cacheGet(key, Recipe.class);
        if (cached != null) return cached;

        Recipe recipe = recipeRepo.findRecipeById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: id=" + id));
        cachePut(key, recipe, 300);
        return recipe;
    }

    public Recipe createRecipe(int authorId, String title, String description, String drinkType,
                                String ingredients, String instructions, int brewTime,
                                String difficulty, List<String> flavorTags, String imageUrl) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (drinkType == null || drinkType.isBlank()) drinkType = "COFFEE";

        Recipe r = new Recipe();
        r.setAuthorId(authorId);
        r.setTitle(title.trim());
        r.setDescription(description);
        r.setDrinkType(drinkType.toUpperCase());
        r.setIngredients(ingredients);
        r.setInstructions(instructions);
        r.setBrewTime(Math.max(0, brewTime));
        r.setDifficulty(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
        r.setFlavorTags(flavorTags != null ? flavorTags : List.of());
        r.setImageUrl(imageUrl != null ? imageUrl : "");
        Recipe created = recipeRepo.createRecipe(r);
        if (flavorTags != null) adjustTagScores(flavorTags, 1.0);
        achievements.checkAndAwardAchievements(authorId);
        return created;
    }

    public Recipe updateRecipe(int recipeId, int authorId, String title, String description,
                                String drinkType, String ingredients, String instructions,
                                int brewTime, String difficulty, List<String> flavorTags, String imageUrl) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");

        Recipe r = new Recipe();
        r.setId(recipeId);
        r.setAuthorId(authorId);
        r.setTitle(title.trim());
        r.setDescription(description);
        r.setDrinkType(drinkType != null ? drinkType.toUpperCase() : "COFFEE");
        r.setIngredients(ingredients);
        r.setInstructions(instructions);
        r.setBrewTime(Math.max(0, brewTime));
        r.setDifficulty(difficulty != null ? difficulty.toUpperCase() : "MEDIUM");
        r.setFlavorTags(flavorTags != null ? flavorTags : List.of());
        r.setImageUrl(imageUrl != null ? imageUrl : "");
        Recipe updated = recipeRepo.updateRecipe(r);
        cacheEvict(CacheKeyBuilder.forRecipe(recipeId));
        return updated;
    }

    public void deleteRecipe(int recipeId, int userId) {
        List<String> tags = recipeRepo.findRecipeById(recipeId, userId)
            .map(Recipe::getFlavorTags).orElse(List.of());
        boolean deleted = recipeRepo.deleteRecipe(recipeId, userId);
        if (!deleted) throw new IllegalArgumentException("Recipe not found or not authorized");
        cacheEvict(CacheKeyBuilder.forRecipe(recipeId));
        if (!tags.isEmpty()) adjustTagScores(tags, -1.0);
    }

    public Map<String, Object> toggleLike(int userId, int recipeId) {
        boolean liked;
        if (recipeRepo.findRecipeById(recipeId, userId)
                .map(Recipe::isLikedByCurrentUser).orElse(false)) {
            recipeRepo.unlikeRecipe(userId, recipeId);
            liked = false;
        } else {
            recipeRepo.likeRecipe(userId, recipeId);
            liked = true;
            recipeRepo.findRecipeById(recipeId, userId).ifPresent(r -> {
                if (r.getAuthorId() != userId) {
                    userRepo.createNotification(r.getAuthorId(), "like", userId, recipeId,
                        "liked your recipe \"" + r.getTitle() + "\"");
                    eventBus.publish(r.getAuthorId(), "like",
                        "Someone liked your recipe \"" + r.getTitle() + "\"");
                }
            });
        }
        int count = recipeRepo.findRecipeById(recipeId, userId)
            .map(Recipe::getLikesCount).orElse(0);
        return Map.of("liked", liked, "likesCount", count);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public List<RecipeComment> getComments(int recipeId) {
        return recipeRepo.findCommentsByRecipeId(recipeId);
    }

    public RecipeComment addComment(int recipeId, int authorId, String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Comment cannot be empty");
        if (content.length() > 1000) throw new IllegalArgumentException("Comment too long (max 1000 chars)");
        RecipeComment comment = recipeRepo.addComment(recipeId, authorId, content);
        recipeRepo.findRecipeById(recipeId, authorId).ifPresent(r -> {
            if (r.getAuthorId() != authorId) {
                userRepo.createNotification(r.getAuthorId(), "comment", authorId, recipeId,
                    "commented on your recipe \"" + r.getTitle() + "\"");
                eventBus.publish(r.getAuthorId(), "comment",
                    "Someone commented on your recipe \"" + r.getTitle() + "\"");
            }
        });
        return comment;
    }

    // ── Saved recipes ─────────────────────────────────────────────────────────

    public Map<String, Object> toggleSave(int userId, int recipeId) {
        boolean saved;
        if (recipeRepo.isRecipeSaved(userId, recipeId)) {
            recipeRepo.unsaveRecipe(userId, recipeId);
            saved = false;
        } else {
            recipeRepo.saveRecipe(userId, recipeId);
            saved = true;
        }
        return Map.of("saved", saved);
    }

    public List<Recipe> getSavedRecipes(int userId, int currentUserId) {
        return recipeRepo.getSavedRecipes(userId, currentUserId);
    }

    public List<Recipe> getLikedRecipes(int userId, int currentUserId) {
        return recipeRepo.getLikedRecipes(userId, currentUserId);
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    public List<Recipe> getHashtagRecipes(int currentUserId, String tag) {
        return recipeRepo.getRecipesByHashtag(currentUserId, tag);
    }

    public List<Map<String, Object>> getTrendingHashtags() {
        try (Jedis jedis = RedisClient.get().pool().getResource()) {
            List<String> tags = jedis.zrevrange(TRENDING_KEY, 0, 19);
            if (!tags.isEmpty()) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (String tag : tags) {
                    Double score = jedis.zscore(TRENDING_KEY, tag);
                    result.add(Map.of("tag", tag, "recipeCount", score != null ? score.longValue() : 0L));
                }
                log.debug("[Cache HIT] {}", TRENDING_KEY);
                return result;
            }
        } catch (Exception e) {
            log.debug("[Trending] Redis Sorted Set алдаа — DB fallback: {}", e.getMessage());
        }
        return recipeRepo.getTrendingHashtags(20);
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public void submitReport(int reporterId, String contentType, int contentId,
                              String reason, String description) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Reason is required");
        List<String> validTypes = List.of("recipe", "user", "comment");
        if (!validTypes.contains(contentType)) throw new IllegalArgumentException("Invalid content type");
        recipeRepo.createReport(reporterId, contentType, contentId, reason, description);
    }

    public List<Map<String, Object>> getReports(String status) {
        return recipeRepo.getAllReports(status);
    }

    public void resolveReport(int reportId, int adminId, String action) {
        List<String> valid = List.of("resolved", "dismissed");
        if (!valid.contains(action)) throw new IllegalArgumentException("Action must be 'resolved' or 'dismissed'");
        recipeRepo.resolveReport(reportId, adminId, action);
    }

    public Map<String, Object> getAdminStats() {
        return Map.of("pendingReports", recipeRepo.getPendingReportCount());
    }

    // ── Collections ───────────────────────────────────────────────────────────

    public List<Map<String, Object>> getCollections(int userId) {
        return recipeRepo.getCollections(userId);
    }

    public Map<String, Object> createCollection(int userId, String name, String description, boolean isPublic) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Collection name is required");
        return recipeRepo.createCollection(userId, name.trim(), description, isPublic);
    }

    public void deleteCollection(int id, int userId) {
        recipeRepo.deleteCollection(id, userId);
    }

    public void addRecipeToCollection(int collectionId, int recipeId, int userId) {
        recipeRepo.addRecipeToCollection(collectionId, recipeId, userId);
    }

    public void removeRecipeFromCollection(int collectionId, int recipeId, int userId) {
        recipeRepo.removeRecipeFromCollection(collectionId, recipeId, userId);
    }

    public List<Map<String, Object>> getCollectionRecipes(int collectionId) {
        return recipeRepo.getCollectionRecipes(collectionId);
    }

    // ── Equipment ─────────────────────────────────────────────────────────────

    public List<Equipment> getEquipment(int userId) {
        return recipeRepo.getEquipmentByUser(userId);
    }

    public Equipment addEquipment(int userId, String category, String name, String brand, String notes) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        return recipeRepo.addEquipment(userId, category, name.trim(), brand, notes);
    }

    public void deleteEquipment(int equipmentId, int userId) {
        recipeRepo.deleteEquipment(equipmentId, userId);
    }
}
