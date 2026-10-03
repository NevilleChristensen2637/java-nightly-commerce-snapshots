package education.storefront.service;

import education.storefront.domain.CommerceSnapshot;
import education.storefront.snapshot.SnapshotDocument;

public final class NightlySnapshotService {
    public enum Result { CREATED, ALREADY_PRESENT }

    private final SnapshotStorage storage;
    private final SnapshotDocument documents;
    private final String bucket;

    public NightlySnapshotService(SnapshotStorage storage, SnapshotDocument documents, String bucket) {
        this.storage = storage;
        this.documents = documents;
        this.bucket = bucket;
    }

    public Result snapshot(CommerceSnapshot commerce) {
        storage.createBucket(bucket);
        String key = documents.objectKey(commerce);
        if (storage.objectExists(bucket, key)) return Result.ALREADY_PRESENT;
        storage.putObject(bucket, key, documents.encode(commerce));
        return Result.CREATED;
    }
}
