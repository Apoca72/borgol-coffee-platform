package mn.edu.num.cafe.core.application;

import mn.edu.num.cafe.core.domain.*;
import mn.edu.num.cafe.infrastructure.persistence.BorgolRepository;
import mn.edu.num.cafe.infrastructure.security.JwtUtil;
import mn.edu.num.cafe.infrastructure.security.PasswordUtil;

import java.util.*;

/**
 * Main application service for the Borgol Coffee Enthusiast Platform.
 *
 * Handles: authentication, user management, recipes, cafes, social features.
 */
public class BorgolService {

    private final BorgolRepository repo;

    public BorgolService(BorgolRepository repo) {
        this.repo = repo;
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public record AuthResult(String token, UserView user) {}

    public AuthResult register(String username, String email, String password) {
        if (username == null || username.isBlank())  throw new IllegalArgumentException("Username is required");
        if (email == null    || email.isBlank())     throw new IllegalArgumentException("Email is required");
        if (password == null || password.length() < 6) throw new IllegalArgumentException("Password must be ≥ 6 characters");
        if (!email.contains("@"))                    throw new IllegalArgumentException("Invalid email address");
        if (username.length() < 3 || username.length() > 50) throw new IllegalArgumentException("Username must be 3–50 characters");
        if (!username.matches("[a-zA-Z0-9_]+"))     throw new IllegalArgumentException("Username: letters, numbers, underscores only");

        if (repo.findUserByEmail(email).isPresent())      throw new IllegalArgumentException("Email already registered");
        if (repo.findUserByUsername(username).isPresent()) throw new IllegalArgumentException("Username already taken");

        String hash = PasswordUtil.hash(password);
        User user   = repo.createUser(username, email, hash);
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    public AuthResult login(String email, String password) {
        if (email == null || email.isBlank())       throw new IllegalArgumentException("Email is required");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password is required");

        User user = repo.findUserByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!PasswordUtil.verify(password, user.getPasswordHash()))
            throw new IllegalArgumentException("Invalid email or password");

        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        return new AuthResult(token, toView(user, 0, false));
    }

    public UserView getMe(int userId) {
        User user = repo.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toView(user, 0, false);
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    public UserView getUserProfile(int targetId, int currentUserId) {
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

        return getMe(userId);
    }

    public void followUser(int followerId, int followingId) {
        if (followerId == followingId) throw new IllegalArgumentException("Cannot follow yourself");
        repo.followUser(followerId, followingId);
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

    // ── Recipes ───────────────────────────────────────────────────────────────

    public List<Recipe> getRecipes(int currentUserId, String search, String drinkType, String sort) {
        return repo.findAllRecipes(currentUserId, search, drinkType, sort);
    }

    public List<Recipe> getFeed(int userId) {
        return repo.getFeedRecipes(userId, 30);
    }

    public List<Recipe> getUserRecipes(int authorId, int currentUserId) {
        return repo.getUserRecipes(authorId, currentUserId);
    }

    public Recipe getRecipe(int id, int currentUserId) {
        return repo.findRecipeById(id, currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: id=" + id));
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
        return repo.updateRecipe(r);
    }

    public void deleteRecipe(int recipeId, int userId) {
        boolean deleted = repo.deleteRecipe(recipeId, userId);
        if (!deleted) throw new IllegalArgumentException("Recipe not found or not authorized");
    }

    public Map<String, Object> toggleLike(int userId, int recipeId) {
        boolean liked;
        if (repo.findRecipeById(recipeId, userId)
                .map(Recipe::isLikedByCurrentUser).orElse(false)) {
            repo.unlikeRecipe(userId, recipeId);
            liked = false;
        } else {
            repo.likeRecipe(userId, recipeId);
            liked = true;
            // notify author
            repo.findRecipeById(recipeId, userId).ifPresent(r -> {
                if (r.getAuthorId() != userId) {
                    repo.createNotification(r.getAuthorId(), "like", userId, recipeId,
                        "liked your recipe \"" + r.getTitle() + "\"");
                }
            });
        }
        int count = repo.findRecipeById(recipeId, userId)
            .map(Recipe::getLikesCount).orElse(0);
        return Map.of("liked", liked, "likesCount", count);
    }

    // ── Comments ──────────────────────────────────────────────────────────────

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

    // ── Cafes ─────────────────────────────────────────────────────────────────

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
     * Return cafes within {@code radiusKm} kilometres of (lat, lng).
     * Uses equirectangular bounding-box approximation — accurate enough for city-scale distances.
     */
    public List<CafeListing> getCafesNearby(int currentUserId, double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        return repo.findCafesNearby(currentUserId,
            lat - latDelta, lat + latDelta,
            lng - lngDelta, lng + lngDelta);
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
