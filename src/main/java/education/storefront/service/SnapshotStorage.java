package education.storefront.service;

public interface SnapshotStorage {
    void createBucket(String bucket);
    boolean objectExists(String bucket, String key);
    void putObject(String bucket, String key, byte[] bytes);
}
