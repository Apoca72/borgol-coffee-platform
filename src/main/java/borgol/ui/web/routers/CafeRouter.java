package borgol.ui.web.routers;

import borgol.core.application.CafeService;
import borgol.ui.web.ApiGateway;
import borgol.ui.web.dto.CheckinReq;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;

public class CafeRouter {

    private final CafeService svc;
    private final ApiGateway  gateway;

    public CafeRouter(CafeService svc, ApiGateway gateway) {
        this.svc     = svc;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
        app.get ("/api/cafes",               this::getCafes);
        app.get ("/api/cafes/nearby",        this::getCafesNearby);
        app.get ("/api/cafes/{id}",          this::getCafe);
        app.post("/api/cafes",               this::createCafe);
        app.post("/api/cafes/{id}/rate",     this::rateCafe);
        app.post("/api/cafes/{id}/checkin",  this::cafeCheckin);
        app.get ("/api/cafes/{id}/checkins", this::getCafeCheckins);
    }

    private void getCafes(Context ctx) {
        int    currentId = authOptional(ctx);
        String search    = ctx.queryParam("search");
        String district  = ctx.queryParam("district");
        ctx.json(svc.getCafes(currentId, search, district));
    }

    private void getCafesNearby(Context ctx) {
        int currentId = authOptional(ctx);
        try {
            double lat    = Double.parseDouble(ctx.queryParamAsClass("lat",    String.class).get());
            double lng    = Double.parseDouble(ctx.queryParamAsClass("lng",    String.class).get());
            double radius = Double.parseDouble(ctx.queryParamAsClass("radius", String.class).getOrDefault("10"));
            ctx.json(svc.getCafesNearby(currentId, lat, lng, radius));
        } catch (NumberFormatException | NullPointerException e) {
            ctx.status(400).json(err("lat and lng are required numeric query parameters"));
        }
    }

    private void getCafe(Context ctx) {
        try {
            int id        = intParam(ctx, "id");
            int currentId = authOptional(ctx);
            ctx.json(svc.getCafe(id, currentId));
        } catch (IllegalArgumentException e) {
            ctx.status(404).json(err(e.getMessage()));
        }
    }

    private void createCafe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.CafeReq.class);
            ctx.status(201).json(svc.createCafe(userId, req.name, req.address,
                req.district, req.city, req.phone, req.description, req.hours, req.imageUrl));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void rateCafe(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        try {
            var req = ctx.bodyAsClass(borgol.ui.web.BorgolApiServer.RateReq.class);
            ctx.json(svc.rateCafe(userId, intParam(ctx, "id"), req.rating, req.review));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(err(e.getMessage()));
        }
    }

    private void cafeCheckin(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;
        var req = ctx.bodyAsClass(CheckinReq.class);
        ctx.status(201).json(svc.checkIn(intParam(ctx, "id"), userId, req.note));
    }

    private void getCafeCheckins(Context ctx) {
        ctx.json(svc.getCheckins(intParam(ctx, "id")));
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
