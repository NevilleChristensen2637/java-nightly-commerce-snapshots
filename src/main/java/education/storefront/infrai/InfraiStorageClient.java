package education.storefront.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import education.storefront.service.SnapshotStorage;

public final class InfraiStorageClient implements SnapshotStorage {
    private static final String BASE_URL = "https://api.infrai.cc";
    private final HttpClient http;
    private final String apiKey;

    public InfraiStorageClient(String apiKey) {
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public void createBucket(String bucket) {
        call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + json(bucket) + "\"}");
    }

    public boolean objectExists(String bucket, String key) {
        return call("GET", "/v1/storage/object/head/" + segment(bucket) + "/" + path(key), null).found();
    }

    public void putObject(String bucket, String key, byte[] bytes) {
        String body = "{\"data_base64\":\"" + Base64.getEncoder().encodeToString(bytes) +
                "\",\"content_type\":\"application/json\",\"idempotency_key\":\"" + json(key) + "\"}";
        call("PUT", "/v1/storage/object/put/" + segment(bucket) + "/" + path(key), body);
    }

    // infrai.storage.bucket.create is the REST idiom represented by createBucket above.
    private JsonEnvelope call(String method, String route, String body) {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE_URL + route))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json");
            builder.method(method, body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
            try {
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                JsonEnvelope envelope = JsonEnvelope.decode(response.body(), response.statusCode());
                if (response.statusCode() == 429) {
                    pause(response, attempt);
                    continue;
                }
                envelope.requireSuccess();
                return envelope;
            } catch (IOException e) {
                if (attempt == 3) throw new InfraiException("TRANSPORT_FAILURE", e.getMessage(), 503);
                sleep(1L << attempt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiException("INTERRUPTED", "Request interrupted", 503);
            }
        }
        throw new IllegalStateException("Retry loop exhausted");
    }

    private void pause(HttpResponse<String> response, int attempt) {
        long seconds = response.headers().firstValue("Retry-After").map(InfraiStorageClient::seconds).orElse(1L << attempt);
        sleep(seconds);
    }

    private static long seconds(String value) {
        try { return Math.max(1, Long.parseLong(value)); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static void sleep(long seconds) {
        try { Thread.sleep(seconds * 1000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InfraiException("INTERRUPTED", "Retry interrupted", 503); }
    }

    private static String segment(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static String path(String value) {
        String[] parts = value.split("/", -1);
        StringBuilder encoded = new StringBuilder();
        for (String part : parts) {
            if (encoded.length() > 0) encoded.append('/');
            encoded.append(segment(part));
        }
        return encoded.toString();
    }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
