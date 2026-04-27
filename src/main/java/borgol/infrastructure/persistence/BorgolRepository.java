package borgol.infrastructure.persistence;

import borgol.core.domain.*;
import borgol.infrastructure.config.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * Fagade delegator — domain repository-уудыг нэгтгэнэ.
 * BorgolService (Desktop Facade) хуучин API-г дуудсаар байна.
 *
 * Загвар: Facade (GoF) — дотоод хуваагдлыг нуун, нэг интерфейс гаргана.
 */
public class BorgolRepository {

    // Domain repositories — injected via constructor
    final UserRepository         userRepo;
    final RecipeRepository       recipeRepo;
    final BrewGuideRepository    brewGuideRepo;
    final JournalRepository      journalRepo;
    final CafeRepository         cafeRepo;
    final AchievementRepository  achievementRepo;

    public BorgolRepository(DatabaseConnection db) {
        new SchemaInitializer(db).run();
        this.userRepo        = new UserRepository(db);
        this.recipeRepo      = new RecipeRepository(db);
        this.brewGuideRepo   = new BrewGuideRepository(db);
        this.journalRepo     = new JournalRepository(db);
        this.cafeRepo        = new CafeRepository(db);
        this.achievementRepo = new AchievementRepository(db);
    }

    // ── User ─────────────────────────────────────────────────────────────────
    public Optional<User> findUserById(int id)                          { return userRepo.findUserById(id); }
    public Optional<User> findUserByEmail(String email)                 { return userRepo.findUserByEmail(email); }
    public Optional<User> findUserByUsername(String username)           { return userRepo.findUserByUsername(username); }
    public User createUser(String u, String e, String p)                { return userRepo.createUser(u, e, p); }
    public void deleteUser(int id)                                      { userRepo.deleteUser(id); }
    public void updateUser(int id, String bio, String av, String exp)   { userRepo.updateUser(id, bio, av, exp); }
    public List<String> getUserFlavorPrefs(int uid)                     { return userRepo.getUserFlavorPrefs(uid); }
    public void setUserFlavorPrefs(int uid, List<String> f)             { userRepo.setUserFlavorPrefs(uid, f); }
    public int getFollowerCount(int uid)                                { return userRepo.getFollowerCount(uid); }
    public int getFollowingCount(int uid)                               { return userRepo.getFollowingCount(uid); }
    public int getUserRecipeCount(int uid)                              { return userRepo.getUserRecipeCount(uid); }
    public boolean isFollowing(int a, int b)                            { return userRepo.isFollowing(a, b); }
    public void followUser(int a, int b)                                { userRepo.followUser(a, b); }
    public void unfollowUser(int a, int b)                              { userRepo.unfollowUser(a, b); }
    public List<User> searchUsers(String q)                             { return userRepo.searchUsers(q); }
    public List<User> findAllUsers(int limit)                           { return userRepo.findAllUsers(limit); }
    public List<User> getFollowingUsers(int uid)                        { return userRepo.getFollowingUsers(uid); }
    public List<User> getFollowerUsers(int uid)                         { return userRepo.getFollowerUsers(uid); }
    public void blockUser(int a, int b)                                 { userRepo.blockUser(a, b); }
    public void unblockUser(int a, int b)                               { userRepo.unblockUser(a, b); }
    public boolean isBlocked(int a, int b)                              { return userRepo.isBlocked(a, b); }
    public void followHashtag(int uid, String tag)                      { userRepo.followHashtag(uid, tag); }
    public void unfollowHashtag(int uid, String tag)                    { userRepo.unfollowHashtag(uid, tag); }
    public List<String> getUserHashtags(int uid)                        { return userRepo.getUserHashtags(uid); }
    public void createNotification(int u, String t, int f, int c, String m) { userRepo.createNotification(u, t, f, c, m); }
    public List<Map<String,Object>> getNotifications(int uid, int lim)  { return userRepo.getNotifications(uid, lim); }
    public void markNotificationsRead(int uid)                          { userRepo.markNotificationsRead(uid); }
    public int getUnreadNotificationCount(int uid)                      { return userRepo.getUnreadNotificationCount(uid); }

