package education.storefront;

import education.storefront.config.SnapshotConfig;
import education.storefront.domain.CommerceSnapshot;
import education.storefront.infrai.InfraiStorageClient;
import education.storefront.service.NightlySnapshotService;
import education.storefront.snapshot.SnapshotDocument;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class NightlyCommerceSnapshot {
    public static void main(String[] args) {
        SnapshotConfig config = SnapshotConfig.fromEnvironment();
        NightlySnapshotService service = new NightlySnapshotService(
                new InfraiStorageClient(config.apiKey()), new SnapshotDocument(), config.bucket());
        if (args.length > 0 && "--once".equals(args[0])) {
            runForPreviousBusinessDay(service, config);
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(config.zone());
        ZonedDateTime first = now.with(config.nightlyTime());
        if (!first.isAfter(now)) first = first.plusDays(1);
        long initialDelay = Duration.between(now, first).toSeconds();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> runForPreviousBusinessDay(service, config),
                initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        System.out.println("Nightly snapshot scheduled for " + config.nightlyTime() + " " + config.zone());
    }

    private static void runForPreviousBusinessDay(NightlySnapshotService service, SnapshotConfig config) {
        CommerceSnapshot input = CommerceSnapshot.lessonOrder(ZonedDateTime.now(config.zone()).toLocalDate().minusDays(1));
        NightlySnapshotService.Result result = service.snapshot(input);
        System.out.println("snapshot_date=" + input.businessDate() + " result=" + result);
    }
}
