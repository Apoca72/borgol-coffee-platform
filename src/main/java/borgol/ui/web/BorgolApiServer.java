package borgol.ui.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import borgol.core.application.BorgolService;
import borgol.core.application.MenuService;
import borgol.core.domain.MenuCategory;
import borgol.infrastructure.messaging.RedisEventBus;
import borgol.infrastructure.security.SoapAuthClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Borgol платформын REST API сервер — Javalin дээр суурилсан.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Front Controller (GoF) + Facade
 * ════════════════════════════════════════════════════════════
 * Зорилго: Бүх HTTP хүсэлтийг нэг цэгт хүлээн авч, холбогдох
 * handler руу чиглүүлнэ. BorgolService-г "facade" болгон ашиглана —
 * API handler нь бизнес логикийг мэдэхгүй, зөвхөн дамжуулна.
 *
 * Зарчим (SOLID):
 *  - Single Responsibility: Энэ класс зөвхөн HTTP хүсэлт/хариу
 *    боловсруулах үүрэгтэй.
 *  - Dependency Inversion: BorgolService-г конструктороор хүлээн авна.
 *
 * SOA (Service-Oriented Architecture):
 *  Frontend → JSON REST Service (энэ) → SOAP Auth Service
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

    private final BorgolService  borgol;       // бизнесийн логик
    private final MenuService    menuService;  // legacy цэсний сервис
    private final Javalin        app;          // Javalin/Jetty HTTP сервер
    private final ApiGateway     gateway;      // нэвтрэлт, хурд хязгаарлалт, CORS
    private final RedisEventBus  eventBus;     // Pub/Sub SSE мэдэгдэл
    // SOAP клиент — SOAP бүртгэл/нэвтрэх endpoint-уудад ашиглана
    private final SoapAuthClient soapClient = new SoapAuthClient();

    public BorgolApiServer(BorgolService borgol, MenuService menuService,
                           ApiGateway gateway, RedisEventBus eventBus) {
        this.borgol      = borgol;
        this.menuService = menuService;
        this.gateway     = gateway;
        this.eventBus    = eventBus;
        this.app = Javalin.create(cfg -> {
            cfg.staticFiles.add("/public");
            cfg.showJavalinBanner = false;
            cfg.jetty.modifyServletContextHandler(h ->
                h.setMaxFormContentSize(8 * 1024 * 1024));
        });
        // CORS, rate limiting, audit log — ApiGateway дамжуулна (architectural boundary)
        gateway.registerFilters(app);
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

    // ── Маршрут бүртгэл ───────────────────────────────────────────────────────
    // Загвар: Front Controller — бүх URL маршрутыг нэг газарт бүртгэнэ.
    // Method reference (this::handler) → Lambda-г богиносгосон хэлбэр.

    private void registerRoutes() {
        // Үндсэн хуудас руу дахин чиглүүлнэ
        app.get("/", ctx -> ctx.redirect("/index.html"));
        // Railway health check — deployment амьд эсэхийг шалгах
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        // ── Дотоод auth (SOAP унасан үед fallback болно) ────────────────────
        app.post("/api/auth/register", this::register);
        app.post("/api/auth/login",    this::login);
        app.get ("/api/auth/me",       this::getMe);

        // ── Lab 06: SOA — SOAP Auth Service-р дамжуулах endpoint-ууд ────────
        // SOA загвар: JSON сервис нь SOAP сервист чиглүүлж, бие даасан
        // auth микросервис ажиллана. Хэрэв SOAP унасан бол local auth руу унана.
        app.post("/api/soap/register", this::soapRegister);
        app.post("/api/soap/login",    this::soapLogin);

        // Users
        app.get   ("/api/users",                  this::getAllUsers);
        app.get   ("/api/users/search",            this::searchUsers);
        app.get   ("/api/users/{id}",              this::getUserProfile);
        app.put   ("/api/users/me",                this::updateProfile);
        app.delete("/api/users/{id}",              this::deleteUser);     // Lab 06 – DELETE profile
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

        // Equipment
        app.get   ("/api/equipment",      this::getEquipment);
        app.post  ("/api/equipment",      this::addEquipment);
        app.delete("/api/equipment/{id}", this::deleteEquipment);

        // Brew Guides
        app.get("/api/brew-guides",      this::getBrewGuides);
        app.get("/api/brew-guides/{id}", this::getBrewGuide);

        // Learn Articles
        app.get("/api/learn",      this::getLearnArticles);
        app.get("/api/learn/{id}", this::getLearnArticle);

        // Saved recipes
        app.post("/api/recipes/{id}/save",   this::saveRecipe);
        app.get ("/api/saved",               this::getSaved);

        // Notifications
        app.get ("/api/notifications",              this::getNotifications);
        app.get ("/api/notifications/count",        this::getNotificationCount);
        app.post("/api/notifications/read",         this::markNotificationsRead);
        // SSE stream — Redis Pub/Sub-аар бодит цагт мэдэгдэл хүлээн авна
        app.get ("/api/notifications/stream",       this::notificationStream);

        // Reports
        app.post("/api/report", this::submitReport);

        // ── Admin панел — зөвхөн id=1 хэрэглэгч хандах боломжтой ───────────
        // Зарчим: Least Privilege — admin эрх зөвхөн шаардлагатай endpoint-д
        // isAdmin() шалгалт handler дотор хийгдэнэ → 403 Forbidden буцаана
        app.get  ("/api/admin/reports",           this::adminGetReports);
        app.post ("/api/admin/reports/{id}/resolve", this::adminResolveReport);
        app.get  ("/api/admin/stats",             this::adminGetStats);

        // ── Bean AI — Google Gemini 1.5 Flash-д суурилсан чат туслагч ───────
        // Server-Sent Events (SSE) → хариуг үг бүрээр дамжуулна (streaming)
        app.post("/api/bean/chat", this::beanChat);

        // Block
        app.post  ("/api/users/{id}/block", this::blockUser);
        app.delete("/api/users/{id}/block", this::unblockUser);

        // Hashtags
        app.get   ("/api/hashtags/trending",          this::getTrendingHashtags);
        app.get   ("/api/hashtags/{tag}/recipes",     this::getHashtagRecipes);
        app.post  ("/api/hashtags/{tag}/follow",      this::followHashtag);
        app.delete("/api/hashtags/{tag}/follow",      this::unfollowHashtag);
        app.get   ("/api/users/me/hashtags",          this::getUserHashtags);

        // ── Legacy цэсний API — backward compatibility ───────────────────────
        // JavaFX desktop app-тай нийцэх зорилгоор хадгалсан
        // Шинэ код нэмэхгүй — Open/Closed зарчим: нэмэх боломжтой, хасахгүй
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

    // ── Lab 06: SOAP-р дамжуулах auth handler-ууд ────────────────────────────
    // Загвар: Strategy + Fallback Chain
    // SOAP амжилттай → SOAP токен ашиглана
    // SOAP унасан → local JWT-р орлуулна (graceful degradation)

    /**
     * POST /api/soap/register
     *
     * SOA урсгал:
     *   1. Frontend → JSON сервис (энэ) HTTP/JSON
     *   2. JSON сервис → SOAP сервис XML/HTTP (RegisterUser)
     *   3. SOAP амжилттай бол → local DB-д профайл үүсгэнэ
     *   4. SOAP токен (эсвэл local JWT) frontend-д буцаана
     */
    private void soapRegister(Context ctx) {
        var req = ctx.bodyAsClass(AuthReq.class);

        // ── SOAP сервист бүртгэлийн хүсэлт илгээнэ ──────────────────────────
        SoapAuthClient.AuthResult soapResult =
                soapClient.register(req.username, req.email, req.password);

        // SOAP сервис унасан эсэхийг мессежээр тодорхойлно
        boolean soapDown = soapResult.message() != null &&
                           soapResult.message().startsWith("SOAP service unavailable");

        if (!soapResult.success() && !soapDown) {
            // SOAP ажиллаж байгаа ч татгалзсан (жишээ: нэр давхардсан)
            ctx.status(400).json(err(soapResult.message()));
            return;
        }

        // ── Дотоод DB-д профайл үүсгэнэ (SOAP унасан үед fallback) ──────────
        // Зарчим: Defensive Programming — нэг сервис унасан ч систем ажиллана
        try {
            var profile = borgol.register(req.username, req.email, req.password);
            ctx.status(201).json(Map.of(
                "token",    soapResult.token() != null ? soapResult.token() : profile.token(),
                "userId",   profile.user().id(),
                "username", req.username,
                "message",  soapDown ? "Registered (local auth)" : "Registered via SOAP Authentication Service"
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    /**
     * POST /api/soap/login
     *
     * SOA flow:
     *   1. Frontend sends credentials to JSON Service
     *   2. JSON Service forwards to SOAP LoginUser
     *   3. SOAP generates JWT token → returned to frontend
     *   4. Frontend stores token → uses it for all subsequent JSON Service calls
     */
    private void soapLogin(Context ctx) {
        var req = ctx.bodyAsClass(AuthReq.class);

        // Delegate to SOAP service
        SoapAuthClient.AuthResult soapResult =
                soapClient.login(req.email, req.password);

        if (!soapResult.success()) {
            // SOAP failed – try local auth as fallback
            try {
                var localResult = borgol.login(req.email, req.password);
                ctx.json(Map.of(
                    "token",    localResult.token(),
                    "userId",   localResult.user().id(),
                    "username", localResult.user().username(),
                    "source",   "local-fallback"
                ));
            } catch (IllegalArgumentException e) {
                ctx.status(401).json(err(soapResult.message()));
            }
            return;
        }

        ctx.json(Map.of(
            "token",    soapResult.token(),
            "userId",   soapResult.userId()   != null ? soapResult.userId()   : 0,
            "username", soapResult.username() != null ? soapResult.username() : "",
            "source",   "soap-auth-service"
        ));
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
        var user = borgol.getMe(userId);
        var resp = new java.util.HashMap<String, Object>();
        resp.put("id",             user.id());
        resp.put("username",       user.username());
        resp.put("email",          user.email());
        resp.put("bio",            user.bio());
        resp.put("avatarUrl",      user.avatarUrl());
        resp.put("expertiseLevel", user.expertiseLevel());
        resp.put("flavorPrefs",    user.flavorPrefs());
        resp.put("followerCount",  user.followerCount());
        resp.put("followingCount", user.followingCount());
        resp.put("recipeCount",    user.recipeCount());
        resp.put("isFollowing",    user.isFollowing());
        resp.put("createdAt",      user.createdAt());
        resp.put("isAdmin",        borgol.isAdmin(userId));
        ctx.json(resp);
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

    /**
     * DELETE /api/users/:id   [auth required]
     *
     * Lab 06 – User Profile CRUD: Delete profile.
     * A user may only delete their own account.
     */
    private void deleteUser(Context ctx) {
        Integer requesterId = authRequired(ctx);
        if (requesterId == null) return;
        try {
            int targetId = intParam(ctx, "id");
            if (requesterId != targetId && !borgol.isAdmin(requesterId)) {
                ctx.status(403).json(err("Not authorized"));
                return;
            }
            borgol.deleteUser(targetId);
            ctx.status(204);
        } catch (Exception e) {
            ctx.status(500).json(err(e.getMessage()));
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

    // ── Equipment handlers ────────────────────────────────────────────────────

    private void getEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getEquipment(userId));
    }

    private void addEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(EquipmentReq.class);
            ctx.status(201).json(borgol.addEquipment(userId, req.category, req.name, req.brand, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        borgol.deleteEquipment(intParam(ctx, "id"), userId);
        ctx.status(204);
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

    // ── Save handlers ─────────────────────────────────────────────────────────

    private void saveRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.toggleSave(userId, intParam(ctx, "id")));
    }

    private void getSaved(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getSavedRecipes(userId, userId));
    }

    // ── Notification handlers ─────────────────────────────────────────────────

    private void getNotifications(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getNotifications(userId));
    }

    private void getNotificationCount(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getNotificationCount(userId));
    }

    private void markNotificationsRead(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        borgol.markNotificationsRead(userId);
        ctx.json(Map.of("ok", true));
    }

    /**
     * SSE endpoint — Redis Pub/Sub-аар бодит цагт мэдэгдэл дамжуулна.
     *
     * Загвар: Observer (push) — polling-г SSE-р солино.
     * EventSource token query param fallback:
     *   EventSource браузерын API custom header дэмждэггүй тул
     *   ?token=xxx query param-аар JWT дамжуулна (ApiGateway дэмжинэ).
     *
     * Heartbeat (25с): proxy timeout-оос хамгаалах тулд хоосон comment
     * илгээнэ — SSE протоколд ": ...\n\n" comment гэж тооцогдоно.
     */
    private void notificationStream(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;

        ctx.contentType("text/event-stream");
        ctx.header("Cache-Control",  "no-cache");
        ctx.header("Connection",     "keep-alive");
        ctx.header("X-Accel-Buffering", "no"); // Nginx buffering-г унтраана

        try {
            PrintWriter writer = ctx.res().getWriter();
            final int uid = userId;

            Consumer<String> handler = event -> {
                try {
                    writer.write("data: " + event + "\n\n");
                    writer.flush();
                } catch (Exception ignored) { /* client disconnected */ }
            };

            eventBus.subscribe(uid, handler);
            try {
                // 25 секунд тутам heartbeat — proxy timeout хамгаалалт
                while (!writer.checkError()) {
                    writer.write(": heartbeat\n\n");
                    writer.flush();
                    Thread.sleep(25_000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                eventBus.unsubscribe(uid, handler);
            }
        } catch (Exception e) {
            ctx.status(500).json(err("SSE stream error"));
        }
    }

    // ── Report handler ────────────────────────────────────────────────────────

    private void submitReport(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(ReportReq.class);
            borgol.submitReport(userId, req.contentType, req.contentId, req.reason, req.description);
            ctx.status(201).json(Map.of("message", "Report submitted"));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // ── Admin handlers ────────────────────────────────────────────────────────

    private void adminGetReports(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!borgol.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        String status = ctx.queryParam("status") != null ? ctx.queryParam("status") : "pending";
        ctx.json(borgol.getReports(status));
    }

    private void adminResolveReport(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!borgol.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        try {
            var req = ctx.bodyAsClass(ResolveReq.class);
            borgol.resolveReport(intParam(ctx, "id"), userId, req.action);
            ctx.json(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void adminGetStats(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!borgol.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        ctx.json(borgol.getAdminStats());
    }

    // ── Bean — AI coffee assistant (Google Gemini) ───────────────────────────

    private static final String BEAN_SYSTEM =
        "You are Bean, a warm and knowledgeable AI coffee assistant for Borgol — " +
        "a community platform for coffee enthusiasts in Ulaanbaatar, Mongolia. " +
        "You help users with: coffee recipes, brew ratios, grind sizes, water temperatures, " +
        "brew methods (pour-over, espresso, AeroPress, French press, cold brew, Moka pot), " +
        "flavor profiles, bean origins, roast levels, and troubleshooting brews. " +
        "Keep answers concise, friendly, and practical. Use ☕ sparingly for warmth. " +
        "Respond in whatever language the user writes in.";

    private static final ObjectMapper BEAN_MAPPER = new ObjectMapper();

    public static class BeanMessage {
        public String role;    // "user" or "assistant"
        public String content;
    }
    public static class BeanReq {
        public List<BeanMessage> messages;
    }

    private void beanChat(Context ctx) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            ctx.status(503).json(err("Bean is not configured (missing GEMINI_API_KEY)"));
            return;
        }

        BeanReq req;
        try {
            req = ctx.bodyAsClass(BeanReq.class);
        } catch (Exception e) {
            ctx.status(400).json(err("Invalid request body"));
            return;
        }
        if (req.messages == null || req.messages.isEmpty()) {
            ctx.status(400).json(err("messages required"));
            return;
        }

        // Build Gemini request JSON
        // Gemini uses "model" for assistant role
        try {
            ObjectNode body = BEAN_MAPPER.createObjectNode();

            // system_instruction
            ObjectNode sysInstr = body.putObject("system_instruction");
            ArrayNode sysParts = sysInstr.putArray("parts");
            sysParts.addObject().put("text", BEAN_SYSTEM);

            // contents (conversation history)
            ArrayNode contents = body.putArray("contents");
            for (BeanMessage m : req.messages) {
                String geminiRole = "assistant".equals(m.role) ? "model" : "user";
                ObjectNode turn = contents.addObject();
                turn.put("role", geminiRole);
                ArrayNode parts = turn.putArray("parts");
                parts.addObject().put("text", m.content);
            }

            // generationConfig
            body.putObject("generationConfig").put("maxOutputTokens", 1024);

            String bodyJson = BEAN_MAPPER.writeValueAsString(body);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                         "gemini-1.5-flash:streamGenerateContent?key=" + apiKey + "&alt=sse";

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

            // Stream SSE back to browser
            ctx.contentType("text/event-stream");
            ctx.header("Cache-Control", "no-cache");
            ctx.header("X-Accel-Buffering", "no");

            HttpResponse<java.io.InputStream> response = http.send(httpReq,
                HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) continue;
                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;

                    try {
                        JsonNode node = BEAN_MAPPER.readTree(data);
                        JsonNode text = node.path("candidates").path(0)
                            .path("content").path("parts").path(0).path("text");
                        if (!text.isMissingNode() && !text.isNull()) {
                            String chunk = text.asText();
                            ctx.outputStream().write(
                                ("data: " + escJson(chunk) + "\n\n")
                                    .getBytes(StandardCharsets.UTF_8));
                            ctx.outputStream().flush();
                        }
                    } catch (Exception ignored) {}
                }
            }

            ctx.outputStream().write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
            ctx.outputStream().flush();

        } catch (Exception e) {
            try {
                ctx.outputStream().write(
                    ("data: " + escJson("[ERROR] " + e.getMessage()) + "\n\n")
                        .getBytes(StandardCharsets.UTF_8));
                ctx.outputStream().flush();
            } catch (Exception ignored) {}
        }
    }

    /** Escape a string for embedding as a JSON string value (no outer quotes). */
    private static String escJson(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    // ── Block handlers ────────────────────────────────────────────────────────

    private void blockUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            borgol.blockUser(userId, intParam(ctx, "id"));
            ctx.json(Map.of("blocked", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void unblockUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        borgol.unblockUser(userId, intParam(ctx, "id"));
        ctx.json(Map.of("blocked", false));
    }

    // ── Hashtag handlers ──────────────────────────────────────────────────────

    private void getTrendingHashtags(Context ctx) {
        ctx.json(borgol.getTrendingHashtags());
    }

    private void getHashtagRecipes(Context ctx) {
        int currentId = authOptional(ctx);
        ctx.json(borgol.getHashtagRecipes(currentId, ctx.pathParam("tag")));
    }

    private void followHashtag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            borgol.followHashtag(userId, ctx.pathParam("tag"));
            ctx.json(Map.of("following", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void unfollowHashtag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        borgol.unfollowHashtag(userId, ctx.pathParam("tag"));
        ctx.json(Map.of("following", false));
    }

    private void getUserHashtags(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(borgol.getUserHashtags(userId));
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    /**
     * Lab 06 – Authentication Middleware with SOAP delegation.
     *
     * SOA flow:
     *   1. Extract Bearer token from Authorization header
     *   2. Call SOAP ValidateToken (JSON Service → SOAP Service)
     *   3. If SOAP says valid → allow request, use SOAP-provided userId
     *   4. If SOAP unavailable → fall back to local JWT verification
     *   5. If both fail → return 401 Unauthorized
     *
     * Returns userId or sends 401 and returns null.
     * Delegates to ApiGateway — auth logic is "inside the gateway subnet".
     */
    private Integer authRequired(Context ctx) {
        return gateway.authenticate(ctx, true);
    }

    /**
     * Returns userId or 0 if not authenticated (public endpoints).
     * Delegates to ApiGateway.
     */
    private int authOptional(Context ctx) {
        Integer id = gateway.authenticate(ctx, false);
        return id != null ? id : 0;
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

    public static class EquipmentReq {
        public String category = "OTHER";
        public String name     = "";
        public String brand    = "";
        public String notes    = "";
    }

    public static class ReportReq {
        public String contentType;
        public int    contentId;
        public String reason;
        public String description;
    }

    public static class ResolveReq {
        public String action; // "resolved" or "dismissed"
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