    // ── Recipe ────────────────────────────────────────────────────────────────
    public List<Recipe> findAllRecipes(int uid, String s, String d, String so) { return recipeRepo.findAllRecipes(uid, s, d, so); }
    public List<Recipe> getFeedRecipes(int uid, int lim)                { return recipeRepo.getFeedRecipes(uid, lim); }
    public List<Recipe> getUserRecipes(int aid, int uid)                { return recipeRepo.getUserRecipes(aid, uid); }
    public Optional<Recipe> findRecipeById(int id, int uid)             { return recipeRepo.findRecipeById(id, uid); }
    public Recipe createRecipe(Recipe r)                                { return recipeRepo.createRecipe(r); }
    public Recipe updateRecipe(Recipe r)                                { return recipeRepo.updateRecipe(r); }
    public boolean deleteRecipe(int id, int uid)                        { return recipeRepo.deleteRecipe(id, uid); }
    public boolean likeRecipe(int uid, int rid)                         { return recipeRepo.likeRecipe(uid, rid); }
    public boolean unlikeRecipe(int uid, int rid)                       { return recipeRepo.unlikeRecipe(uid, rid); }
    public List<RecipeComment> findCommentsByRecipeId(int rid)          { return recipeRepo.findCommentsByRecipeId(rid); }
    public RecipeComment addComment(int rid, int aid, String c)         { return recipeRepo.addComment(rid, aid, c); }
    public List<Recipe> getLikedRecipes(int uid, int cuid)              { return recipeRepo.getLikedRecipes(uid, cuid); }
    public void saveRecipe(int uid, int rid)                            { recipeRepo.saveRecipe(uid, rid); }
    public void unsaveRecipe(int uid, int rid)                          { recipeRepo.unsaveRecipe(uid, rid); }
    public boolean isRecipeSaved(int uid, int rid)                      { return recipeRepo.isRecipeSaved(uid, rid); }
    public List<Recipe> getSavedRecipes(int uid, int cuid)              { return recipeRepo.getSavedRecipes(uid, cuid); }
    public List<Recipe> getRecipesByHashtag(int cuid, String tag)       { return recipeRepo.getRecipesByHashtag(cuid, tag); }
    public List<Map<String,Object>> getTrendingHashtags(int lim)        { return recipeRepo.getTrendingHashtags(lim); }
    public void createReport(int r, String ct, int ci, String re, String d) { recipeRepo.createReport(r, ct, ci, re, d); }
    public List<Map<String,Object>> getAllReports(String status)         { return recipeRepo.getAllReports(status); }
    public void resolveReport(int rid, int by, String s)                { recipeRepo.resolveReport(rid, by, s); }
    public int getPendingReportCount()                                   { return recipeRepo.getPendingReportCount(); }
    public List<Map<String,Object>> getCollections(int uid)             { return recipeRepo.getCollections(uid); }
    public Map<String,Object> createCollection(int uid, String n, String d, boolean p) { return recipeRepo.createCollection(uid, n, d, p); }
    public void deleteCollection(int id, int uid)                       { recipeRepo.deleteCollection(id, uid); }
    public void addRecipeToCollection(int cid, int rid, int uid)        { recipeRepo.addRecipeToCollection(cid, rid, uid); }
    public void removeRecipeFromCollection(int cid, int rid, int uid)   { recipeRepo.removeRecipeFromCollection(cid, rid, uid); }
    public List<Map<String,Object>> getCollectionRecipes(int cid)       { return recipeRepo.getCollectionRecipes(cid); }
    public List<Equipment> getEquipmentByUser(int uid)                  { return recipeRepo.getEquipmentByUser(uid); }
    public Equipment addEquipment(int uid, String cat, String n, String b, String no) { return recipeRepo.addEquipment(uid, cat, n, b, no); }
    public Optional<Equipment> getEquipmentById(int id)                 { return recipeRepo.getEquipmentById(id); }
    public void deleteEquipment(int id, int uid)                        { recipeRepo.deleteEquipment(id, uid); }

