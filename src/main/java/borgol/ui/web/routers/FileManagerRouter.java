package borgol.ui.web.routers;

import borgol.infrastructure.storage.S3StorageService;
import borgol.ui.web.ApiGateway;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * File Manager Service — HTTP router for object storage operations.
 *
 * ════════════════════════════════════════════════════════════
 * SOA Role: Dedicated "File Manager Service" (Lab 07 §4)
 * ════════════════════════════════════════════════════════════
 * All endpoints require a valid JWT/SOAP token (Gap 4 — SOAP ValidateToken
 * is invoked by gateway.authenticate before any upload logic runs).
 *
 * Routes:
 *   POST /api/files/upload          — upload a file, returns {"url": "https://..."}
 *     Query param  ?type=avatar     → stored under avatars/{userId}_{ts}.jpg
 *     Query param  ?type=recipe     → stored under recipes/{userId}_{ts}.{ext}
 *     Default                       → stored under uploads/{userId}_{ts}.{ext}
 *
 * Request:  multipart/form-data, field name "file"
 * Response: {"url": "https://cdn.example.com/bucket/avatars/42_1716000000.jpg"}
 */
public class FileManagerRouter {

    private static final Logger log = LoggerFactory.getLogger(FileManagerRouter.class);

    // File size guard — 8 MB application limit (Jetty form limit is 10 MB, keeping 2 MB headroom for multipart overhead)
    private static final long MAX_UPLOAD_BYTES = 8L * 1024 * 1024;

    private final S3StorageService storage;
    private final ApiGateway       gateway;

    /**
     * @param storage may be null when S3 env vars are not set (local dev);
     *                upload endpoint returns 503 in that case.
     */
    public FileManagerRouter(S3StorageService storage, ApiGateway gateway) {
        this.storage = storage;
        this.gateway = gateway;
    }

    public void register(Javalin app) {
        app.post("/api/files/upload", this::uploadFile);
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /**
     * POST /api/files/upload
     *
     * Gap 4: SOAP ValidateToken is called inside gateway.authenticate() before
     * any file I/O. The same authenticate(ctx, true) pattern used by all routers.
     */
    private void uploadFile(Context ctx) {
        // ── Gap 4: SOAP token validation (same pattern as every other router) ──
        Integer userId = authRequired(ctx);
        if (userId == null) return; // 401 already sent by gateway

        // ── Check S3 is configured ──────────────────────────────────────────
        if (storage == null) {
            log.warn("[FileManager] Upload attempted but S3 is not configured");
            ctx.status(503).json(err(
                "File Manager Service not configured. " +
                "Set S3_ACCESS_KEY, S3_SECRET_KEY, S3_BUCKET, S3_ENDPOINT."));
            return;
        }

        // ── Read the multipart file ─────────────────────────────────────────
        UploadedFile file = ctx.uploadedFile("file");
        if (file == null) {
            ctx.status(400).json(err("No file in request. Send a multipart/form-data POST with field name \"file\"."));
            return;
        }

        // Basic MIME validation — only images accepted
        String contentType = file.contentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            ctx.status(400).json(err("Only image files are accepted (received: " + contentType + ")"));
            return;
        }

        if (file.size() > MAX_UPLOAD_BYTES) {
            ctx.status(413).json(err("File too large. Maximum allowed size is 8 MB."));
            return;
        }

        // ── Build object key ────────────────────────────────────────────────
        String type = ctx.queryParamAsClass("type", String.class).getOrDefault("upload");
        String ext  = extensionFromContentType(contentType);
        String key  = buildKey(type, userId, ext);

        // ── Gap 2: Upload to S3-compatible object storage ───────────────────
        try {
            String url = storage.upload(key, file.content(), file.size(), contentType);
            log.info("[FileManager] user={} uploaded {} → {}", userId, key, url);
            ctx.status(201).json(Map.of("url", url));
        } catch (Exception e) {
            log.error("[FileManager] Upload failed for user={}: {}", userId, e.getMessage(), e);
            ctx.status(500).json(err("Upload failed: " + e.getMessage()));
        }
    }

    // ── Key builder ──────────────────────────────────────────────────────────

    /**
     * Generates a unique, human-readable object key.
     *   type=avatar  → "avatars/42_1716000000000.jpg"
     *   type=recipe  → "recipes/42_1716000000000.jpg"
     *   default      → "uploads/42_1716000000000.jpg"
     */
    private String buildKey(String type, int userId, String ext) {
        String folder = switch (type.toLowerCase()) {
            case "avatar" -> "avatars";
            case "recipe" -> "recipes";
            case "cafe"   -> "cafes";
            default       -> "uploads";
        };
        return folder + "/" + userId + "_" + System.currentTimeMillis() + ext;
    }

    /** Maps common MIME types to file extensions. */
    private String extensionFromContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png"               -> ".png";
            case "image/gif"               -> ".gif";
            case "image/webp"              -> ".webp";
            default                        -> ".jpg";
        };
    }

    // ── Helpers (same pattern as every other router) ─────────────────────────

    /** Calls gateway.authenticate(ctx, true) — invokes SOAP ValidateToken then JWT fallback. */
    private Integer authRequired(Context ctx) { return gateway.authenticate(ctx, true); }

    private record ErrorResponse(String error) {}
    private ErrorResponse err(String msg) { return new ErrorResponse(msg); }
}
