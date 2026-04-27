package borgol.ui.web.routers;

import borgol.core.application.BorgolService;
import borgol.ui.web.ApiGateway;
import borgol.ui.web.dto.JournalReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class JournalRouter {

    private final BorgolService svc;
    private final ApiGateway    gateway;

    public JournalRouter(BorgolService svc, ApiGateway gateway) {
        this.svc     = svc;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
        app.get   ("/api/journal",      this::getJournal);
        app.post  ("/api/journal",      this::createJournalEntry);
        app.put   ("/api/journal/{id}", this::updateJournalEntry);
        app.delete("/api/journal/{id}", this::deleteJournalEntry);
        app.get   ("/api/journal/stats", this::getJournalStats);

        app.get   ("/api/beans",      this::getBeanBags);
        app.post  ("/api/beans",      this::createBeanBag);
        app.put   ("/api/beans/{id}", this::updateBeanBag);
        app.delete("/api/beans/{id}", this::deleteBeanBag);
    }

    private void getJournal(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getJournalEntries(userId));
    }

    private void createJournalEntry(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(JournalReq.class);
            ctx.status(201).json(svc.createJournalEntry(userId, req.coffeeBean, req.origin,
                req.roastLevel, req.brewMethod, req.grindSize, req.waterTempC,
                req.doseGrams, req.yieldGrams, req.brewTimeSec,
                req.ratingAroma, req.ratingFlavor, req.ratingAcidity,
                req.ratingBody, req.ratingSweetness, req.ratingFinish, req.notes, req.weatherData));
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
            ctx.json(svc.updateJournalEntry(id, userId, req.coffeeBean, req.origin,
                req.roastLevel, req.brewMethod, req.grindSize, req.waterTempC,
                req.doseGrams, req.yieldGrams, req.brewTimeSec,
                req.ratingAroma, req.ratingFlavor, req.ratingAcidity,
                req.ratingBody, req.ratingSweetness, req.ratingFinish, req.notes, req.weatherData));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteJournalEntry(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            svc.deleteJournalEntry(intParam(ctx, "id"), userId);
            ctx.status(204);
        } catch (IllegalArgumentException e) {
            ctx.status(403).json(err(e.getMessage()));
        }
    }

    private void getJournalStats(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getJournalStats(userId));
    }

    // ── Bean Bag handlers ────────────────────────────────────────────────────

    private void getBeanBags(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        ctx.json(svc.getBeanBags(userId));
    }

    private void createBeanBag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.BeanBagReq.class);
            if (req.name == null || req.name.isBlank())
                throw new IllegalArgumentException("Bean name is required");
            ctx.status(201).json(svc.createBeanBag(userId, req.name, req.roaster, req.origin,
                req.roastLevel, req.roastDate, req.remainingGrams, req.rating, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void updateBeanBag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.BeanBagReq.class);
            if (req.name == null || req.name.isBlank())
                throw new IllegalArgumentException("Bean name is required");
            ctx.json(svc.updateBeanBag(intParam(ctx, "id"), userId, req.name, req.roaster,
                req.origin, req.roastLevel, req.roastDate, req.remainingGrams, req.rating, req.notes));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void deleteBeanBag(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        svc.deleteBeanBag(intParam(ctx, "id"), userId);
        ctx.status(204);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Integer authRequired(Context ctx) { return gateway.authenticate(ctx, true); }
    private int intParam(Context ctx, String name) {
        try { return Integer.parseInt(ctx.pathParam(name)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid ID: " + ctx.pathParam(name)); }
    }
    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }
}
