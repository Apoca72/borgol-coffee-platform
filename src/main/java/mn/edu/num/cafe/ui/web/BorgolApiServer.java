package mn.edu.num.cafe.ui.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import mn.edu.num.cafe.core.application.BorgolService;
import mn.edu.num.cafe.core.application.MenuService;
import mn.edu.num.cafe.core.domain.MenuCategory;
import mn.edu.num.cafe.infrastructure.security.JwtUtil;

import java.util.List;
import java.util.Map;

/**
 * Borgol Coffee Enthusiast Platform — REST API server.
 *
 * API groups:
 *   POST /api/auth/register     — register new user
 *   POST /api/auth/login        — login, returns JWT
 *   GET  /api/auth/me           — get current user [auth]
 *
 *   GET  /api/users/{id}        — user profile
 *   PUT  /api/users/me          — update own profile [auth]
 *   POST /api/users/{id}/follow — follow user [auth]
 *   DELETE /api/users/{id}/follow — unfollow [auth]
 *   GET  /api/users/search      — search users
 *
 *   GET  /api/recipes           — list/search recipes
 *   GET  /api/recipes/{id}      — recipe detail
 *   POST /api/recipes           — create recipe [auth]
 *   PUT  /api/recipes/{id}      — update recipe [auth]
 *   DELETE /api/recipes/{id}    — delete recipe [auth]
 *   POST /api/recipes/{id}/like — toggle like [auth]
 *   GET  /api/recipes/{id}/comments  — get comments
 *   POST /api/recipes/{id}/comments  — add comment [auth]
 *
 *   GET  /api/feed              — personalized feed [auth]
 *
 *   GET  /api/cafes             — list/search cafes
 *   GET  /api/cafes/{id}        — cafe detail
 *   POST /api/cafes             — create cafe [auth]
 *   POST /api/cafes/{id}/rate   — rate cafe [auth]
 *
 *   GET  /api/menu              — menu items (legacy)
 *   POST /api/menu              — add menu item (legacy)
 *   PUT  /api/menu/{id}         — update menu item (legacy)
 *   DELETE /api/menu/{id}       — delete menu item (legacy)
 */
public class BorgolApiServer {

    private final BorgolService borgol;
    private final MenuService   menuService;
    private final Javalin       app;

    public BorgolApiServer(BorgolService borgol, MenuService menuService) {
        this.borgol      = borgol;
        this.menuService = menuService;
        this.app = Javalin.create(cfg -> {
            cfg.staticFiles.add("/public");
            cfg.showJavalinBanner = false;
        });
        registerRoutes();
    }

    public void start(int port) {
        app.start(port);
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════╗");
        System.out.println("  ║   Borgol Coffee Enthusiast Platform          ║");
        System.out.printf ("  ║   http://localhost:%-25d║%n", port);
        System.out.println("  ║   Press Ctrl+C to stop                       ║");
        System.out.println("  ╚══════════════════════════════════════════════╝");
    }

    // ── Route registration ────────────────────────────────────────────────────