    // ── BrewGuide ─────────────────────────────────────────────────────────────
    public List<BrewGuide> findAllBrewGuides()                          { return brewGuideRepo.findAllBrewGuides(); }
    public Optional<BrewGuide> findBrewGuideById(int id)                { return brewGuideRepo.findBrewGuideById(id); }
    public List<LearnArticle> findAllLearnArticles()                    { return brewGuideRepo.findAllLearnArticles(); }
    public Optional<LearnArticle> findLearnArticleById(int id)         { return brewGuideRepo.findLearnArticleById(id); }
    public boolean isStaticContentSeeded()                              { return brewGuideRepo.isStaticContentSeeded(); }
    public void seedBrewGuide(BrewGuide g)                              { brewGuideRepo.seedBrewGuide(g); }
    public void seedLearnArticle(LearnArticle a)                        { brewGuideRepo.seedLearnArticle(a); }
    public boolean isBeanArticlesSeeded()                               { return brewGuideRepo.isBeanArticlesSeeded(); }
    public boolean isCafesSeeded()                                      { return brewGuideRepo.isCafesSeeded(); }
    public boolean isDrinkArticlesSeeded()                              { return brewGuideRepo.isDrinkArticlesSeeded(); }

    // ── Journal ───────────────────────────────────────────────────────────────
    public List<BrewJournalEntry> getJournalEntries(int uid)            { return journalRepo.getJournalEntries(uid); }
    public Optional<BrewJournalEntry> findJournalEntry(int id, int uid) { return journalRepo.findJournalEntry(id, uid); }
    public BrewJournalEntry createJournalEntry(BrewJournalEntry e)      { return journalRepo.createJournalEntry(e); }
    public BrewJournalEntry updateJournalEntry(BrewJournalEntry e)      { return journalRepo.updateJournalEntry(e); }
    public boolean deleteJournalEntry(int id, int uid)                  { return journalRepo.deleteJournalEntry(id, uid); }
    public List<BeanBag> getBeanBags(int uid)                           { return journalRepo.getBeanBags(uid); }
    public BeanBag createBeanBag(BeanBag b)                             { return journalRepo.createBeanBag(b); }
    public BeanBag updateBeanBag(BeanBag b)                             { return journalRepo.updateBeanBag(b); }
    public void deleteBeanBag(int id, int uid)                          { journalRepo.deleteBeanBag(id, uid); }
    public Map<String,Object> getJournalStats(int uid)                  { return journalRepo.getJournalStats(uid); }

    // ── Cafe ──────────────────────────────────────────────────────────────────
    public List<CafeListing> findAllCafes(int uid, String s, String d)  { return cafeRepo.findAllCafes(uid, s, d); }
    public Optional<CafeListing> findCafeById(int id, int uid)          { return cafeRepo.findCafeById(id, uid); }
    public CafeListing createCafe(CafeListing c)                        { return cafeRepo.createCafe(c); }
    public void updateCafeCoordinates(int id, double lat, double lng)   { cafeRepo.updateCafeCoordinates(id, lat, lng); }
    public List<CafeListing> findCafesNearby(int uid, double la, double la2, double lo, double lo2) { return cafeRepo.findCafesNearby(uid, la, la2, lo, lo2); }
    public boolean rateCafe(int uid, int cid, int r, String rev)        { return cafeRepo.rateCafe(uid, cid, r, rev); }
    public Map<String,Object> checkIn(int cid, int uid, String note)    { return cafeRepo.checkIn(cid, uid, note); }
    public List<Map<String,Object>> getCheckins(int cid)                { return cafeRepo.getCheckins(cid); }

    // ── Achievement ───────────────────────────────────────────────────────────
    public List<Map<String,Object>> getAchievements(int uid, Map<String,String[]> meta) { return achievementRepo.getAchievements(uid, meta); }
    public List<String> checkAndAwardAchievements(int uid, Map<String,String[]> meta)   { return achievementRepo.checkAndAwardAchievements(uid, meta); }
}
