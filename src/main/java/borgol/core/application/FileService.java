package borgol.core.application;

import borgol.infrastructure.storage.S3FileStorage;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Lab 07 — File Manager Service: business logic for image uploads.
 *
 * Validates content type, generates a unique object key, delegates to S3FileStorage.
 * Returns the public URL stored in the database (not base64).
 */
public class FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_BYTES = 8 * 1024 * 1024; // 8 MB

    private final S3FileStorage storage;

    public FileService(S3FileStorage storage) {
        this.storage = storage;
    }

    public boolean isConfigured() { return storage.isConfigured(); }

    /**
     * Upload an image for a user. Returns the public URL.
     *
     * @param userId      owner user ID (used as path prefix in the object key)
     * @param filename    original filename (used to extract extension)
     * @param contentType MIME type asserted by the client
     * @param data        file bytes as InputStream
     * @param sizeBytes   file size in bytes
     * @return public URL of the uploaded image
     * @throws IllegalArgumentException if content type or size is invalid
     */
    public String uploadImage(int userId, String filename, String contentType,
                              InputStream data, long sizeBytes) {
        if (!ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
        if (sizeBytes > MAX_BYTES) {
            throw new IllegalArgumentException("File too large (max 8 MB)");
        }

        String ext = extractExtension(filename, contentType);
        String key = String.format("borgol/uploads/%d/%d_%s%s",
            userId, System.currentTimeMillis(),
            UUID.randomUUID().toString().replace("-", "").substring(0, 8),
            ext);

        return storage.upload(key, data, contentType, sizeBytes);
    }

    private String extractExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|webp|gif)")) return ext;
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default           -> ".bin";
        };
    }
}
