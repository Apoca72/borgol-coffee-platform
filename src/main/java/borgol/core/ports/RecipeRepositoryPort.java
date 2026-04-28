package borgol.core.ports;

import borgol.core.domain.Equipment;
import borgol.core.domain.Recipe;
import borgol.core.domain.RecipeComment;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RecipeRepositoryPort {
    List<Recipe> findAllRecipes(int currentUserId, String search, String drinkType, String sort);
    List<Recipe> getFeedRecipes(int userId, int limit);
    List<Recipe> getUserRecipes(int authorId, int currentUserId);
    Optional<Recipe> findRecipeById(int id, int currentUserId);
    Recipe createRecipe(Recipe r);
    Recipe updateRecipe(Recipe r);
    boolean deleteRecipe(int id, int userId);
    boolean likeRecipe(int userId, int recipeId);
    boolean unlikeRecipe(int userId, int recipeId);
    List<RecipeComment> findCommentsByRecipeId(int recipeId);
    RecipeComment addComment(int recipeId, int authorId, String content);
    List<Recipe> getLikedRecipes(int userId, int currentUserId);
    void saveRecipe(int userId, int recipeId);
    void unsaveRecipe(int userId, int recipeId);
    boolean isRecipeSaved(int userId, int recipeId);
    List<Recipe> getSavedRecipes(int userId, int currentUserId);
    List<Recipe> getRecipesByHashtag(int currentUserId, String tag);
    List<Map<String, Object>> getTrendingHashtags(int limit);
    void createReport(int reporterId, String contentType, int contentId, String reason, String description);
    List<Map<String, Object>> getAllReports(String status);
    void resolveReport(int reportId, int resolvedBy, String status);
    int getPendingReportCount();
    List<Map<String, Object>> getCollections(int userId);
    Map<String, Object> createCollection(int userId, String name, String description, boolean isPublic);
    void deleteCollection(int id, int userId);
    void addRecipeToCollection(int collectionId, int recipeId, int userId);
    void removeRecipeFromCollection(int collectionId, int recipeId, int userId);
    List<Map<String, Object>> getCollectionRecipes(int collectionId);
    List<Equipment> getEquipmentByUser(int userId);
    Equipment addEquipment(int userId, String category, String name, String brand, String notes);
    Optional<Equipment> getEquipmentById(int id);
    void deleteEquipment(int id, int userId);
}
