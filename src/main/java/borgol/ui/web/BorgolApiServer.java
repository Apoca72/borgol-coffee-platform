package borgol.ui.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.Javalin;
import io.javalin.http.Context;
import borgol.core.application.BorgolService;
import borgol.core.application.MenuService;
import borgol.core.domain.MenuCategory;
import borgol.infrastructure.messaging.RedisEventBus;
import borgol.infrastructure.security.SoapAuthClient;
import borgol.ui.web.routers.UserRouter;
import borgol.ui.web.routers.RecipeRouter;
import borgol.ui.web.routers.BrewGuideRouter;
import borgol.ui.web.routers.JournalRouter;
import borgol.ui.web.routers.CafeRouter;
import borgol.ui.web.routers.AchievementRouter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Borgol REST API coordinator — delegates domain routes to domain routers,
 * retains only cross-cutting concerns (SOAP, SSE, AI chat, legacy menu).
 */
public class BorgolApiServer {

    private final BorgolService  borgol;
    private final MenuService    menuService;
    private final Javalin        app;
    private final ApiGateway     gateway;
    private final RedisEventBus  eventBus;

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

    // ── Route registration — delegates to domain routers ─────────────────────

    private void registerRoutes() {
        // Static / health
        app.get("/", ctx -> ctx.redirect("/index.html"));
        app.get("/health", ctx -> ctx.json(Map.of("status", "ok")));

        // ── Domain routers ──────────────────────────────────────────────────
        new UserRouter(borgol.getUserService(), gateway).register(app);
        new RecipeRouter(borgol.getRecipeService(), borgol.getUserService(), gateway).register(app);
        new BrewGuideRouter(borgol.getBrewGuideService()).register(app);
        new JournalRouter(borgol, gateway).register(app);
        new CafeRouter(borgol.getCafeService(), gateway).register(app);
        new AchievementRouter(borgol.getAchievementService(), gateway).register(app);

        // ── SOAP auth (SOA lab — kept in coordinator) ───────────────────────
        app.post("/api/soap/register", this::soapRegister);
        app.post("/api/soap/login",    this::soapLogin);

        // ── SSE notification stream (requires RedisEventBus) ────────────────
        app.get("/api/notifications/stream", this::notificationStream);

        // ── Bean AI chat (Gemini SSE — cross-cutting) ───────────────────────
        app.post("/api/bean/chat", this::beanChat);

        // ── Legacy menu API ─────────────────────────────────────────────────
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

    // ── SOAP auth handlers (SOA lab — Strategy + Fallback Chain) ─────────────

    private void soapRegister(Context ctx) {
        var req = ctx.bodyAsClass(AuthReq.class);
        SoapAuthClient.AuthResult soapResult =
                gateway.soapRegister(req.username, req.email, req.password);
        boolean soapDown = soapResult.message() != null &&
                           soapResult.message().startsWith("SOAP service unavailable");
        if (!soapResult.success() && !soapDown) {
            ctx.status(400).json(err(soapResult.message()));
            return;
        }
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

    private void soapLogin(Context ctx) {
        var req = ctx.bodyAsClass(AuthReq.class);
        SoapAuthClient.AuthResult soapResult =
                gateway.soapLogin(req.email, req.password);
        if (!soapResult.success()) {
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

    // ── SSE notification stream (requires RedisEventBus) ─────────────────────

    private void notificationStream(Context ctx) {
        Integer userId = authRequired(ctx);
        if (userId == null) return;

        ctx.contentType("text/event-stream");
        ctx.header("Cache-Control",  "no-cache");
        ctx.header("Connection",     "keep-alive");
        ctx.header("X-Accel-Buffering", "no");

        try {
            PrintWriter writer = ctx.res().getWriter();
            final int uid = userId;

            Consumer<String> handler = event -> {
                try {
                    writer.write("data: " + event + "\n\n");
                    writer.flush();
                } catch (Exception ignored) {}
            };

            eventBus.subscribe(uid, handler);
            try {
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

    // ── Bean AI chat (Google Gemini SSE) ──────────────────────────────────────

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
        public String role;
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
        try { req = ctx.bodyAsClass(BeanReq.class); }
        catch (Exception e) { ctx.status(400).json(err("Invalid request body")); return; }
        if (req.messages == null || req.messages.isEmpty()) {
            ctx.status(400).json(err("messages required"));
            return;
        }
        try {
            ObjectNode body = BEAN_MAPPER.createObjectNode();
            ObjectNode sysInstr = body.putObject("system_instruction");
            ArrayNode sysParts = sysInstr.putArray("parts");
            sysParts.addObject().put("text", BEAN_SYSTEM);
            ArrayNode contents = body.putArray("contents");
            for (BeanMessage m : req.messages) {
                String geminiRole = "assistant".equals(m.role) ? "model" : "user";
                ObjectNode turn = contents.addObject();
                turn.put("role", geminiRole);
                turn.putArray("parts").addObject().put("text", m.content);
            }
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
                            ctx.outputStream().write(
                                ("data: " + escJson(text.asText()) + "\n\n")
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

    private static String escJson(String s) {
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    // ── Auth helpers ─────────────────────────────────────────────────────────

    private Integer authRequired(Context ctx) {
        return gateway.authenticate(ctx, true);
    }

    private int intParam(Context ctx, String name) {
        try { return Integer.parseInt(ctx.pathParam(name)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid ID: " + ctx.pathParam(name)); }
    }

    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }

    // ── Request POJOs (shared with routers) ──────────────────────────────────

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
        public String action;
    }

    public static class BeanBagReq {
        public String name          = "";
        public String roaster       = "";
        public String origin        = "";
        public String roastLevel    = "MEDIUM";
        public String roastDate;
        public double remainingGrams = 0;
        public int    rating         = 0;
        public String notes          = "";
    }
}
