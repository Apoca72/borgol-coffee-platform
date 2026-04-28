package borgol.core.application;

import borgol.core.domain.*;
import borgol.infrastructure.messaging.RedisEventBus;
import borgol.infrastructure.persistence.BorgolRepository;

import java.util.List;
import java.util.Map;

/**
 * Borgol платформын Facade — domain service-уудыг нэг цэгт нэгтгэнэ.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Facade (GoF)
 * ════════════════════════════════════════════════════════════
 * Зорилго: BorgolApiServer, Main, desktop UI зэрэг клиентүүд энэ
 * нэг классаар ажиллана — domain service-уудын мэдэх шаардлагагүй.
 *
 * Зарчим (SOLID):
 *  - Single Responsibility → зөвхөн дамжуулалт, бизнес логик байхгүй.
 *  - Dependency Inversion  → Repository + EventBus DI-аар хүлээн авна.
 */
public class BorgolService {

    // ── Domain services ───────────────────────────────────────────────────────
    private final UserService        userService;
    private final RecipeService      recipeService;
    private final BrewGuideService   brewGuideService;
    private final JournalService     journalService;
    private final CafeService        cafeService;
    private final AchievementService achievementService;

    // ── Public getters for domain routers ───────────────────────────────────
    public UserService        getUserService()        { return userService; }
    public RecipeService      getRecipeService()      { return recipeService; }
    public BrewGuideService   getBrewGuideService()   { return brewGuideService; }
    public JournalService     getJournalService()     { return journalService; }
    public CafeService        getCafeService()        { return cafeService; }
    public AchievementService getAchievementService() { return achievementService; }

    // ── Shared DTOs (retained for backward compatibility with desktop UI) ─────
    public record AuthResult(String token, UserView user) {}

    public record UserView(
        int id, String username, String email, String bio, String avatarUrl,
        String expertiseLevel, List<String> flavorPrefs,
        int followerCount, int followingCount, int recipeCount,
        boolean isFollowing, String createdAt
    ) {}

    // ── Constructor ───────────────────────────────────────────────────────────

    public BorgolService(BorgolRepository repo, RedisEventBus eventBus) {
        this.achievementService = new AchievementService(repo.getAchievementRepo());
        this.userService        = new UserService(repo.getUserRepo(), eventBus);
        this.recipeService      = new RecipeService(repo.getRecipeRepo(), repo.getUserRepo(),
                                                    achievementService, eventBus);
        this.brewGuideService   = new BrewGuideService(repo.getBrewGuideRepo());
        this.journalService     = new JournalService(repo.getJournalRepo(), achievementService);
        this.cafeService        = new CafeService(repo.getCafeRepo(), achievementService);
    }

    // ── Type bridge helpers ───────────────────────────────────────────────────

    private UserView toFacadeView(UserService.UserView v) {
        if (v == null) return null;
        return new UserView(v.id(), v.username(), v.email(), v.bio(), v.avatarUrl(),
                v.expertiseLevel(), v.flavorPrefs(),
                v.followerCount(), v.followingCount(), v.recipeCount(),
                v.isFollowing(), v.createdAt());
    }

