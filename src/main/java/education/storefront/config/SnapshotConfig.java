package education.storefront.config;

import java.time.LocalTime;
import java.time.ZoneId;

public final class SnapshotConfig {
    private final String apiKey;
    private final String bucket;
    private final ZoneId zone;
    private final LocalTime nightlyTime;

    private SnapshotConfig(String apiKey, String bucket, ZoneId zone, LocalTime nightlyTime) {
        this.apiKey = apiKey;
        this.bucket = bucket;
        this.zone = zone;
        this.nightlyTime = nightlyTime;
    }

    public static SnapshotConfig fromEnvironment() {
        String key = required("INFRAI_API_KEY");
        String bucket = valueOrDefault("SNAPSHOT_BUCKET", "commerce-learning-snapshots");
        ZoneId zone = ZoneId.of(valueOrDefault("SNAPSHOT_ZONE", "UTC"));
        LocalTime time = LocalTime.parse(valueOrDefault("SNAPSHOT_TIME", "02:00"));
        return new SnapshotConfig(key, bucket, zone, time);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be set");
        }
        return value;
    }

    private static String valueOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public String apiKey() { return apiKey; }
    public String bucket() { return bucket; }
    public ZoneId zone() { return zone; }
    public LocalTime nightlyTime() { return nightlyTime; }
}
