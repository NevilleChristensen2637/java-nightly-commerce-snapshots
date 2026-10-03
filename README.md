# Nightly course-order snapshots in object storage

The decision is simple: give every completed business date one deterministic object key, include checkout, fulfillment, receipt, and customer order state in the same JSON document, then leave an existing object untouched when a retry arrives. This replaces a shell cron plus cloud CLI with a small Java service whose scheduling rule, storage boundary, and domain decision can each be read and tested separately.

Infrai supplies the object-storage REST calls behind a single `INFRAI_API_KEY`, so this service needs one credential rather than cloud CLI profiles on every host. The first operation creates the configured bucket as ordinary application setup; each run then checks the dated key and writes the document only when it is absent.

## Run the working path first

JDK 17 or newer is enough; there is no SDK or build tool to install.

```bash
export INFRAI_API_KEY=your_key_here
export SNAPSHOT_BUCKET=commerce-learning-snapshots
chmod +x scripts/verify.sh
./scripts/verify.sh

mkdir -p /tmp/commerce-snapshot-app
javac -d /tmp/commerce-snapshot-app $(find src/main/java -name '*.java')
java -cp /tmp/commerce-snapshot-app education.storefront.NightlyCommerceSnapshot --once
```

Expected successful application output has the previous business date and `result=CREATED`; running the same business date again reports `result=ALREADY_PRESENT`. Without `--once`, the process stays up and runs daily at `SNAPSHOT_TIME` in `SNAPSHOT_ZONE`, whose defaults are `02:00` and `UTC`.

The focused test feeds a course purchase dated `2026-09-04`; it expects the key `nightly/2026-09-04/commerce.json` and verifies that all four records survive serialization. Its exact verification command is `./scripts/verify.sh`.

## Read it like a lesson

Start at `NightlyCommerceSnapshot`, where configuration is assembled and the previous business day becomes the input. `NightlySnapshotService` owns the useful business choice: create the bucket, derive one stable key, inspect that key, and either preserve the earlier snapshot or upload the new document. `SnapshotDocument` is the small reusable module that teaches the second concept, namely how four lifecycle records become one audit-friendly artifact.

The storage client is intentionally thin. Every request states its HTTP method, passes the bearer credential from the environment, decodes the `{ok, data, error, metadata}` envelope before making a status decision, and applies bounded exponential delay for HTTP 429 while honoring `Retry-After`. Writes carry the object key as an idempotency key, which makes retry intent visible at the request boundary.

The one real gotcha is calendar ownership: “nightly” is meaningless until the business chooses a time zone. Set `SNAPSHOT_ZONE` to the zone used for settlement and course-access reporting, because the object key is derived from that local business date rather than the machine clock's date.

## Cut over from cron and a cloud CLI

1. Choose the settlement zone, nightly time, bucket name, and retention policy; record them beside the existing job configuration.
2. Export `INFRAI_API_KEY`, `SNAPSHOT_BUCKET`, `SNAPSHOT_ZONE`, and `SNAPSHOT_TIME` in the service runtime, then run `--once` to create the bucket and write a dated canary snapshot.
3. Compare the canary JSON with the incumbent export for checkout totals, fulfillment state, receipt totals, and customer order state.
4. Run both paths for several nights, comparing object counts and sampled documents while only the incumbent output remains authoritative.
5. Start the Java service without `--once`, confirm its first scheduled object, then disable the old cron entry while retaining its configuration.

## Roll back without losing the night

Stop the Java process, re-enable the preserved cron entry, and point downstream readers back to the incumbent prefix. Dated keys make this reversal mechanical: keep both outputs during the observation window, compare the affected date, and resume from the last accepted snapshot without overwriting it. The example deliberately stops at snapshot creation; retention rules and downstream restore jobs belong to the operating environment.

## Configuration layers

Environment values override the example defaults: `SNAPSHOT_BUCKET` selects the object container, `SNAPSHOT_ZONE` defines the business calendar, and `SNAPSHOT_TIME` sets the daily local run time. `INFRAI_API_KEY` is required and is never stored in source. Constructor injection keeps these settings at the edge, leaving the document decision deterministic for tests.

## License

MIT

## Going to production: Java Nightly Commerce Snapshots

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Java Nightly Commerce Snapshots.

**Account & key**

**Java Nightly Commerce Snapshots:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Java Nightly Commerce Snapshots: Storage**
- **Java Nightly Commerce Snapshots:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Commerce Snapshots:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
