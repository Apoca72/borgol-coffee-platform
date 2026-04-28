package borgol.ui.web.routers;

import borgol.core.application.BrewGuideService;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class BrewGuideRouter {

    private final BrewGuideService svc;

    public BrewGuideRouter(BrewGuideService svc) {
        this.svc = svc;
    }

    public void register(Javalin app) {
        app.get("/api/brew-guides",      this::getBrewGuides);
        app.get("/api/brew-guides/{id}", this::getBrewGuide);
        app.get("/api/learn",            this::getLearnArticles);
        app.get("/api/learn/{id}",       this::getLearnArticle);
    }

    private void getBrewGuides(Context ctx) {
        ctx.json(svc.getBrewGuides());
    }

    private void getBrewGuide(Context ctx) {
        try {
            ctx.json(svc.getBrewGuide(intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void getLearnArticles(Context ctx) {
        ctx.json(svc.getLearnArticles());
    }

    private void getLearnArticle(Context ctx) {
        try {
            ctx.json(svc.getLearnArticle(intParam(ctx, "id")));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private int intParam(Context ctx, String name) {
        try { return Integer.parseInt(ctx.pathParam(name)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid ID: " + ctx.pathParam(name)); }
    }
    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }
}
