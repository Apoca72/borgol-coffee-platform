package borgol.infrastructure.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;

/**
 * Lab 07 — File Manager Service: S3-compatible object storage adapter.
 *
 * Reads all credentials from environment variables — never hardcoded.
 *   S3_ACCESS_KEY  — access key ID
 *   S3_SECRET_KEY  — secret access key
 *   S3_BUCKET      — bucket name
 *   S3_ENDPOINT    — custom endpoint URL (e.g. DigitalOcean Spaces, Cloudflare R2)
 *   S3_REGION      — region (default: us-east-1)
 *   S3_PUBLIC_URL  — public base URL for serving files (optional, falls back to endpoint/bucket)
 *
 * Returns isConfigured() == false if any required env var is absent → caller returns 503.
 */
public class S3FileStorage {

    private final S3Client client;
    private final String   bucket;
    private final String   publicBaseUrl;
    private final boolean  configured;

    public S3FileStorage() {
        String accessKey  = System.getenv("S3_ACCESS_KEY");
        String secretKey  = System.getenv("S3_SECRET_KEY");
        String bucketName = System.getenv("S3_BUCKET");
        String endpoint   = System.getenv("S3_ENDPOINT");
        String region     = System.getenv().getOrDefault("S3_REGION", "us-east-1");
        String publicUrl  = System.getenv("S3_PUBLIC_URL");

        if (accessKey == null || secretKey == null || bucketName == null || endpoint == null) {
            this.configured    = false;
            this.client        = null;
            this.bucket        = null;
            this.publicBaseUrl = null;
            return;
        }

        this.configured = true;
        this.bucket     = bucketName;
        this.publicBaseUrl = (publicUrl != null && !publicUrl.isBlank())
            ? publicUrl.replaceAll("/+$", "")
            : endpoint.replaceAll("/+$", "") + "/" + bucketName;

        this.client = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .forcePathStyle(true) // required for non-AWS S3-compatible providers
            .build();
    }

    public boolean isConfigured() { return configured; }

    /**
     * Uploads bytes from the given InputStream under the specified key and returns
     * the public URL where the file can be accessed.
     *
     * @param key         object key, e.g. "borgol/uploads/3/1716000000000_a1b2c3d4.jpg"
     * @param data        file bytes as InputStream
     * @param contentType MIME type, e.g. "image/jpeg"
     * @param sizeBytes   content length in bytes (required by S3 SDK for streaming)
     * @return public URL string
     */
    public String upload(String key, InputStream data, String contentType, long sizeBytes) {
        PutObjectRequest req = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .acl("public-read") // files must be publicly readable
            .build();

        client.putObject(req, RequestBody.fromInputStream(data, sizeBytes));
        return publicBaseUrl + "/" + key;
    }
}
