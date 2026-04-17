package mn.edu.num.cafe.core.application;

import mn.edu.num.cafe.core.domain.*;
import mn.edu.num.cafe.infrastructure.cache.CacheKeyBuilder;
import mn.edu.num.cafe.infrastructure.cache.RedisClient;
import mn.edu.num.cafe.infrastructure.email.EmailService;
import mn.edu.num.cafe.infrastructure.persistence.BorgolRepository;
import mn.edu.num.cafe.infrastructure.security.JwtUtil;
import mn.edu.num.cafe.infrastructure.security.PasswordUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Borgol платформын үндсэн бизнесийн логикийн давхарга.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Service Layer (Fowler's Patterns of Enterprise App)
 * ════════════════════════════════════════════════════════════
 * Зорилго: HTTP handler (BorgolApiServer) болон SQL (BorgolRepository)
 * хоёрын хооронд бизнесийн дүрмийг хэрэгжүүлнэ.
 *
 * Зарчим (SOLID):
 *  - Single Responsibility → Auth, User, Recipe, Cafe тус бүрийн
 *    бизнес дүрмийг нэг газар хариуцна.
 *  - Open/Closed → Repository-г солих боломжтой, Service өөрчлөгдөхгүй.
 *
 * Архитектур: Hexagonal (Ports & Adapters)
 *  - Core domain (энэ класс) нь web болон DB-ийн технологиос тусгаарлагдана.
 *  - BorgolRepository → DB Adapter (гадагш порт)
 *  - BorgolApiServer  → Web Adapter (дотогш порт)
 */
public class BorgolService {

    private static final Logger log = LoggerFactory.getLogger(BorgolService.class);

    // Repository-г DI (Dependency Injection) аргаар хүлээн авна
    // → тест хийхэд mock оруулах боломжтой
    private final BorgolRepository repo;

    /**
     * Gson жишээ — кэш давхаргад JSON сериализаци/десериализацид ашиглана.
     * Thread-safe: Gson нь immutable, хуваалцах боломжтой.
     */
    private final Gson gson = new Gson();

    public BorgolService(BorgolRepository repo) {
        this.repo = repo;
    }

    // ── Кэш туслах методууд ───────────────────────────────────────────────────
    // Зарчим: DRY — кэш логикийг нэг газар тодорхойлж давтахгүй.
    // Redis алдаа гарвал silent degradation — DB-с шууд унших горимд ордог.

    /**
     * Redis-ээс JSON утга уншиж заасан төрөл рүү хөрвүүлнэ.
     * HIT/MISS-г DEBUG лог болгоно.
     *
     * @param key      кэшийн түлхүүр
     * @param type     deserialize хийх Java төрөл (TypeToken.getType())
     * @param <T>      буцаах объектын төрөл
     * @return         кэшэд байвал объект, байхгүй/алдаа бол null (MISS)
     */
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

    /**
     * Объектыг JSON болгон Redis-д TTL-тэй хадгална.
     *
     * @param key        кэшийн түлхүүр
     * @param value      хадгалах объект
     * @param ttlSeconds хугацаа (секундээр)
     */
    private void cachePut(String key, Object value, int ttlSeconds) {
        try {
            RedisClient.get().setex(key, ttlSeconds, gson.toJson(value));
        } catch (Exception e) {
            log.debug("[Cache] Redis бичихэд алдаа: {} — {}", key, e.getMessage());
        }
    }

    /**
     * Redis-ээс кэшийг устгана — бичих үйлдлийн дараа кэш invalidation.
     * Cache-aside хэв маягийн чухал хэсэг: бичсний дараа устгана.
     *
     * @param key устгах кэшийн түлхүүр
     */
    private void cacheEvict(String key) {
        try {
            RedisClient.get().del(key);
            log.debug("[Cache EVICT] {}", key);
        } catch (Exception e) {
            log.debug("[Cache] Redis устгахад алдаа: {} — {}", key, e.getMessage());
        }
    }

    // ── Нэвтрэх / Бүртгэх ────────────────────────────────────────────────────
    // Зарчим: Fail Fast — оролтын баталгаажуулалт эхэлж хийнэ,
    // DB-д хандахаас өмнө алдааг илрүүлж, 400 буцаана.

    // Java Record → immutable value object, getter автоматаар үүснэ
    public record AuthResult(String token, UserView user) {}

    public AuthResult register(String username, String email, String password) {
        // ── Оролтын баталгаажуулалт (Input Validation) ──────────────────────
        // Зарчим: Defense in Depth — хэрэглэгчийн оролтыг хэзээ ч итгэхгүй
        if (username == null || username.isBlank())  throw new IllegalArgumentException("Username is required");
        if (email == null    || email.isBlank())     throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be ≥ 6 characters");
        if (!email.contains("@"))                    throw new IllegalArgumentException("Invalid email address");
        if (username.length() < 3 || username.length() > 50) throw new IllegalArgumentException("Username must be 3–50 characters");
        if (!username.matches("[a-zA-Z0-9_]+"))     throw new IllegalArgumentException("Username: letters, numbers, underscores only");

        // ── Давхардал шалгах (Uniqueness check) ─────────────────────────────
        if (repo.findUserByEmail(email).isPresent())      throw new IllegalArgumentException("Email already registered");
        if (repo.findUserByUsername(username).isPresent()) throw new IllegalArgumentException("Username already taken");

        // ── Нууц үг хэш хийх + хэрэглэгч үүсгэх ───────────────────────────
        // PasswordUtil.hash() → SHA-256 + random salt — текст хадгалахгүй
        String hash = PasswordUtil.hash(password);
        User user   = repo.createUser(username, email, hash);
        // JwtUtil → HMAC-SHA256 токен үүсгэнэ, 7 хоног хүчинтэй
        String token = JwtUtil.createToken(user.getId(), user.getUsername());

        // ── Тавтай морилох имэйл илгээх (silent fail) ───────────────────────
        // EmailService: SMTP тохируулагдаагүй бол аяндаа алгасна (log.debug)
        EmailService.get().sendWelcomeEmail(email, username);

        return new AuthResult(token, toView(user, 0, false));
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank())       throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        // Optional.orElseThrow() → хэрэглэгч олдохгүй бол "Invalid email or password"
        // Аюулгүй байдал: "User not found" гэж хэлэхгүй — тухайн и-мэйл бүртгэлтэй эсэхийг нуuna
        User user = repo.findUserByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        // constantTimeEquals → timing attack-аас хамгаалсан нууц үг шалгалт
        if (!PasswordUtil.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");

        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    public UserView getMe(int userId) {
        // ── Cache-aside: borgol:user:{userId}, TTL 600с ──────────────────────
        String key = CacheKeyBuilder.forUser(userId);
        UserView cached = cacheGet(key, UserView.class);
        if (cached != null) return cached;

        User user = repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserView view = toView(user, 0, false);
        cachePut(key, view, 600);
        return view;
    }

    public void deleteUser(int userId) {
        repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        repo.deleteUser(userId);
    }

    // ── Хэрэглэгчийн үйлдлүүд ────────────────────────────────────────────────

    public UserView getUserProfile(int targetId, int currentUserId) {
        // isFollowing → дагаж байгаа эсэхийг нэмэлт мэдээлэл болгон буцаана
        // currentUserId == 0 → нэвтрээгүй хэрэглэгч (зочин)
        User user = repo.findUserById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean isFollowing = currentUserId > 0 && repo.isFollowing(currentUserId, targetId);
        return toView(user, currentUserId, isFollowing);
    }

    public UserView updateProfile(int userId, String bio, String avatarUrl, String expertiseLevel,
                                  List<String> flavorPrefs) {
        // Validate expertise level
        List<String> validLevels = List.of("BEGINNER", "ENTHUSIAST", "BARISTA", "EXPERT");
        if (expertiseLevel != null && !validLevels.contains(expertiseLevel.toUpperCase()))
            expertiseLevel = "BEGINNER";

        repo.updateUser(userId,
            bio != null ? bio : "",
            avatarUrl != null ? avatarUrl : "",
            expertiseLevel != null ? expertiseLevel.toUpperCase() : "BEGINNER");

        if (flavorPrefs != null) repo.setUserFlavorPrefs(userId, flavorPrefs);

        // Профайл шинэчлэгдсэн тул хэрэглэгчийн кэш хүчингүй болгоно
        cacheEvict(CacheKeyBuilder.forUser(userId));

        return getMe(userId);
    }

    public void followUser(int followerId, int followingId) {
        // Өөрийгөө дагах боломжгүй — бизнесийн дүрэм
        if (followerId == followingId) throw new IllegalArgumentException("Cannot follow yourself");
        repo.followUser(followerId, followingId);
        // Дагасны дараа автоматаар мэдэгдэл үүсгэнэ — Observer хэв маягтай төстэй
        repo.createNotification(followingId, "follow", followerId, 0,
            "started following you");
    }

    public void unfollowUser(int followerId, int followingId) {
        repo.unfollowUser(followerId, followingId);
    }

    public List<UserView> searchUsers(String query, int currentUserId) {
        if (query == null || query.isBlank()) return List.of();
        return repo.searchUsers(query).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    // ── Жорын үйлдлүүд ───────────────────────────────────────────────────────
    // Зарчим: Thin Controller, Fat Service — HTTP handler нь зөвхөн дамжуулна,
    // бизнес логик (баталгаажуулалт, мэдэгдэл) энд байна.

    public List<Recipe> getRecipes(int currentUserId, String search, String drinkType, String sort) {
        return repo.findAllRecipes(currentUserId, search, drinkType, sort);
    }

    public List<Recipe> getFeed(int userId) {
        // ── Cache-aside: borgol:feed:userId:{userId}, TTL 60с ────────────────
        // Feed нь хэрэглэгч бүрт өөр тул userId кэш түлхүүрт орно.
        // Богино TTL (60с) — feed хурдан хуучирдаг (шинэ жор орж ирдэг).
        String key = CacheKeyBuilder.forFeed(userId);
        Type listType = new TypeToken<List<Recipe>>(){}.getType();
        List<Recipe> cached = cacheGet(key, listType);
        if (cached != null) return cached;

        List<Recipe> feed = repo.getFeedRecipes(userId, 30);
        cachePut(key, feed, 60);
        return feed;
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        return repo.getUserRecipes(authorId, currentUserId);
    }

    public Recipe getRecipe(int id, int currentUserId) {
        // ── Cache-aside: borgol:recipe:{id}, TTL 300с ────────────────────────
        // Анхааруулга: кэш нь хэрэглэгч-тусгаарлагдаагүй (shared by all users).
        // likedByCurrentUser, savedByCurrentUser талбарууд кэшэд хадгалагдана.
        // TTL 300с (5 мин) тул худал өгөгдлийн нөлөөлөл хязгаарлагдана.
        String key = CacheKeyBuilder.forRecipe(id);
        Recipe cached = cacheGet(key, Recipe.class);
        if (cached != null) return cached;

        Recipe recipe = repo.findRecipeById(id, currentUserId)
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
        return repo.createRecipe(r);
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
        Recipe updated = repo.updateRecipe(r);
        // Жор шинэчлэгдсэн тул кэш хүчингүй болгоно
        cacheEvict(CacheKeyBuilder.forRecipe(recipeId));
        return updated;
    }

    public void deleteRecipe(int recipeId, int userId) {
        boolean deleted = repo.deleteRecipe(recipeId, userId);
        if (!deleted) throw new IllegalArgumentException("Recipe not found or not authorized");
        // Жор устгагдсан тул кэш хүчингүй болгоно
        cacheEvict(CacheKeyBuilder.forRecipe(recipeId));
    }

    public Map<String, Object> toggleLike(int userId, int recipeId) {
        // ── Toggle логик: одоо like дарсан эсэхийг шалгаад эсрэгийг хийнэ ──
        boolean liked;
        if (repo.findRecipeById(recipeId, userId)
                .map(Recipe::isLikedByCurrentUser).orElse(false)) {
            repo.unlikeRecipe(userId, recipeId);
            liked = false;
        } else {
            repo.likeRecipe(userId, recipeId);
            liked = true;
            // Өөрийн жорт like дарсан бол мэдэгдэл илгээхгүй (spam хориглох)
            repo.findRecipeById(recipeId, userId).ifPresent(r -> {
                if (r.getAuthorId() != userId) {
                    repo.createNotification(r.getAuthorId(), "like", userId, recipeId,
                        "liked your recipe \"" + r.getTitle() + "\"");
                }
            });
        }
        // Шинэчлэгдсэн тооллогыг буцаана — frontend дахин дуудахгүйгээр шинэчилнэ
        int count = repo.findRecipeById(recipeId, userId)
            .map(Recipe::getLikesCount).orElse(0);
        return Map.of("liked", liked, "likesCount", count);
    }

    // ── Сэтгэгдлийн үйлдлүүд ─────────────────────────────────────────────────

    public List<RecipeComment> getComments(int recipeId) {
        return repo.findCommentsByRecipeId(recipeId);
    }

    public RecipeComment addComment(int recipeId, int authorId, String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Comment cannot be empty");
        if (content.length() > 1000) throw new IllegalArgumentException("Comment too long (max 1000 chars)");
        RecipeComment comment = repo.addComment(recipeId, authorId, content);
        // Notify author
        repo.findRecipeById(recipeId, authorId).ifPresent(r -> {
            if (r.getAuthorId() != authorId) {
                repo.createNotification(r.getAuthorId(), "comment", authorId, recipeId,
                    "commented on your recipe \"" + r.getTitle() + "\"");
            }
        });
        return comment;
    }

    // ── Кафены үйлдлүүд ──────────────────────────────────────────────────────

    public List<CafeListing> getCafes(int currentUserId, String search, String district) {
        return repo.findAllCafes(currentUserId, search, district);
    }

    public CafeListing getCafe(int id, int currentUserId) {
        return repo.findCafeById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Cafe not found: id=" + id));
    }

    public CafeListing createCafe(int submittedBy, String name, String address, String district,
                                   String city, String phone, String description, String hours,
                                   String imageUrl) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Cafe name is required");

        CafeListing c = new CafeListing();
        c.setName(name.trim());
        c.setAddress(address);
        c.setDistrict(district);
        c.setCity(city != null && !city.isBlank() ? city : "Ulaanbaatar");
        c.setPhone(phone);
        c.setDescription(description);
        c.setHours(hours);
        c.setImageUrl(imageUrl != null ? imageUrl : "");
        c.setSubmittedBy(submittedBy);
        return repo.createCafe(c);
    }

    public CafeListing rateCafe(int userId, int cafeId, int rating, String review) {
        repo.rateCafe(userId, cafeId, rating, review);
        return getCafe(cafeId, userId);
    }

    /** Update GPS pin for a cafe (seed use). */
    public void updateCafeCoordinates(int cafeId, double lat, double lng) {
        repo.updateCafeCoordinates(cafeId, lat, lng);
    }

    /**
     * (lat, lng) координатаас radiusKm км-ийн дотор байгаа кафенуудыг буцаана.
     *
     * Алгоритм: Equirectangular bounding-box наближение
     * ─ 111 км ≈ 1 өргөрөгийн зэрэг (latitude degree)
     * ─ Longitude зэрэг нь өргөргийн cosine-тай пропорциональ
     * ─ Хот дотор (< 50 км) энэ алдаа хүлцэгдэхүйц нарийвчлалтай
     *
     * Зарчим: Good Enough Solution — тригонометрийн нарийн томьёо
     * (Haversine) хэрэггүй, тойргийн хайлт бус дөрвөлжин хайлт хийнэ.
     */
    public List<CafeListing> getCafesNearby(int currentUserId, double lat, double lng, double radiusKm) {
        // ── Cache-aside: borgol:cafes:nearby:{lat}:{lng}, TTL 120с ───────────
        // Координатаар кэш хийнэ (userId орохгүй — газрын мэдээлэл нийтийн).
        // forCafesNearby нь 4 аравтын нарийвчлалтай форматлана (~11м).
        String key = CacheKeyBuilder.forCafesNearby(lat, lng);
        Type listType = new TypeToken<List<CafeListing>>(){}.getType();
        List<CafeListing> cached = cacheGet(key, listType);
        if (cached != null) return cached;

        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        // Дөрвөлжин bounding box → SQL-д BETWEEN ашиглаж шүүнэ
        List<CafeListing> result = repo.findCafesNearby(currentUserId,
            lat - latDelta, lat + latDelta,
            lng - lngDelta, lng + lngDelta);
        cachePut(key, result, 120);
        return result;
    }

    // ── Extra queries ─────────────────────────────────────────────────────────

    public List<UserView> getAllUsers(int currentUserId) {
        return repo.findAllUsers(60).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<Recipe> getLikedRecipes(int userId, int currentUserId) {
        return repo.getLikedRecipes(userId, currentUserId);
    }

    public List<UserView> getFollowingUsers(int userId, int currentUserId) {
        return repo.getFollowingUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    public List<UserView> getFollowerUsers(int userId, int currentUserId) {
        return repo.getFollowerUsers(userId).stream()
            .map(u -> {
                boolean following = currentUserId > 0 && repo.isFollowing(currentUserId, u.getId());
                return toView(u, currentUserId, following);
            }).toList();
    }

    // ── Brew Journal ──────────────────────────────────────────────────────────

    public List<BrewJournalEntry> getJournalEntries(int userId) {
        return repo.getJournalEntries(userId);
    }

    public BrewJournalEntry getJournalEntry(int id, int userId) {
        return repo.findJournalEntry(id, userId)
            .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }

    public BrewJournalEntry createJournalEntry(int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes) {
        BrewJournalEntry e = new BrewJournalEntry();
        e.setUserId(userId);
        e.setCoffeeBean(coffeeBean != null ? coffeeBean : "");
        e.setOrigin(origin != null ? origin : "");
        e.setRoastLevel(roastLevel != null ? roastLevel : "");
        e.setBrewMethod(brewMethod != null ? brewMethod : "");
        e.setGrindSize(grindSize != null ? grindSize : "");
        e.setWaterTempC(waterTempC);
        e.setDoseGrams(doseGrams);
        e.setYieldGrams(yieldGrams);
        e.setBrewTimeSec(brewTimeSec);
        e.setRatingAroma(ratingAroma);
        e.setRatingFlavor(ratingFlavor);
        e.setRatingAcidity(ratingAcidity);
        e.setRatingBody(ratingBody);
        e.setRatingSweetness(ratingSweetness);
        e.setRatingFinish(ratingFinish);
        e.setNotes(notes != null ? notes : "");
        return repo.createJournalEntry(e);
    }

    public BrewJournalEntry updateJournalEntry(int id, int userId, String coffeeBean, String origin,
            String roastLevel, String brewMethod, String grindSize, int waterTempC,
            double doseGrams, double yieldGrams, int brewTimeSec,
            int ratingAroma, int ratingFlavor, int ratingAcidity,
            int ratingBody, int ratingSweetness, int ratingFinish, String notes) {
        BrewJournalEntry e = new BrewJournalEntry();
        e.setId(id);
        e.setUserId(userId);
        e.setCoffeeBean(coffeeBean != null ? coffeeBean : "");
        e.setOrigin(origin != null ? origin : "");
        e.setRoastLevel(roastLevel != null ? roastLevel : "");
        e.setBrewMethod(brewMethod != null ? brewMethod : "");
        e.setGrindSize(grindSize != null ? grindSize : "");
        e.setWaterTempC(waterTempC);
        e.setDoseGrams(doseGrams);
        e.setYieldGrams(yieldGrams);
        e.setBrewTimeSec(brewTimeSec);
        e.setRatingAroma(ratingAroma);
        e.setRatingFlavor(ratingFlavor);
        e.setRatingAcidity(ratingAcidity);
        e.setRatingBody(ratingBody);
        e.setRatingSweetness(ratingSweetness);
        e.setRatingFinish(ratingFinish);
        e.setNotes(notes != null ? notes : "");
        return repo.updateJournalEntry(e);
    }

    public void deleteJournalEntry(int id, int userId) {
        boolean deleted = repo.deleteJournalEntry(id, userId);
        if (!deleted) throw new IllegalArgumentException("Entry not found or not authorized");
    }

    // ── Brew Guides ───────────────────────────────────────────────────────────

    public List<BrewGuide> getBrewGuides() {
        return repo.findAllBrewGuides();
    }

    public BrewGuide getBrewGuide(int id) {
        return repo.findBrewGuideById(id)
            .orElseThrow(() -> new IllegalArgumentException("Brew guide not found: id=" + id));
    }

    // ── Learn Articles ────────────────────────────────────────────────────────

    public List<LearnArticle> getLearnArticles() {
        return repo.findAllLearnArticles();
    }

    public LearnArticle getLearnArticle(int id) {
        return repo.findLearnArticleById(id)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: id=" + id));
    }

    // ── Static content seeding (idempotent) ───────────────────────────────────

    public void seedStaticContent() {
        if (repo.isStaticContentSeeded()) return;

        // ── Brew Guides ───────────────────────────────────────────────────────
        seedGuide("Pour Over (V60)", "☕",
            "A classic manual brew producing a clean, bright cup that highlights delicate flavors.",
            "BEGINNER", 4,
            "Coffee:15g\nWater:250ml\nGrind:Medium-fine\nTemp:92°C\nRatio:1:16.5",
            "1. Rinse the V60 filter with hot water and discard rinse water\n" +
            "2. Add 15g of medium-fine ground coffee to the filter\n" +
            "3. Create a small well in the center of the grounds\n" +
            "4. Bloom: pour 30ml of water and wait 30 seconds\n" +
            "5. Pour in slow concentric circles to reach 130ml at 1:00\n" +
            "6. Continue pouring to 250ml total by 2:00\n" +
            "7. Allow to drain completely — total time ~3:30");

        seedGuide("French Press", "🫖",
            "A full-immersion brew with rich body and bold flavors from the metal filter.",
            "BEGINNER", 5,
            "Coffee:30g\nWater:500ml\nGrind:Coarse\nTemp:95°C\nRatio:1:16",
            "1. Preheat the French press with hot water, then discard\n" +
            "2. Add 30g of coarsely ground coffee\n" +
            "3. Pour 500ml of water at 95°C over the grounds\n" +
            "4. Stir gently to ensure all grounds are saturated\n" +
            "5. Place the lid on with the plunger pulled up\n" +
            "6. Steep for exactly 4 minutes\n" +
            "7. Press plunger slowly and steadily — pour immediately");

        seedGuide("AeroPress", "🔄",
            "Versatile and forgiving — produces a smooth, espresso-like concentrate.",
            "INTERMEDIATE", 2,
            "Coffee:17g\nWater:220ml\nGrind:Medium\nTemp:85°C\nRatio:1:13",
            "1. Insert a paper filter into the AeroPress cap and rinse\n" +
            "2. Assemble in inverted position (plunger down)\n" +
            "3. Add 17g of ground coffee\n" +
            "4. Pour 220ml of water at 85°C and stir for 10 seconds\n" +
            "5. Secure the cap with filter\n" +
            "6. At 1:30 flip onto your cup carefully\n" +
            "7. Press steadily for 30 seconds — stop at first hiss");

        seedGuide("Espresso", "⚡",
            "High-pressure extraction creating an intense, concentrated shot with crema.",
            "ADVANCED", 1,
            "Coffee:18g\nYield:36g\nGrind:Extra-fine\nTemp:93°C\nPressure:9 bar",
            "1. Flush the group head with hot water for 2 seconds\n" +
            "2. Dose 18g of finely ground coffee into the portafilter\n" +
            "3. Distribute evenly and tamp with 15kg of pressure\n" +
            "4. Lock portafilter into the group head\n" +
            "5. Start extraction — aim for first drops at 5-7 seconds\n" +
            "6. Target 36g yield in 25-30 seconds total\n" +
            "7. Adjust grind finer if fast, coarser if slow");

        seedGuide("Cold Brew", "🧊",
            "Slow, cold extraction over 12–24 hours produces a smooth, sweet concentrate.",
            "BEGINNER", 720,
            "Coffee:100g\nWater:800ml\nGrind:Extra-coarse\nTemp:Cold (4°C)\nRatio:1:8",
            "1. Coarsely grind 100g of coffee beans\n" +
            "2. Combine coffee and 800ml of cold filtered water in a jar\n" +
            "3. Stir to ensure all grounds are saturated\n" +
            "4. Cover and refrigerate for 12–24 hours\n" +
            "5. Strain through a fine mesh strainer twice\n" +
            "6. Optionally pass through a paper filter for clarity\n" +
            "7. Store in fridge up to 2 weeks — dilute 1:1 to serve");

        seedGuide("Moka Pot", "🏠",
            "Stovetop espresso-style brew with rich, bittersweet flavors and heavy body.",
            "BEGINNER", 8,
            "Coffee:22g\nWater:200ml\nGrind:Fine-medium\nTemp:Stovetop\nRatio:1:9",
            "1. Fill the bottom chamber with hot water to the valve\n" +
            "2. Insert the filter basket and fill with 22g of ground coffee\n" +
            "3. Level the grounds without tamping\n" +
            "4. Screw the top chamber on tightly\n" +
            "5. Place on medium-low heat\n" +
            "6. Keep lid open and watch for coffee to emerge slowly\n" +
            "7. Remove from heat when sputtering — serve immediately");

        // ── Learn Articles ────────────────────────────────────────────────────
        seedArticle("Understanding Roast Levels", "🔥", "Roasting",
            "## Light Roast\n" +
            "Light roasts are roasted to an internal temperature of 180–205°C. " +
            "The beans are light brown and have no surface oils. " +
            "They preserve the most origin character — you'll taste the terroir, " +
            "the altitude, and the variety of the bean itself. " +
            "Expect floral, fruity, and tea-like notes with high acidity.\n\n" +
            "## Medium Roast\n" +
            "Roasted to 210–220°C, medium roasts balance origin flavor with roast character. " +
            "The beans are medium brown with little oil. You get sweetness, " +
            "caramel notes, and a balanced acidity. This is the most popular roast level " +
            "and works well for drip coffee and pour overs.\n\n" +
            "## Dark Roast\n" +
            "Dark roasts reach 225–245°C. The beans are dark brown to almost black " +
            "with oily surfaces. Roast flavors dominate — chocolate, bittersweet, smoky. " +
            "Origin character is mostly lost. Lower acidity, fuller body. " +
            "Classic for espresso and French press.", 4);

        seedArticle("The Science of Coffee Extraction", "⚗️", "Brewing Science",
            "## What Is Extraction?\n" +
            "Extraction is the process of dissolving soluble compounds from coffee grounds " +
            "into water. About 30% of a coffee bean is water-soluble, but you only want " +
            "to extract 18–22% for the best flavor.\n\n" +
            "## Under-Extraction\n" +
            "Under-extracted coffee (below 18%) tastes sour, salty, and lacking sweetness. " +
            "This happens when water is too cool, grind is too coarse, " +
            "brew time is too short, or the dose is too low.\n\n" +
            "## Over-Extraction\n" +
            "Over-extracted coffee (above 22%) tastes bitter, harsh, and dry. " +
            "Fix by using a coarser grind, lower water temperature, " +
            "shorter contact time, or less coffee.\n\n" +
            "## The Golden Ratio\n" +
            "The Specialty Coffee Association recommends a brew ratio of 1:15 to 1:17 " +
            "(coffee to water by weight). Start at 1:15 and adjust to taste.", 5);

        seedArticle("Water Quality for Coffee", "💧", "Brewing Science",
            "## Why Water Matters\n" +
            "Coffee is 98% water. The minerals dissolved in water dramatically affect " +
            "extraction and taste. Distilled water produces flat, lifeless coffee " +
            "because minerals help extract compounds from grounds.\n\n" +
            "## Ideal Mineral Content\n" +
            "The SCA recommends Total Dissolved Solids (TDS) of 75–250 ppm, " +
            "with a target of about 150 ppm. Magnesium ions enhance flavor extraction. " +
            "Calcium contributes to body. Too much sodium makes coffee taste salty.\n\n" +
            "## Temperature\n" +
            "Brew temperature should be 90–96°C (195–205°F). Below 88°C leads to " +
            "under-extraction. Above 96°C increases bitterness. " +
            "For lighter roasts, use higher temperatures (94–96°C). " +
            "For darker roasts, go lower (88–92°C).\n\n" +
            "## Practical Tips\n" +
            "Filtered tap water is usually ideal. Avoid softened water — " +
            "it replaces calcium and magnesium with sodium.", 4);

        seedArticle("Coffee Tasting & the Flavor Wheel", "🎨", "Tasting",
            "## What Is the Coffee Flavor Wheel?\n" +
            "The SCA Flavor Wheel maps the spectrum of coffee flavors into categories: " +
            "fruity, floral, sweet, nutty/cocoa, spicy, roasted, and savory. " +
            "It was created to give baristas and enthusiasts a shared vocabulary.\n\n" +
            "## How to Taste Coffee\n" +
            "Start by smelling the dry grounds (fragrance). Then smell the wet coffee (aroma). " +
            "Slurp the coffee to spray it across your palate. " +
            "Notice the flavors, the mouthfeel (body), acidity, and how it finishes.\n\n" +
            "## Key Attributes\n" +
            "**Aroma** — fragrances you smell before and during drinking.\n" +
            "**Acidity** — brightness or liveliness; citric, malic, or phosphoric.\n" +
            "**Body** — mouthfeel; thin, medium, or full/syrupy.\n" +
            "**Sweetness** — natural sugars that balance acidity and bitterness.\n" +
            "**Finish** — how flavors linger after swallowing.", 5);

        seedArticle("Arabica vs Robusta", "🌿", "Coffee Origins",
            "## Arabica (Coffea arabica)\n" +
            "Arabica makes up ~60% of global coffee production. " +
            "It grows at high altitudes (600–2000m) in tropical climates. " +
            "Arabica has lower caffeine (~1.5%), higher sugars and lipids, " +
            "and a wider flavor spectrum. Expect floral, fruity, chocolatey, or " +
            "caramel notes with pleasant acidity.\n\n" +
            "## Robusta (Coffea canephora)\n" +
            "Robusta grows at lower altitudes and is more disease-resistant. " +
            "It has nearly twice the caffeine of Arabica (~2.7%), " +
            "which acts as a natural pest deterrent. " +
            "Robusta is cheaper to produce and has a harsher, more bitter taste. " +
            "It's commonly used in instant coffee and espresso blends for crema.\n\n" +
            "## Which Is Better?\n" +
            "For specialty coffee, Arabica is the standard. " +
            "But high-quality Robusta from Uganda or Vietnam can be surprisingly complex " +
            "and is excellent for espresso blends that need more body and crema.", 4);

        seedArticle("Grind Size Guide", "⚙️", "Brewing Science",
            "## Why Grind Size Matters\n" +
            "Grind size determines the surface area exposed to water. " +
            "Finer grinds extract faster; coarser grinds extract slower. " +
            "Matching grind to brew method is essential for balanced extraction.\n\n" +
            "## Grind Size Chart\n" +
            "**Extra Fine** — Turkish coffee; powder-like consistency\n" +
            "**Fine** — Espresso; fine sand texture\n" +
            "**Medium-Fine** — Pour over (V60, Kalita); between sand and sea salt\n" +
            "**Medium** — Drip coffee, AeroPress; sea salt\n" +
            "**Medium-Coarse** — Chemex, Clever Dripper; rough sand\n" +
            "**Coarse** — French Press; coarse sea salt\n" +
            "**Extra Coarse** — Cold brew; peppercorn\n\n" +
            "## The Grinder Matters\n" +
            "Blade grinders produce inconsistent particles causing uneven extraction. " +
            "Burr grinders (conical or flat) create uniform grinds. " +
            "For specialty coffee, a quality burr grinder is the single best investment.", 5);
    }

    public void seedEnrichedContent() {
        // ── Specialty Bean Articles ───────────────────────────────────────────
        if (!repo.isBeanArticlesSeeded()) {
            seedArticle("Ethiopian Yirgacheffe", "🌸", "Beans",
                "## Origin\nYirgacheffe is a town in the Gedeo Zone of southern Ethiopia, widely " +
                "regarded as the birthplace of coffee. Grown at 1,700–2,200 m altitude.\n\n" +
                "## Flavor Profile\nLight roast reveals **jasmine, blueberry, lemon zest, and bergamot**. " +
                "Bright, tea-like acidity with a delicate, clean finish.\n\n" +
                "## Roast\nBest enjoyed as a **light roast** (180–195°C) to preserve floral aromatics. " +
                "Medium roast brings out more caramel sweetness. Avoid dark — it destroys the character.\n\n" +
                "## Brew Pairings\nPour Over (V60), AeroPress, or filter drip. " +
                "High-temperature water (93–96°C) works well with light roasts.", 4);

            seedArticle("Colombian Huila", "🏔️", "Beans",
                "## Origin\nHuila is a mountainous department in southwest Colombia at 1,500–2,000 m. " +
                "Known for its volcanic soil, consistent rainfall, and family-run farms.\n\n" +
                "## Flavor Profile\n**Caramel sweetness, red apple, citrus, and milk chocolate** " +
                "with a smooth medium body and balanced acidity.\n\n" +
                "## Roast\nExcels as a **medium roast** (210–220°C). Retains sweetness without " +
                "becoming bitter. One of the most versatile origins for any brew method.\n\n" +
                "## Brew Pairings\nWorks beautifully for espresso, pour over, and French press. " +
                "An excellent daily driver for home baristas.", 4);

            seedArticle("Kenyan AA", "🦁", "Beans",
                "## Origin\nKenya's central highlands around Mt. Kenya and the Aberdare Range " +
                "(1,400–2,000 m). 'AA' is the largest screen size grade, indicating large, dense beans.\n\n" +
                "## Flavor Profile\n**Blackcurrant, tomato, grapefruit, and wine-like brightness**. " +
                "Complex, juicy acidity with a full body and lingering finish.\n\n" +
                "## Roast\n**Light to medium** (190–215°C). The SL28 and SL34 varietals express " +
                "maximum complexity when not over-roasted. A challenging but rewarding origin.\n\n" +
                "## Brew Pairings\nPour over showcases the brightness. French press enhances body. " +
                "Not ideal for espresso — acidity becomes sharp under pressure.", 4);

            seedArticle("Guatemalan Antigua", "🌋", "Beans",
                "## Origin\nGrown in the Antigua valley surrounded by three volcanoes — Agua, Fuego, " +
                "and Acatenango — at 1,500–1,700 m. Volcanic ash provides exceptional mineral richness.\n\n" +
                "## Flavor Profile\n**Dark chocolate, brown sugar, almond, and mild spice** with a " +
                "medium-heavy body and low, soft acidity.\n\n" +
                "## Roast\nShines at **medium-dark roast** (220–230°C). The chocolate notes deepen " +
                "without crossing into bitterness. One of the most consistent Central American origins.\n\n" +
                "## Brew Pairings\nExcellent for espresso and Moka Pot. Also exceptional as a " +
                "French press — the body holds up well to the metal filter.", 4);

            seedArticle("Indonesian Mandheling", "🌿", "Beans",
                "## Origin\nFrom the Batak highlands of North Sumatra, Indonesia, around Lake Toba " +
                "at 1,100–1,600 m. Processed using the unique 'wet-hulling' (Giling Basah) method.\n\n" +
                "## Flavor Profile\n**Earthy, cedar, dark chocolate, tobacco, and mushroom** with a " +
                "very full body and low acidity. Syrupy mouthfeel.\n\n" +
                "## Roast\nTraditionally roasted **dark** (230–245°C). The wet-hulling process " +
                "creates the characteristic earthy funk and heavy body. Not for the light-roast enthusiast.\n\n" +
                "## Brew Pairings\nFrench press and Moka Pot maximize the body. Also well-suited " +
                "to cold brew — the earthiness mellows beautifully over 18 hours.", 4);

            seedArticle("Brazilian Cerrado Mineiro", "🌾", "Beans",
                "## Origin\nThe Cerrado Mineiro region in Minas Gerais state, Brazil, at 800–1,300 m. " +
                "A designated Geographic Indication zone with dry climate and flat terrain — " +
                "suited to mechanized harvesting and consistent naturals.\n\n" +
                "## Flavor Profile\n**Milk chocolate, hazelnut, caramel, and soft sweetness** " +
                "with very low acidity and a smooth, round body.\n\n" +
                "## Roast\n**Medium roast** (210–220°C) highlights sweetness. The low acidity makes " +
                "it forgiving at medium-dark too. Brazil is the world's largest coffee producer " +
                "and Cerrado is its flagship specialty region.\n\n" +
                "## Brew Pairings\nThe go-to espresso base for blends. Excellent for cold brew " +
                "due to natural sweetness. Great entry-point origin for new specialty drinkers.", 4);
        }

        // ── Ulaanbaatar Cafe Listings ─────────────────────────────────────────
        if (!repo.isCafesSeeded()) {
            seedCafe("Luna Blanca", "Peace Ave 15, Sukhbaatar District", "Sukhbaatar",
                "A beloved specialty coffee and brunch spot near the State Department Store. Known for single-origin pour overs and all-day breakfast.",
                "08:00–22:00", 47.9184, 106.9177);
            seedCafe("Nomads Coffee", "Olympic Street 12, Sukhbaatar District", "Sukhbaatar",
                "Local specialty roastery and café. Roasts their own beans in-house, with a rotating menu of Mongolian and imported single-origins.",
                "09:00–21:00", 47.9161, 106.9203);
            seedCafe("Café Amsterdam", "Baga Toiruu 6, Sukhbaatar District", "Sukhbaatar",
                "European-style café with fresh pastries, strong espresso, and a relaxed atmosphere. Popular with expats and students.",
                "08:30–21:30", 47.9175, 106.9155);
            seedCafe("Coffee Lab UB", "Seoul Street 34, Sukhbaatar District", "Sukhbaatar",
                "Specialty coffee training lab and tasting bar. Offers cupping sessions, barista courses, and a retail bean selection.",
                "10:00–20:00", 47.9143, 106.9220);
            seedCafe("Rocky Mountain Coffee", "Ulaanbaatar Hotel, Sukhbaatar District", "Sukhbaatar",
                "International specialty chain with consistent quality. Full espresso bar, seasonal drinks, and free Wi-Fi.",
                "07:00–22:00", 47.9203, 106.9244);
            seedCafe("Merkuri Coffee", "Tsagdaagiin Gudamj 8, Bayangol District", "Bayangol",
                "Minimalist pour-over focused café. Clean design, focused menu, and some of the best manual brew coffee in the city.",
                "09:00–20:00", 47.9098, 106.8901);
            seedCafe("Grand Coffee", "Narnii Road 22, Bayanzurkh District", "Bayanzurkh",
                "Large, comfortable café ideal for work and meetings. Extensive food menu alongside a full espresso bar.",
                "08:00–23:00", 47.9226, 106.9498);
            seedCafe("Espresso Yourself", "Zaisan Area, Khan-Uul District", "Khan-Uul",
                "Cozy neighborhood café in the Zaisan hills. Specialty roasts, homemade cakes, and a stunning view of the city.",
                "09:00–21:00", 47.8912, 106.9063);
        }

        // ── Coffee Drink Guide Article ────────────────────────────────────────
        if (!repo.isDrinkArticlesSeeded()) {
            seedArticle("Coffee Drink Guide", "☕", "Drinks",
                "## Espresso\n" +
                "A 25–30 ml concentrated shot extracted at 9 bar in 25–30 seconds. The foundation of most " +
                "café drinks. Intense, syrupy, with a golden-red crema. **Best bean:** Medium-dark or dark " +
                "roast (Brazilian, Guatemalan, or Italian blend).\n\n" +
                "## Americano\n" +
                "Espresso diluted with hot water (1:2–1:4). Maintains espresso flavor without the " +
                "concentration. **Milk ratio:** None. Often ordered black. Larger serving for those who " +
                "want espresso flavor in a longer drink.\n\n" +
                "## Latte\n" +
                "Espresso + steamed milk (150–200 ml) + thin microfoam layer. Ratio ~1:4 coffee to milk. " +
                "Creamy, mild, crowd-pleasing. **Best bean:** Medium roast (Colombian, Brazilian). " +
                "Canvas for latte art.\n\n" +
                "## Cappuccino\n" +
                "Equal thirds: espresso + steamed milk + thick milk foam. 150–180 ml total. " +
                "More intense than a latte, less sweet. Traditionally dry (more foam) or wet (more milk). " +
                "**Best bean:** Dark or medium-dark.\n\n" +
                "## Flat White\n" +
                "Double ristretto (short espresso) + 120 ml velvety microfoam — smaller and stronger than a latte. " +
                "Australian origin. **Best bean:** A blend or single origin with chocolate/nut notes.\n\n" +
                "## Cold Brew\n" +
                "Coffee steeped in cold water for 12–24 hours, then filtered. Smooth, naturally sweet, " +
                "low acidity. Serve over ice diluted 1:1. **Best bean:** Medium or dark roast with " +
                "chocolate and nut character (Brazilian Cerrado, Colombian).\n\n" +
                "## Pour Over\n" +
                "Manual filter brew using gravity. Produces a clean, bright, tea-like cup that highlights " +
                "delicate origin flavors. Methods: V60, Chemex, Kalita Wave. " +
                "**Best bean:** Light roast with floral/fruity notes (Ethiopian, Kenyan).\n\n" +
                "## French Press\n" +
                "Full-immersion brew with a metal mesh filter. Rich body, heavy mouthfeel, " +
                "more oils and texture than filter. 4-minute steep. **Best bean:** Medium-dark to dark " +
                "roast (Guatemalan, Indonesian Mandheling).", 6);
        }
    }

    private void seedGuide(String name, String icon, String desc, String diff, int time,
                            String params, String steps) {
        BrewGuide g = new BrewGuide();
        g.setMethodName(name);
        g.setIcon(icon);
        g.setDescription(desc);
        g.setDifficulty(diff);
        g.setBrewTimeMin(time);
        g.setParameters(params);
        g.setSteps(steps);
        repo.seedBrewGuide(g);
    }

    private void seedArticle(String title, String icon, String category, String content, int readTime) {
        LearnArticle a = new LearnArticle();
        a.setTitle(title);
        a.setIcon(icon);
        a.setCategory(category);
        a.setContent(content);
        a.setReadTimeMin(readTime);
        repo.seedLearnArticle(a);
    }

    private void seedCafe(String name, String address, String district,
                          String description, String hours, double lat, double lng) {
        CafeListing c = new CafeListing();
        c.setName(name);
        c.setAddress(address);
        c.setDistrict(district);
        c.setCity("Ulaanbaatar");
        c.setDescription(description);
        c.setHours(hours);
        c.setLat(lat);
        c.setLng(lng);
        c.setSubmittedBy(0);
        repo.createCafe(c);
    }

    // ── Equipment ─────────────────────────────────────────────────────────────

    public List<Equipment> getEquipment(int userId) {
        return repo.getEquipmentByUser(userId);
    }

    public Equipment addEquipment(int userId, String category, String name, String brand, String notes) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name is required");
        return repo.addEquipment(userId, category, name.trim(), brand, notes);
    }

    public void deleteEquipment(int equipmentId, int userId) {
        repo.deleteEquipment(equipmentId, userId);
    }

    // ── Saved Recipes ─────────────────────────────────────────────────────────

    public Map<String, Object> toggleSave(int userId, int recipeId) {
        boolean saved;
        if (repo.isRecipeSaved(userId, recipeId)) {
            repo.unsaveRecipe(userId, recipeId);
            saved = false;
        } else {
            repo.saveRecipe(userId, recipeId);
            saved = true;
        }
        return Map.of("saved", saved);
    }

    public List<Recipe> getSavedRecipes(int userId, int currentUserId) {
        return repo.getSavedRecipes(userId, currentUserId);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public List<Map<String, Object>> getNotifications(int userId) {
        return repo.getNotifications(userId, 50);
    }

    public void markNotificationsRead(int userId) {
        repo.markNotificationsRead(userId);
    }

    public Map<String, Object> getNotificationCount(int userId) {
        return Map.of("unread", repo.getUnreadNotificationCount(userId));
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    public void submitReport(int reporterId, String contentType, int contentId,
                              String reason, String description) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Reason is required");
        List<String> validTypes = List.of("recipe", "user", "comment");
        if (!validTypes.contains(contentType)) throw new IllegalArgumentException("Invalid content type");
        repo.createReport(reporterId, contentType, contentId, reason, description);
    }

    public List<Map<String, Object>> getReports(String status) {
        return repo.getAllReports(status);
    }

    public void resolveReport(int reportId, int adminId, String action) {
        List<String> valid = List.of("resolved", "dismissed");
        if (!valid.contains(action)) throw new IllegalArgumentException("Action must be 'resolved' or 'dismissed'");
        repo.resolveReport(reportId, adminId, action);
    }

    public Map<String, Object> getAdminStats() {
        return Map.of("pendingReports", repo.getPendingReportCount());
    }

    public boolean isAdmin(int userId) {
        if (userId == 1) return true;
        String adminEmail = System.getenv("ADMIN_EMAIL");
        if (adminEmail != null && !adminEmail.isBlank()) {
            return repo.findUserById(userId)
                .map(u -> adminEmail.equalsIgnoreCase(u.getEmail()))
                .orElse(false);
        }
        return false;
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    public void blockUser(int blockerId, int blockedId) {
        if (blockerId == blockedId) throw new IllegalArgumentException("Cannot block yourself");
        repo.blockUser(blockerId, blockedId);
    }

    public void unblockUser(int blockerId, int blockedId) {
        repo.unblockUser(blockerId, blockedId);
    }

    public boolean isBlocked(int blockerId, int blockedId) {
        return repo.isBlocked(blockerId, blockedId);
    }

    // ── Hashtags ──────────────────────────────────────────────────────────────

    public void followHashtag(int userId, String tag) {
        if (tag == null || tag.isBlank()) throw new IllegalArgumentException("Tag is required");
        repo.followHashtag(userId, tag.toLowerCase().replaceAll("[^a-z0-9_]", ""));
    }

    public void unfollowHashtag(int userId, String tag) {
        repo.unfollowHashtag(userId, tag.toLowerCase());
    }

    public List<String> getUserHashtags(int userId) {
        return repo.getUserHashtags(userId);
    }

    public List<Recipe> getHashtagRecipes(int currentUserId, String tag) {
        return repo.getRecipesByHashtag(currentUserId, tag);
    }

    public List<Map<String, Object>> getTrendingHashtags() {
        return repo.getTrendingHashtags(20);
    }

    // ── View mapping ──────────────────────────────────────────────────────────

    public record UserView(
        int id, String username, String email, String bio, String avatarUrl,
        String expertiseLevel, List<String> flavorPrefs,
        int followerCount, int followingCount, int recipeCount,
        boolean isFollowing, String createdAt
    ) {}

    private UserView toView(User u, int currentUserId, boolean isFollowing) {
        List<String> prefs = repo.getUserFlavorPrefs(u.getId());
        return new UserView(
            u.getId(), u.getUsername(), u.getEmail(),
            u.getBio(), u.getAvatarUrl(), u.getExpertiseLevel(),
            prefs,
            repo.getFollowerCount(u.getId()),
            repo.getFollowingCount(u.getId()),
            repo.getUserRecipeCount(u.getId()),
            isFollowing,
            u.getCreatedAt()
        );
    }
}
