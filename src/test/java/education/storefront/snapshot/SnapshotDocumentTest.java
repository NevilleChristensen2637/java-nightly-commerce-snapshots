package education.storefront.snapshot;

import education.storefront.domain.CommerceSnapshot;
import education.storefront.service.NightlySnapshotService;
import education.storefront.service.SnapshotStorage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public final class SnapshotDocumentTest {
    public static void main(String[] args) {
        CommerceSnapshot input = CommerceSnapshot.lessonOrder(LocalDate.parse("2026-09-04"));
        SnapshotDocument document = new SnapshotDocument();
        String body = new String(document.encode(input), StandardCharsets.UTF_8);

        require(document.objectKey(input).equals("nightly/2026-09-04/commerce.json"), "stable date-based key");
        require(body.contains("\"checkout_id\":\"checkout-1042\""), "checkout included");
        require(body.contains("\"state\":\"DIGITAL_ACCESS_GRANTED\""), "fulfillment included");
        require(body.contains("\"receipt_id\":\"receipt-1042\""), "receipt included");
        require(body.contains("\"customer_id\":\"learner-28\""), "customer update included");

        RecordingStorage emptyStorage = new RecordingStorage(false);
        NightlySnapshotService firstRun = new NightlySnapshotService(emptyStorage, document, "course-orders");
        require(firstRun.snapshot(input) == NightlySnapshotService.Result.CREATED, "first run creates snapshot");
        require(emptyStorage.putCount == 1, "first run writes once");

        RecordingStorage populatedStorage = new RecordingStorage(true);
        NightlySnapshotService retry = new NightlySnapshotService(populatedStorage, document, "course-orders");
        require(retry.snapshot(input) == NightlySnapshotService.Result.ALREADY_PRESENT, "retry preserves snapshot");
        require(populatedStorage.putCount == 0, "retry does not overwrite");
        System.out.println("PASS: one business date maps to one complete commerce snapshot");
    }

    private static void require(boolean condition, String decision) {
        if (!condition) throw new AssertionError(decision);
    }

    private static final class RecordingStorage implements SnapshotStorage {
        private final boolean exists;
        private int putCount;

        private RecordingStorage(boolean exists) { this.exists = exists; }
        public void createBucket(String bucket) { }
        public boolean objectExists(String bucket, String key) { return exists; }
        public void putObject(String bucket, String key, byte[] bytes) { putCount++; }
    }
}
