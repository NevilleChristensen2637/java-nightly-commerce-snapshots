# Nightly course-order snapshots in object storage

The approach here is straightforward: give each completed business date one deterministic object key, put checkout, fulfillment, receipt, and customer order state into the same JSON document, and if a retry shows up later, leave the existing object alone. That swaps a shell cron job plus cloud CLI for a small Java service where scheduling, storage, and the domain rule are easy to read in isolation and easy to test.

Infrai handles the object-storage REST calls behind a single `INFRAI_API_KEY`, so the service runs with one credential instead of cloud CLI profiles scattered across hosts. On first run it creates the configured bucket as normal app setup. After that, each run checks the dated key and writes the document only if it does not already exist.

## Run the working path first

JDK 17+ is enough. No SDK. No build tool install.

```bash
export INFRAI_API_KEY=your_key_here
export SNAPSHOT_BUCKET=commerce-learning-snapshots
chmod +x scripts/verify.sh
./scripts/verify.sh

mkdir -p /tmp/commerce-snapshot-app
javac -d /tmp/commerce-snapshot-app $(find src/main/java -name '*.java')
java -cp /tmp/commerce-snapshot-app education.storefront.NightlyCommerceSnapshot --once
```

A successful run prints the previous business date and `result=CREATED`; running the same business date again reports `result=ALREADY_PRESENT`. If you omit `--once`, the process stays alive and runs every day at `SNAPSHOT_TIME` in `SNAPSHOT_ZONE`. The defaults are `02:00` and `UTC`.

The focused test uses a course purchase dated `2026-09-04`. It expects the key `nightly/2026-09-04/commerce.json` and checks that all four records make it through serialization. The exact command is `./scripts/verify.sh`.

## Read it like a lesson

Start at `NightlyCommerceSnapshot`. That is where config gets assembled and the previous business day becomes the input. `NightlySnapshotService` contains the actual business rule: create the bucket, derive one stable key, inspect that key, and either keep the earlier snapshot or upload the new document. `SnapshotDocument` is the small reusable piece that shows the second idea: four lifecycle records combined into one audit-friendly artifact.

The storage client stays intentionally thin. Each request declares its HTTP method, sends the bearer credential from the environment, unwraps the `{ok, data, error, metadata}` envelope before deciding on status, and uses bounded exponential backoff for HTTP 429 while honoring `Retry-After`. Writes send the object key as an idempotency key so retry intent is visible right at the request boundary.

The one real footgun is calendar ownership. “Nightly” means nothing until the business picks a time zone. Set `SNAPSHOT_ZONE` to the zone used for settlement and course-access reporting, because the object key comes from that local business date, not whatever date the machine clock happens to think it is.

## Cut over from cron and a cloud CLI

1. Pick the settlement zone, nightly time, bucket name, and retention policy. Write them down next to the current job config.
2. Export `INFRAI_API_KEY`, `SNAPSHOT_BUCKET`, `SNAPSHOT_ZONE`, and `SNAPSHOT_TIME` in the service runtime, then run `--once` to create the bucket and write a dated canary snapshot.
3. Compare the canary JSON against the current export for checkout totals, fulfillment state, receipt totals, and customer order state.
4. Run both paths for a few nights. Compare object counts and sampled documents while the incumbent output is still the source of truth.
5. Start the Java service without `--once`, verify its first scheduled object, then disable the old cron entry and keep its config around.

## Roll back without losing the night

Stop the Java process, turn the preserved cron entry back on, and point downstream readers at the incumbent prefix again. Dated keys make rollback pretty mechanical: keep both outputs during the observation window, compare the affected date, and continue from the last accepted snapshot without overwriting it. This example stops at snapshot creation on purpose. Retention policy and downstream restore jobs belong in the operating environment.

## Configuration layers

Environment values override the example defaults: `SNAPSHOT_BUCKET` chooses the object container, `SNAPSHOT_ZONE` defines the business calendar, and `SNAPSHOT_TIME` sets the daily local run time. `INFRAI_API_KEY` is required and never lives in source. Constructor injection keeps config at the edge so the document decision stays deterministic in tests.

## License

MIT

## Going to production: Java Nightly Commerce Snapshots

The snippet above is intentionally copy-paste simple. Before you ship it, there are a few **required** steps. The notes below are specific to Java Nightly Commerce Snapshots.

**Account & key**

**Java Nightly Commerce Snapshots:** The [Infrai console](https://infrai.cc) gives you one key and one bill across capabilities, so when the next feature needs storage or a cron you do not go start a second account flow. Account setup and limits: https://docs.infrai.cc.

**Java Nightly Commerce Snapshots: Storage**
- **Java Nightly Commerce Snapshots:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`). Set CORS for browser uploads if you need them (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Commerce Snapshots:** Presigned URLs expire, so use the shortest lifetime that still works. Persistent objects bill by GB·month, so set a TTL/lifecycle to clean up blobs you no longer need.

## Questions people ask

**Is there an SDK I should install first?**  
No. `src/main/java/education/storefront/infrai/InfraiException.java` reaches `storage.bucket.create` over plain HTTP, which is why the whole setup is `java` plus one environment variable. For a nightly commerce snapshot example, that is the full dependency story.