    private void registerRoutes() {
        app.get("/", ctx -> ctx.redirect("/index.html"));

        // Auth
        app.post("/api/auth/register", this::register);
        app.post("/api/auth/login",    this::login);
        app.get ("/api/auth/me",       this::getMe);

        // Users
        app.get   ("/api/users",                  this::getAllUsers);
        app.get   ("/api/users/search",            this::searchUsers);
        app.get   ("/api/users/{id}",              this::getUserProfile);
        app.put   ("/api/users/me",                this::updateProfile);
        app.post  ("/api/users/{id}/follow",       this::followUser);
        app.delete("/api/users/{id}/follow",       this::unfollowUser);
        app.get   ("/api/users/{id}/recipes",      this::getUserRecipes);
        app.get   ("/api/users/{id}/liked",        this::getUserLiked);
        app.get   ("/api/users/{id}/following",    this::getUserFollowing);
        app.get   ("/api/users/{id}/followers",    this::getUserFollowers);

        // Recipes
        app.get   ("/api/recipes",               this::getRecipes);
        app.get   ("/api/recipes/{id}",          this::getRecipe);
        app.post  ("/api/recipes",               this::createRecipe);
        app.put   ("/api/recipes/{id}",          this::updateRecipe);
        app.delete("/api/recipes/{id}",          this::deleteRecipe);
        app.post  ("/api/recipes/{id}/like",     this::likeRecipe);
        app.get   ("/api/recipes/{id}/comments", this::getComments);
        app.post  ("/api/recipes/{id}/comments", this::addComment);

        // Feed
        app.get("/api/feed", this::getFeed);

        // Cafes
        app.get ("/api/cafes",               this::getCafes);
        app.get ("/api/cafes/nearby",        this::getCafesNearby);
        app.get ("/api/cafes/{id}",          this::getCafe);
        app.post("/api/cafes",               this::createCafe);
        app.post("/api/cafes/{id}/rate",     this::rateCafe);

        // Brew Journal
        app.get   ("/api/journal",      this::getJournal);
        app.post  ("/api/journal",      this::createJournalEntry);
        app.put   ("/api/journal/{id}", this::updateJournalEntry);
        app.delete("/api/journal/{id}", this::deleteJournalEntry);

        // Brew Guides
        app.get("/api/brew-guides",      this::getBrewGuides);
        app.get("/api/brew-guides/{id}", this::getBrewGuide);

        // Learn Articles
        app.get("/api/learn",      this::getLearnArticles);
        app.get("/api/learn/{id}", this::getLearnArticle);

        // Legacy menu API (kept for backward compatibility)
        app.get   ("/api/menu",      ctx -> ctx.json(menuService.getAllItems()));
        app.get   ("/api/menu/{id}", ctx -> {
            int id = intParam(ctx, "id");
            menuService.getItemById(id).ifPresentOrElse(ctx::json,
                () -> ctx.status(404).json(err("Item not found")));
        });
        app.post  ("/api/menu", ctx -> {
            MenuItemReq req = ctx.bodyAsClass(MenuItemReq.class);
            ctx.status(201).json(menuService.addItem(req.name,
                MenuCategory.valueOf(req.category), req.price, req.available));
        });
        app.put   ("/api/menu/{id}", ctx -> {
            MenuItemReq req = ctx.bodyAsClass(MenuItemReq.class);
            ctx.json(menuService.updateItem(intParam(ctx, "id"), req.name,
                MenuCategory.valueOf(req.category), req.price, req.available));
        });
        app.delete("/api/menu/{id}", ctx -> {
            menuService.removeItem(intParam(ctx, "id"));
            ctx.status(204);
        });
    }

    // ── Auth handlers ─────────────────────────────────────────────────────────

    private void register(Context ctx) {
        try {
            var req = ctx.bodyAsClass(AuthReq.class);
            var result = borgol.register(req.username, req.email, req.password);
            ctx.status(201).json(result);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void login(Context ctx) {
        try {
            var req = ctx.bodyAsClass(AuthReq.class);
            var result = borgol.login(req.email, req.password);
            ctx.json(result);
        } catch (IllegalArgumentException e) {
            ctx.status(401).json(err(e.getMessage()));
        }
    }

    private void getMe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getMe(userId));
    }

    // ── User handlers ─────────────────────────────────────────────────────────

