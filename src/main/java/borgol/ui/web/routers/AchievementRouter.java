package borgol.ui.web.routers;

import borgol.core.application.AchievementService;
import borgol.ui.web.ApiGateway;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class AchievementRouter {

    private final AchievementService svc;
    private final ApiGateway         gateway;

    public AchievementRouter(AchievementService svc, ApiGateway gateway) {
        this.svc     = svc;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
        app.get ("/api/achievements",        this::getAchievements);
        app.post("/api/achievements/check",  this::checkAchievements);
    }

    private void getAchievements(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getAchievements(userId));
    }

    private void checkAchievements(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.checkAndAwardAchievements(userId));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Integer authRequired(Context ctx) { return gateway.authenticate(ctx, true); }
}