    private AuthResult toFacadeAuth(UserService.AuthResult r) {
        return new AuthResult(r.token(), toFacadeView(r.user()));
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public AuthResult register(String username, String email, String password) {
        return toFacadeAuth(userService.register(username, email, password));
    }

    public AuthResult login(String email, String password) {
        return toFacadeAuth(userService.login(email, password));
    }

    public UserView getMe(int userId) {
        return toFacadeView(userService.getMe(userId));
    }

    public void deleteUser(int userId) {
        userService.deleteUser(userId);
    }

    // ── User queries ──────────────────────────────────────────────────────────

    public UserView getUserProfile(int targetId, int currentUserId) {
        return toFacadeView(userService.getUserProfile(targetId, currentUserId));
    }

    public UserView updateProfile(int userId, String bio, String avatarUrl, String expertiseLevel,
                                  List<String> flavorPrefs) {
        return toFacadeView(userService.updateProfile(userId, bio, avatarUrl, expertiseLevel, flavorPrefs));
    }

    public void followUser(int followerId, int followingId) {
        userService.followUser(followerId, followingId);
    }

    public void unfollowUser(int followerId, int followingId) {
        userService.unfollowUser(followerId, followingId);
    }

    public List<UserView> searchUsers(String query, int currentUserId) {
        return userService.searchUsers(query, currentUserId).stream().map(this::toFacadeView).toList();
    }

    public List<UserView> getAllUsers(int currentUserId) {
        return userService.getAllUsers(currentUserId).stream().map(this::toFacadeView).toList();
    }

    public List<UserView> getFollowingUsers(int userId, int currentUserId) {
        return userService.getFollowingUsers(userId, currentUserId).stream().map(this::toFacadeView).toList();
    }

    public List<UserView> getFollowerUsers(int userId, int currentUserId) {
        return userService.getFollowerUsers(userId, currentUserId).stream().map(this::toFacadeView).toList();
    }

    // ── Recipes ───────────────────────────────────────────────────────────────

    public List<Recipe> getRecipes(int currentUserId, String search, String drinkType, String sort) {
        return recipeService.getRecipes(currentUserId, search, drinkType, sort);
    }

    public List<Recipe> getFeed(int userId) {
        return recipeService.getFeed(userId);
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        return recipeService.getUserRecipes(authorId, currentUserId);
    }

    public Recipe getRecipe(int id, int currentUserId) {
        return recipeService.getRecipe(id, currentUserId);
    }

    public Recipe createRecipe(int authorId, String title, String description, String drinkType,
                                String ingredients, String instructions, int brewTime,
                                String difficulty, List<String> flavorTags, String imageUrl) {
        return recipeService.createRecipe(authorId, title, description, drinkType,
                ingredients, instructions, brewTime, difficulty, flavorTags, imageUrl);
    }

    public Recipe updateRecipe(int recipeId, int authorId, String title, String description,
                                String drinkType, String ingredients, String instructions,
                                int brewTime, String difficulty, List<String> flavorTags, String imageUrl) {
        return recipeService.updateRecipe(recipeId, authorId, title, description,
                drinkType, ingredients, instructions, brewTime, difficulty, flavorTags, imageUrl);
    }

    public void deleteRecipe(int recipeId, int userId) {
        recipeService.deleteRecipe(recipeId, userId);
    }

    public Map<String, Object> toggleLike(int userId, int recipeId) {
        return recipeService.toggleLike(userId, recipeId);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    public List<RecipeComment> getComments(int recipeId) {
        return recipeService.getComments(recipeId);
    }

    public RecipeComment addComment(int recipeId, int authorId, String content) {
        return recipeService.addComment(recipeId, authorId, content);
    }

    // ── Saved recipes ─────────────────────────────────────────────────────────

    public Map<String, Object> toggleSave(int userId, int recipeId) {
        return recipeService.toggleSave(userId, recipeId);
    }

    public List<Recipe> getSavedRecipes(int userId, int currentUserId) {
        return recipeService.getSavedRecipes(userId, currentUserId);
    }

    public List<Recipe> getLikedRecipes(int userId, int currentUserId) {
        return recipeService.getLikedRecipes(userId, currentUserId);
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    public List<Recipe> getHashtagRecipes(int currentUserId, String tag) {
        return recipeService.getHashtagRecipes(currentUserId, tag);
    }

    public List<Map<String, Object>> getTrendingHashtags() {
        return recipeService.getTrendingHashtags();
    }

    public void followHashtag(int userId, String tag) {
        userService.followHashtag(userId, tag);
    }

    public void unfollowHashtag(int userId, String tag) {
        userService.unfollowHashtag(userId, tag);
    }

    public List<String> getUserHashtags(int userId) {
        return userService.getUserHashtags(userId);
    }

    // ── Equipment ─────────────────────────────────────────────────────────────

    public List<Equipment> getEquipment(int userId) {
        return recipeService.getEquipment(userId);
    }

    public Equipment addEquipment(int userId, String category, String name, String brand, String notes) {
        return recipeService.addEquipment(userId, category, name, brand, notes);
    }

    public void deleteEquipment(int equipmentId, int userId) {
        recipeService.deleteEquipment(equipmentId, userId);
    }

    // ── Collections ───────────────────────────────────────────────────────────

    public List<Map<String, Object>> getCollections(int userId) {
        return recipeService.getCollections(userId);
    }

    public Map<String, Object> createCollection(int userId, String name, String description, boolean isPublic) {
        return recipeService.createCollection(userId, name, description, isPublic);
    }

    public void deleteCollection(int id, int userId) {
        recipeService.deleteCollection(id, userId);
    }

    public void addRecipeToCollection(int collectionId, int recipeId, int userId) {
        recipeService.addRecipeToCollection(collectionId, recipeId, userId);
    }

    public void removeRecipeFromCollection(int collectionId, int recipeId, int userId) {
        recipeService.removeRecipeFromCollection(collectionId, recipeId, userId);
    }

    public List<Map<String, Object>> getCollectionRecipes(int collectionId) {
        return recipeService.getCollectionRecipes(collectionId);
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public void submitReport(int reporterId, String contentType, int contentId,
                              String reason, String description) {
        recipeService.submitReport(reporterId, contentType, contentId, reason, description);
    }

    public List<Map<String, Object>> getReports(String status) {
        return recipeService.getReports(status);
    }

    public void resolveReport(int reportId, int adminId, String action) {
        recipeService.resolveReport(reportId, adminId, action);
    }

    public Map<String, Object> getAdminStats() {
        return recipeService.getAdminStats();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    public boolean isAdmin(int userId) {
        return userService.isAdmin(userId);
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    public void blockUser(int blockerId, int blockedId) {
        userService.blockUser(blockerId, blockedId);
    }

    public void unblockUser(int blockerId, int blockedId) {
        userService.unblockUser(blockerId, blockedId);
    }

    public boolean isBlocked(int blockerId, int blockedId) {
        return userService.isBlocked(blockerId, blockedId);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getNotifications(int userId) {
        return userService.getNotifications(userId);
    }

    public void markNotificationsRead(int userId) {
        userService.markNotificationsRead(userId);
    }

    public Map<String, Object> getNotificationCount(int userId) {
        return userService.getNotificationCount(userId);
    }

    // ── Cafes ─────────────────────────────────────────────────────────────────

    public List<CafeListing> getCafes(int currentUserId, String search, String district) {
        return cafeService.getCafes(currentUserId, search, district);
    }

    public CafeListing getCafe(int id, int currentUserId) {
        return cafeService.getCafe(id, currentUserId);
    }

    public CafeListing createCafe(int submittedBy, String name, String address, String district,
                                   String city, String phone, String description, String hours,
                                   String imageUrl) {
        return cafeService.createCafe(submittedBy, name, address, district, city, phone, description, hours, imageUrl);
    }

    public CafeListing rateCafe(int userId, int cafeId, int rating, String review) {
        return cafeService.rateCafe(userId, cafeId, rating, review);
    }

    public void updateCafeCoordinates(int cafeId, double lat, double lng) {
        cafeService.updateCafeCoordinates(cafeId, lat, lng);
    }

    public List<CafeListing> getCafesNearby(int currentUserId, double lat, double lng, double radiusKm) {
        return cafeService.getCafesNearby(currentUserId, lat, lng, radiusKm);
    }

    // ── Check-ins ─────────────────────────────────────────────────────────────

    public Map<String, Object> checkIn(int cafeId, int userId, String note) {
        return cafeService.checkIn(cafeId, userId, note);
    }

    public List<Map<String, Object>> getCheckins(int cafeId) {
        return cafeService.getCheckins(cafeId);
    }

    // ── Brew Guides ───────────────────────────────────────────────────────────

    public List<BrewGuide> getBrewGuides() {
        return brewGuideService.getBrewGuides();
    }

    public BrewGuide getBrewGuide(int id) {
        return brewGuideService.getBrewGuide(id);
    }

    // ── Learn Articles ────────────────────────────────────────────────────────

    public List<LearnArticle> getLearnArticles() {
        return brewGuideService.getLearnArticles();
    }

    public LearnArticle getLearnArticle(int id) {
        return brewGuideService.getLearnArticle(id);
    }

    // ── Static content seeding ────────────────────────────────────────────────

    public void seedStaticContent() {
        brewGuideService.seedStaticContent();
    }

    public void seedEnrichedContent() {
        brewGuideService.seedEnrichedContent();
        cafeService.seedCafes();
    }

    // ── Brew Journal ──────────────────────────────────────────────────────────

    public List<BrewJournalEntry> getJournalEntries(int userId) {
        return journalService.getJournalEntries(userId);
    }

    public BrewJournalEntry getJournalEntry(int id, int userId) {
        return journalService.findJournalEntry(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }

    public BrewJournalEntry createJournalEntry(int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes, String weatherData) {
        return journalService.createJournalEntry(userId, coffeeBean, origin, roastLevel,
                brewMethod, grindSize, waterTempC, doseGrams, yieldGrams, brewTimeSec,
                ratingAroma, ratingFlavor, ratingAcidity, ratingBody, ratingSweetness, ratingFinish,
                notes, weatherData);
    }

    public BrewJournalEntry updateJournalEntry(int id, int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes, String weatherData) {
        return journalService.updateJournalEntry(id, userId, coffeeBean, origin, roastLevel,
                brewMethod, grindSize, waterTempC, doseGrams, yieldGrams, brewTimeSec,
                ratingAroma, ratingFlavor, ratingAcidity, ratingBody, ratingSweetness, ratingFinish,
                notes, weatherData);
    }

    public void deleteJournalEntry(int id, int userId) {
        boolean deleted = journalService.deleteJournalEntry(id, userId);
        if (!deleted) throw new IllegalArgumentException("Entry not found or not authorized");
    }

    public Map<String, Object> getJournalStats(int userId) {
        return journalService.getJournalStats(userId);
    }

    // ── Bean Bags ─────────────────────────────────────────────────────────────

    public List<BeanBag> getBeanBags(int userId) {
        return journalService.getBeanBags(userId);
    }

    public BeanBag createBeanBag(int userId, String name, String roaster,
            String origin, String roastLevel, String roastDate,
            double remainingGrams, int rating, String notes) {
        BeanBag b = new BeanBag();
        b.setUserId(userId);
        b.setName(name);
        b.setRoaster(roaster != null ? roaster : "");
        b.setOrigin(origin != null ? origin : "");
        b.setRoastLevel(roastLevel != null ? roastLevel : "MEDIUM");
        b.setRoastDate(roastDate);
        b.setRemainingGrams(remainingGrams);
        b.setRating(Math.max(0, Math.min(5, rating)));
        b.setNotes(notes != null ? notes : "");
        return journalService.createBeanBag(b);
    }

    public BeanBag updateBeanBag(int id, int userId, String name,
            String roaster, String origin, String roastLevel, String roastDate,
            double remainingGrams, int rating, String notes) {
        BeanBag b = new BeanBag();
        b.setId(id);
        b.setUserId(userId);
        b.setName(name);
        b.setRoaster(roaster != null ? roaster : "");
        b.setOrigin(origin != null ? origin : "");
        b.setRoastLevel(roastLevel != null ? roastLevel : "MEDIUM");
        b.setRoastDate(roastDate);
        b.setRemainingGrams(remainingGrams);
        b.setRating(Math.max(0, Math.min(5, rating)));
        b.setNotes(notes != null ? notes : "");
        return journalService.updateBeanBag(b);
    }

    public void deleteBeanBag(int id, int userId) {
        journalService.deleteBeanBag(id, userId);
    }

    // ── Achievements ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> getAchievements(int userId) {
        return achievementService.getAchievements(userId);
    }

    public List<String> checkAndAwardAchievements(int userId) {
        return achievementService.checkAndAwardAchievements(userId);
    }
}
