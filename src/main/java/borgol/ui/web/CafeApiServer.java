package borgol.ui.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import borgol.core.application.MenuService;
import borgol.core.domain.MenuCategory;

/**
 * REST API сервер — Inbound Web Adapter.
 *
 * Загвар: Adapter (Hexagonal Architecture) — HTTP хүсэлтийг MenuService дуудлага болгон хөрвүүлнэ.
 * Жишиг: TMS-ийн MainFrame.java-тай адилхан үүрэгтэй, зөвхөн HTTP ашигладаг.
 *
 * REST endpoints:
 *   GET    /api/menu        → getAllItems()
 *   GET    /api/menu/{id}   → getItemById()
 *   POST   /api/menu        → addItem()
 *   PUT    /api/menu/{id}   → updateItem()
 *   DELETE /api/menu/{id}   → removeItem()
 *
 * Static files: /public/index.html → http://localhost:7000/
 *
 * Архитектурын дүрэм: java.sql.*, Repository-г шууд дуудахгүй.
 *                     Зөвхөн MenuService-г ашиглана.
 */
public class CafeApiServer {

    private final MenuService menuService;
    private final Javalin     app;

    public CafeApiServer(MenuService menuService) {
        this.menuService = menuService;
        this.app = Javalin.create(config -> {
            // Serve static files from src/main/resources/public/
            config.staticFiles.add("/public");
            // Reduce Javalin startup noise in console
            config.showJavalinBanner = false;
        });
        registerRoutes();
    }

    // ── Route бүртгэл ─────────────────────────────────────────────────────────

    private void registerRoutes() {
        // Redirect / → /index.html
        app.get("/", ctx -> ctx.redirect("/index.html"));

        // REST API
        app.get   ("/api/menu",      this::getAllItems);
        app.get   ("/api/menu/{id}", this::getItemById);
        app.post  ("/api/menu",      this::addItem);
        app.put   ("/api/menu/{id}", this::updateItem);
        app.delete("/api/menu/{id}", this::deleteItem);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void getAllItems(Context ctx) {
        ctx.json(menuService.getAllItems());
    }

    private void getItemById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            menuService.getItemById(id).ifPresentOrElse(
                    ctx::json,
                    () -> ctx.status(404).json(error("Item not found: id=" + id))
            );
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("Invalid ID format"));
        }
    }

    private void addItem(Context ctx) {
        try {
            MenuItemRequest req = ctx.bodyAsClass(MenuItemRequest.class);
            var dto = menuService.addItem(
                    req.name, MenuCategory.valueOf(req.category), req.price, req.available);
            ctx.status(201).json(dto);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(error(e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(error("Server error: " + e.getMessage()));
        }
    }

    private void updateItem(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            MenuItemRequest req = ctx.bodyAsClass(MenuItemRequest.class);
            var dto = menuService.updateItem(
                    id, req.name, MenuCategory.valueOf(req.category), req.price, req.available);
            ctx.json(dto);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(error(e.getMessage()));
        } catch (Exception e) {
            ctx.status(500).json(error("Server error: " + e.getMessage()));
        }
    }

    private void deleteItem(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            menuService.removeItem(id);
            ctx.status(204);
        } catch (NumberFormatException e) {
            ctx.status(400).json(error("Invalid ID format"));
        } catch (Exception e) {
            ctx.status(500).json(error("Server error: " + e.getMessage()));
        }
    }

    // ── Туслах ─────────────────────────────────────────────────────────────────

    private record ErrorResponse(String error) {}

    private ErrorResponse error(String msg) {
        return new ErrorResponse(msg);
    }

    public void start(int port) {
        app.start(port);
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║    MyCafé Menu Manager — Web UI          ║");
        System.out.printf ("  ║    http://localhost:%-21d║%n", port);
        System.out.println("  ║    Press Ctrl+C to stop                  ║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * JSON хүсэлтийн POJO — Jackson public field deserialization ашиглана.
     * POST /api/menu, PUT /api/menu/{id} хоёуланд хэрэглэнэ.
     */
    public static class MenuItemRequest {
        public String  name;
        public String  category = "COFFEE";
        public double  price;
        public boolean available = true;
    }
}
