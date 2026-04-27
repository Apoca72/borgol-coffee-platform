package borgol.ui.web.routers;

import borgol.core.application.UserService;
import borgol.ui.web.ApiGateway;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class UserRouter {

    private final UserService svc;
    private final ApiGateway  gateway;

    public UserRouter(UserService svc, ApiGateway gateway) {
        this.svc     = svc;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
        app.post("/api/auth/register",      this::register);
        app.post("/api/auth/login",         this::login);
        app.get ("/api/auth/me",            this::getMe);

        app.get   ("/api/users",                  this::getAllUsers);
        app.get   ("/api/users/search",            this::searchUsers);
        app.get   ("/api/users/{id}",              this::getUserProfile);
        app.put   ("/api/users/me",                this::updateProfile);
        app.delete("/api/users/{id}",              this::deleteUser);
        app.post  ("/api/users/{id}/follow",       this::followUser);
        app.delete("/api/users/{id}/follow",       this::unfollowUser);
        app.get   ("/api/users/{id}/following",    this::getUserFollowing);
        app.get   ("/api/users/{id}/followers",    this::getUserFollowers);

        app.get ("/api/notifications",              this::getNotifications);
        app.get ("/api/notifications/count",        this::getNotificationCount);
        app.post("/api/notifications/read",         this::markNotificationsRead);

        app.post  ("/api/users/{id}/block", this::blockUser);
        app.delete("/api/users/{id}/block", this::unblockUser);

        app.post  ("/api/hashtags/{tag}/follow",      this::followHashtag);
        app.delete("/api/hashtags/{tag}/follow",      this::unfollowHashtag);
        app.get   ("/api/users/me/hashtags",          this::getUserHashtags);
    }

    // ── Auth handlers ────────────────────────────────────────────────────────

    private void register(Context ctx) {
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.AuthReq.class);
            var result = svc.register(req.username, req.email, req.password);
            ctx.status(201).json(result);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void login(Context ctx) {
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.AuthReq.class);
            var result = svc.login(req.email, req.password);
            ctx.json(result);
        } catch (IllegalArgumentException e) {
            ctx.status(401).json(err(e.getMessage()));
        }
    }

    private void getMe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        var user = svc.getMe(userId);
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
        resp.put("isAdmin",        svc.isAdmin(userId));
        ctx.json(resp);
    }

    // ── User handlers ────────────────────────────────────────────────────────

    private void getUserProfile(Context ctx) {
        try {
            int targetId   = intParam(ctx, "id");
            int currentId  = authOptional(ctx);
            ctx.json(svc.getUserProfile(targetId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void updateProfile(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.ProfileUpdateReq.class);
            ctx.json(svc.updateProfile(userId, req.bio, req.avatarUrl,
                req.expertiseLevel, req.flavorPrefs));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteUser(Context ctx) {
        Integer requesterId = authRequired(ctx);
        if (requesterId == null) return;
        try {
            int targetId = intParam(ctx, "id");
            if (requesterId != targetId && !svc.isAdmin(requesterId)) {
                ctx.status(403).json(err("Not authorized"));
                return;
            }
            svc.deleteUser(targetId);
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
            svc.followUser(userId, targetId);
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
            svc.unfollowUser(userId, targetId);
            ctx.json(Map.of("following", false));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void getAllUsers(Context ctx) {
        int currentId = authOptional(ctx);
        ctx.json(svc.getAllUsers(currentId));
    }

    private void searchUsers(Context ctx) {
        String query  = ctx.queryParam("q");
        int currentId = authOptional(ctx);
        ctx.json(svc.searchUsers(query, currentId));
    }

    private void getUserFollowing(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getFollowingUsers(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getUserFollowers(Context ctx) {
        try {
            int userId    = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getFollowerUsers(userId, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Notification handlers ────────────────────────────────────────────────

    private void getNotifications(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getNotifications(userId));
    }

    private void getNotificationCount(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getNotificationCount(userId));
    }

    private void markNotificationsRead(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.markNotificationsRead(userId);
        ctx.json(Map.of("ok", true));
    }

    // ── Block handlers ───────────────────────────────────────────────────────

    private void blockUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            svc.blockUser(userId, intParam(ctx, "id"));
            ctx.json(Map.of("blocked", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void unblockUser(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.unblockUser(userId, intParam(ctx, "id"));
        ctx.json(Map.of("blocked", false));
    }

    // ── Hashtag handlers ─────────────────────────────────────────────────────

    private void followHashtag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            svc.followHashtag(userId, ctx.pathParam("tag"));
            ctx.json(Map.of("following", true));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void unfollowHashtag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.unfollowHashtag(userId, ctx.pathParam("tag"));
        ctx.json(Map.of("following", false));
    }

    private void getUserHashtags(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getUserHashtags(userId));
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
