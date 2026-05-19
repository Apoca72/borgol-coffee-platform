package borgol.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;

/**
 * File Manager Service — S3-compatible Object Storage adapter.
 *
 * Supports DigitalOcean Spaces, Cloudflare R2, Backblaze B2, AWS S3, or
 * any S3-compatible endpoint. All credentials are read from environment
 * variables — never hardcoded.
 *
 * Required environment variables:
 *   S3_ACCESS_KEY  — API key / access key ID
 *   S3_SECRET_KEY  — API secret / secret access key
 *   S3_BUCKET      — bucket name (e.g. "borgol-media")
 *   S3_ENDPOINT    — full base URL (e.g. "https://sgp1.digitaloceanspaces.com")
 *
 * Returned public URL format:
 *   {S3_ENDPOINT}/{S3_BUCKET}/{key}
 *   e.g. https://sgp1.digitaloceanspaces.com/borgol-media/avatars/42_1716000000.jpg
 */
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String   bucket;
    private final String   publicBase; // endpoint/bucket — prepended to every key

    /**
     * Builds the S3 client from environment variables.
     * Throws {@link IllegalStateException} if any required variable is missing.
     */
    public S3StorageService() {
        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");
        this.bucket      = System.getenv("S3_BUCKET");
        String endpoint  = System.getenv("S3_ENDPOINT");

        if (accessKey == null || secretKey == null || bucket == null || endpoint == null) {
            throw new IllegalStateException(
                "File Manager: missing S3 environment variables. " +
                "Required: S3_ACCESS_KEY, S3_SECRET_KEY, S3_BUCKET, S3_ENDPOINT");
        }

        // Derive a region string from the endpoint host for the SDK.
        // DigitalOcean Spaces format: https://sgp1.digitaloceanspaces.com → "sgp1"
        // For AWS S3: https://s3.amazonaws.com → falls back to "us-east-1"
        String region = deriveRegion(endpoint);

        this.s3 = S3Client.builder()
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();

        // Normalise endpoint (strip trailing slash) before building public base
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.publicBase = base + "/" + bucket;

        log.info("[FileManager] S3 client ready → endpoint={}, bucket={}", endpoint, bucket);
    }

    /**
     * Uploads a file to object storage and returns its public URL.
     *
     * @param key         object key, e.g. "avatars/42_1716000000.jpg"
     * @param data        raw bytes as InputStream
     * @param size        byte length of the stream (required by AWS SDK v2)
     * @param contentType MIME type, e.g. "image/jpeg"
     * @return            publicly accessible HTTPS URL of the uploaded object
     */
    public String upload(String key, InputStream data, long size, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .acl(ObjectCannedACL.PUBLIC_READ)   // files must be publicly readable
            .build();

        s3.putObject(req, RequestBody.fromInputStream(data, size));
        String url = publicBase + "/" + key;
        log.info("[FileManager] Uploaded → {}", url);
        return url;
    }

    /** Returns true if this service was configured (env vars present). */
    public static boolean isConfigured() {
        return System.getenv("S3_ACCESS_KEY") != null
            && System.getenv("S3_SECRET_KEY") != null
            && System.getenv("S3_BUCKET")     != null
            && System.getenv("S3_ENDPOINT")   != null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String deriveRegion(String endpoint) {
        try {
            String host = URI.create(endpoint).getHost(); // e.g. "sgp1.digitaloceanspaces.com"
            if (host != null && host.contains(".")) {
                String subdomain = host.split("\\.")[0]; // "sgp1" / "nyc3" / "s3" / etc.
                if (!"s3".equals(subdomain)) return subdomain; // DO/R2/B2 sub-region
            }
        } catch (Exception ignored) {}
        return "us-east-1"; // AWS S3 or unrecognised endpoint → default region
    }
}
