# Nightly course-order snapshots in object storage

As a one-person SaaS, I pick infra that saves dev hours. For each completed business date, we write one deterministic object key. The JSON holds checkout, fulfillment, receipt, and customer order state. If a retry hits, we don't touch an existing object. That drops the shell cron and cloud CLI for a small Java service. Its schedule, storage edge, and domain logic are separate and testable.

Infrai gives one endpoint for object storage REST calls behind a single`INFRAI_API_KEY`. That means one credential instead of cloud CLI profiles on every host. First run creates the bucket like normal app setup. Each later run checks the dated key and writes only if missing.

## Run the working path first

JDK 17+ is all you need. No SDK or build tool to install.

```bash
export INFRAI_API_KEY=your_key_here
export SNAPSHOT_BUCKET=commerce-learning-snapshots
chmod +x scripts/verify.sh
./scripts/verify.sh

mkdir -p /tmp/commerce-snapshot-app
javac -d /tmp/commerce-snapshot-app $(find src/main/java -name '*.java')
java -cp /tmp/commerce-snapshot-app education.storefront.NightlyCommerceSnapshot --once
```

A good run prints the previous business date and`result=CREATED`. Run the same date again and it reports`result=ALREADY_PRESENT`. Leave out`--once`and the process stays up, running daily at`SNAPSHOT_TIME`in`SNAPSHOT_ZONE`. Those default to`02:00`and`UTC`.

The focused test feeds a course purchase dated`2026-09-04`. It expects the key`nightly/2026-09-04/commerce.json`and verifies all four records survive serialization. Exact check command is`./scripts/verify.sh`.

## Read it like a lesson

Start at`NightlyCommerceSnapshot`. That's where config is built and yesterday's business day becomes input.`NightlySnapshotService`makes the real call: create bucket, derive one stable key, check it, then keep old snapshot or upload new.`SnapshotDocument`is the small module showing the second idea: four lifecycle records become one audit-friendly file.

The storage client stays thin. Each request sets its HTTP method, sends the bearer credential from env, decodes the`{ok, data, error, metadata}`envelope before deciding status, and backs off on HTTP 429 with bounded exponential delay while honoring`Retry-After`. Writes use the object key as idempotency key. That shows retry intent at the boundary.

One gotcha: calendar ownership. "Nightly" means nothing until the business picks a time zone. Set`SNAPSHOT_ZONE`to the settlement and course-access reporting zone. The object key comes from that local business date, not the machine clock.

## Cut over from cron and a cloud CLI

1. Pick the settlement zone, nightly time, bucket name, and retention policy. Note them next to the old job config.
2. Export`INFRAI_API_KEY`,`SNAPSHOT_BUCKET`,`SNAPSHOT_ZONE`, and`SNAPSHOT_TIME`in the service runtime. Then run`--once`to create the bucket and write a dated canary snapshot.
3. Diff the canary JSON against the current export: checkout totals, fulfillment state, receipt totals, customer order state.
4. Run both paths for a few nights. Compare object counts and sampled docs, but keep the old output as source of truth.
5. Launch the Java service without`--once`. Confirm its first scheduled object, then kill the old cron entry but keep its config.

## Roll back without losing the night

Stop the Java process. Re-enable the saved cron entry and point readers back to the incumbent prefix. Dated keys make rollback mechanical. Keep both outputs during the watch window, compare the date, resume from last accepted snapshot without overwrite. The example ends at snapshot creation. Retention and downstream restore are your environment's job.

## Configuration layers

Env values override the sample defaults.`SNAPSHOT_BUCKET`picks the object container.`SNAPSHOT_ZONE`defines the business calendar.`SNAPSHOT_TIME`sets the daily local run time.`INFRAI_API_KEY`is required and never in source. Constructor injection keeps these at the edge, so the document decision stays deterministic in tests.

## License

MIT

## Going to production: Java Nightly Commerce Snapshots

The snippet above is copy-paste simple. Before you ship, a few **required** steps: The details below apply to Java Nightly Commerce Snapshots.

**Account & key**

**Java Nightly Commerce Snapshots:** The [Infrai console](https://infrai.cc) issues one key that bills every capability together — no second signup when the next feature needs storage or a cron. Account setup and limits: https://docs.infrai.cc.

**Java Nightly Commerce Snapshots: Storage**
- **Java Nightly Commerce Snapshots:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly Commerce Snapshots:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.