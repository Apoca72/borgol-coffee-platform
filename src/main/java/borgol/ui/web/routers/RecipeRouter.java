package borgol.ui.web.routers;

import borgol.core.application.RecipeService;
import borgol.core.application.UserService;
import borgol.ui.web.ApiGateway;
import borgol.ui.web.dto.CollectionReq;
import borgol.ui.web.dto.CollectionRecipeReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class RecipeRouter {

    private final RecipeService svc;
    private final UserService   userSvc;
    private final ApiGateway    gateway;

    public RecipeRouter(RecipeService svc, UserService userSvc, ApiGateway gateway) {
        this.svc     = svc;
        this.userSvc = userSvc;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
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

        // Saved recipes
        app.post("/api/recipes/{id}/save",   this::saveRecipe);
        app.get ("/api/saved",               this::getSaved);

        // User recipe views (delegated from UserRouter for convenience)
        app.get("/api/users/{id}/recipes",      this::getUserRecipes);
        app.get("/api/users/{id}/liked",        this::getUserLiked);

        // Hashtags (recipe-related)
        app.get("/api/hashtags/trending",          this::getTrendingHashtags);
        app.get("/api/hashtags/{tag}/recipes",     this::getHashtagRecipes);

        // Reports
        app.post("/api/report", this::submitReport);

        // Admin
        app.get  ("/api/admin/reports",              this::adminGetReports);
        app.post ("/api/admin/reports/{id}/resolve", this::adminResolveReport);
        app.get  ("/api/admin/stats",                this::adminGetStats);

        // Collections
        app.get   ("/api/collections",                              this::getCollections);
        app.post  ("/api/collections",                              this::createCollection);
        app.delete("/api/collections/{id}",                         this::deleteCollection);
        app.get   ("/api/collections/{id}/recipes",                 this::getCollectionRecipes);
        app.post  ("/api/collections/{id}/recipes",                 this::addRecipeToCollection);
        app.delete("/api/collections/{id}/recipes/{recipeId}",      this::removeRecipeFromCollection);

        // Equipment
        app.get   ("/api/equipment",      this::getEquipment);
        app.post  ("/api/equipment",      this::addEquipment);
        app.delete("/api/equipment/{id}", this::deleteEquipment);
    }

    // ── Recipe handlers ──────────────────────────────────────────────────────

    private void getRecipes(Context ctx) {
        int    currentId = authOptional(ctx);
        String search    = ctx.queryParam("search");
        String drinkType = ctx.queryParam("drinkType");
        String sort      = ctx.queryParam("sort");
        ctx.json(svc.getRecipes(currentId, search, drinkType, sort));
    }

    private void getRecipe(Context ctx) {
        try {
            int id        = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getRecipe(id, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void createRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.RecipeReq.class);
            ctx.status(201).json(svc.createRecipe(userId, req.title, req.description,
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
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.RecipeReq.class);
            ctx.json(svc.updateRecipe(id, userId, req.title, req.description,
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
            svc.deleteRecipe(intParam(ctx, "id"), userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) {
            ctx.status(403).json(err(e.getMessage()));
        }
    }

    private void likeRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            ctx.json(svc.toggleLike(userId, intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getComments(Context ctx) {
        ctx.json(svc.getComments(intParam(ctx, "id")));
    }

    private void addComment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.CommentReq.class);
            ctx.status(201).json(svc.addComment(intParam(ctx, "id"), userId, req.content));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // ── Feed ─────────────────────────────────────────────────────────────────

    private void getFeed(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getFeed(userId));
    }

    // ── Save handlers ────────────────────────────────────────────────────────

    private void saveRecipe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.toggleSave(userId, intParam(ctx, "id")));
    }

    private void getSaved(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getSavedRecipes(userId, userId));
    }

    // ── User recipe views ────────────────────────────────────────────────────

    private void getUserRecipes(Context ctx) {
        try {
            int authorId  = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getUserRecipes(authorId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getUserLiked(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getLikedRecipes(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Hashtag handlers ─────────────────────────────────────────────────────

    private void getTrendingHashtags(Context ctx) {
        ctx.json(svc.getTrendingHashtags());
    }

    private void getHashtagRecipes(Context ctx) {
        int currentId = authOptional(ctx);
        ctx.json(svc.getHashtagRecipes(currentId, ctx.pathParam("tag")));
    }

    // ── Report handlers ──────────────────────────────────────────────────────

    private void submitReport(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.ReportReq.class);
            svc.submitReport(userId, req.contentType, req.contentId, req.reason, req.description);
            ctx.status(201).json(Map.of("message", "Report submitted"));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    // ── Admin handlers ───────────────────────────────────────────────────────

    private void adminGetReports(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!userSvc.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        String status = ctx.queryParam("status") != null ? ctx.queryParam("status") : "pending";
        ctx.json(svc.getReports(status));
    }

    private void adminResolveReport(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!userSvc.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.ResolveReq.class);
            svc.resolveReport(intParam(ctx, "id"), userId, req.action);
            ctx.json(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void adminGetStats(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        if (!userSvc.isAdmin(userId)) { ctx.status(403).json(err("Admin access required")); return; }
        ctx.json(svc.getAdminStats());
    }

    // ── Collection handlers ──────────────────────────────────────────────────

    private void getCollections(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getCollections(userId));
    }

    private void createCollection(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(CollectionReq.class);
            ctx.status(201).json(svc.createCollection(userId, req.name, req.description, req.isPublic));
        } catch (IllegalArgumentException e) { ctx.status(400).json(err(e.getMessage())); }
    }

    private void deleteCollection(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.deleteCollection(intParam(ctx, "id"), userId);
        ctx.status(204);
    }

    private void getCollectionRecipes(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getCollectionRecipes(intParam(ctx, "id")));
    }

    private void addRecipeToCollection(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(CollectionRecipeReq.class);
            svc.addRecipeToCollection(intParam(ctx, "id"), req.recipeId, userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) { ctx.status(404).json(err(e.getMessage())); }
    }

    private void removeRecipeFromCollection(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            svc.removeRecipeFromCollection(intParam(ctx, "id"), intParam(ctx, "recipeId"), userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) { ctx.status(404).json(err(e.getMessage())); }
    }

    // ── Equipment handlers ───────────────────────────────────────────────────

    private void getEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getEquipment(userId));
    }

    private void addEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.EquipmentReq.class);
            ctx.status(201).json(svc.addEquipment(userId, req.category, req.name, req.brand, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteEquipment(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.deleteEquipment(intParam(ctx, "id"), userId);
        ctx.status(204);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Integer authRequired(Context ctx) { return gateway.authenticate(ctx, true); }
    private int authOptional(Context ctx) {
        Integer id = gateway.authenticate(ctx, false);
        return id != null ? id : 0;
    }
    private int intParam(Context ctx, String name) {
        try { return Integer.parseInt(ctx.pathParam(name)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid ID: " + ctx.pathParam(name)); }
    }
    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }
}