    private void getUserProfile(Context ctx) {
        try {
            int targetId   = intParam(ctx, "id");
            int currentId  = authOptional(ctx);
            ctx.json(borgol.getUserProfile(targetId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void updateProfile(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(ProfileUpdateReq.class);
            ctx.json(borgol.updateProfile(userId, req.bio, req.avatarUrl,
                req.expertiseLevel, req.flavorPrefs));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void followUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            int targetId = intParam(ctx, "id");
            borgol.followUser(userId, targetId);
            ctx.json(Map.of("following", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void unfollowUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            int targetId = intParam(ctx, "id");
            borgol.unfollowUser(userId, targetId);
            ctx.json(Map.of("following", false));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void getAllUsers(Context ctx) {
        int currentId = authOptional(ctx);
        ctx.json(borgol.getAllUsers(currentId));
    }

    private void searchUsers(Context ctx) {
        String query  = ctx.queryParam("q");
        int currentId = authOptional(ctx);
        ctx.json(borgol.searchUsers(query, currentId));
    }

    private void getUserLiked(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getLikedRecipes(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getUserFollowing(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getFollowingUsers(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getUserFollowers(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getFollowerUsers(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getUserRecipes(Context ctx) {
        try {
            int authorId  = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getUserRecipes(authorId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Recipe handlers ───────────────────────────────────────────────────────

    private void getRecipes(Context ctx) {
        int    currentId = authOptional(ctx);
        String search    = ctx.queryParam("search");
        String drinkType = ctx.queryParam("drinkType");
        String sort      = ctx.queryParam("sort");
        ctx.json(borgol.getRecipes(currentId, search, drinkType, sort));
    }

    private void getRecipe(Context ctx) {
        try {
            int id        = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getRecipe(id, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void createRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(RecipeReq.class);
            ctx.status(201).json(borgol.createRecipe(userId, req.title, req.description,
                req.drinkType, req.ingredients, req.instructions,
                req.brewTime, req.difficulty, req.flavorTags, req.imageUrl));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void updateRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            int id  = intParam(ctx, "id");
            var req = ctx.bodyAsClass(RecipeReq.class);
            ctx.json(borgol.updateRecipe(id, userId, req.title, req.description,
                req.drinkType, req.ingredients, req.instructions,
                req.brewTime, req.difficulty, req.flavorTags, req.imageUrl));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            borgol.deleteRecipe(intParam(ctx, "id"), userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) {
            ctx.status(403).json(err(e.getMessage()));
        }
    }

    private void likeRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            ctx.json(borgol.toggleLike(userId, intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getComments(Context ctx) {
        ctx.json(borgol.getComments(intParam(ctx, "id")));
    }

    private void addComment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(CommentReq.class);
            ctx.status(201).json(borgol.addComment(intParam(ctx, "id"), userId, req.content));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // ── Feed handler ──────────────────────────────────────────────────────────

    private void getFeed(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getFeed(userId));
    }

    // ── Cafe handlers ─────────────────────────────────────────────────────────

    private void getCafes(Context ctx) {
        int    currentId = authOptional(ctx);
        String search    = ctx.queryParam("search");
        String district  = ctx.queryParam("district");
        ctx.json(borgol.getCafes(currentId, search, district));
    }

    /** GET /api/cafes/nearby?lat=X&lng=Y&radius=10 */
    private void getCafesNearby(Context ctx) {
        int currentId = authOptional(ctx);
        try {
            double lat    = Double.parseDouble(ctx.queryParamAsClass("lat",    String.class).get());
            double lng    = Double.parseDouble(ctx.queryParamAsClass("lng",    String.class).get());
            double radius = Double.parseDouble(ctx.queryParamAsClass("radius", String.class).getOrDefault("10"));
            ctx.json(borgol.getCafesNearby(currentId, lat, lng, radius));
        } catch (NumberFormatException | NullPointerException e) {
            ctx.status(400).json(err("lat and lng are required numeric query parameters"));
        }
    }

    private void getCafe(Context ctx) {
        try {
            int id        = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(borgol.getCafe(id, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void createCafe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(CafeReq.class);
            ctx.status(201).json(borgol.createCafe(userId, req.name, req.address,
                req.district, req.city, req.phone, req.description, req.hours, req.imageUrl));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void rateCafe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(RateReq.class);
            ctx.json(borgol.rateCafe(userId, intParam(ctx, "id"), req.rating, req.review));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // ── Brew Journal handlers ─────────────────────────────────────────────────

    private void getJournal(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getJournalEntries(userId));
    }

    private void createJournalEntry(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(JournalReq.class);
            ctx.status(201).json(borgol.createJournalEntry(userId, req.coffeeBean, req.origin,
                req.roastLevel, req.brewMethod, req.grindSize, req.waterTempC,
                req.doseGrams, req.yieldGrams, req.brewTimeSec,
                req.ratingAroma, req.ratingFlavor, req.ratingAcidity,
                req.ratingBody, req.ratingSweetness, req.ratingFinish, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void updateJournalEntry(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            int id  = intParam(ctx, "id");
            var req = ctx.bodyAsClass(JournalReq.class);
            ctx.json(borgol.updateJournalEntry(id, userId, req.coffeeBean, req.origin,
                req.roastLevel, req.brewMethod, req.grindSize, req.waterTempC,
                req.doseGrams, req.yieldGrams, req.brewTimeSec,
                req.ratingAroma, req.ratingFlavor, req.ratingAcidity,
                req.ratingBody, req.ratingSweetness, req.ratingFinish, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteJournalEntry(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            borgol.deleteJournalEntry(intParam(ctx, "id"), userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) {
            ctx.status(403).json(err(e.getMessage()));
        }
    }

    // ── Brew Guide handlers ───────────────────────────────────────────────────

    private void getBrewGuides(Context ctx) {
        ctx.json(borgol.getBrewGuides());
    }

    private void getBrewGuide(Context ctx) {
        try {
            ctx.json(borgol.getBrewGuide(intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Learn Article handlers ────────────────────────────────────────────────

    private void getLearnArticles(Context ctx) {
        ctx.json(borgol.getLearnArticles());
    }

    private void getLearnArticle(Context ctx) {
        try {
            ctx.json(borgol.getLearnArticle(intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /** Returns userId or sends 401 and returns null. */
    private Integer authRequired(Context ctx) {
        Integer userId = JwtUtil.getUserId(ctx.header("Authorization"));
        if (userId == null) {
            ctx.status(401).json(err("Authentication required"));
            return null;
        }
        return userId;
    }

    /** Returns userId or 0 if not authenticated (public endpoints). */
    private int authOptional(Context ctx) {
        Integer userId = JwtUtil.getUserId(ctx.header("Authorization"));
        return userId != null ? userId : 0;
    }

    private int intParam(Context ctx, String name) {
        try {
            return Integer.parseInt(ctx.pathParam(name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID: " + ctx.pathParam(name));
        }
    }

    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }

    // ── Request POJOs ─────────────────────────────────────────────────────────

    public static class AuthReq {
        public String username;
        public String email;
        public String password;
    }

    public static class ProfileUpdateReq {
        public String       bio;
        public String       avatarUrl;
        public String       expertiseLevel;
        public List<String> flavorPrefs;
    }

    public static class RecipeReq {
        public String       title;
        public String       description;
        public String       drinkType    = "COFFEE";
        public String       ingredients;
        public String       instructions;
        public int          brewTime;
        public String       difficulty   = "MEDIUM";
        public List<String> flavorTags;
        public String       imageUrl;
    }

    public static class CommentReq {
        public String content;
    }

    public static class CafeReq {
        public String name;
        public String address;
        public String district;
        public String city        = "Ulaanbaatar";
        public String phone;
        public String description;
        public String hours;
        public String imageUrl;
    }

    public static class RateReq {
        public int    rating;
        public String review;
    }

    public static class MenuItemReq {
        public String  name;
        public String  category  = "COFFEE";
        public double  price;
        public boolean available = true;
    }

    public static class JournalReq {
        public String coffeeBean  = "";
        public String origin      = "";
        public String roastLevel  = "";
        public String brewMethod  = "";
        public String grindSize   = "";
        public int    waterTempC  = 93;
        public double doseGrams   = 18;
        public double yieldGrams  = 36;
        public int    brewTimeSec = 0;
        public int    ratingAroma     = 5;
        public int    ratingFlavor    = 5;
        public int    ratingAcidity   = 5;
        public int    ratingBody      = 5;
        public int    ratingSweetness = 5;
        public int    ratingFinish    = 5;
        public String notes       = "";
    }
}
