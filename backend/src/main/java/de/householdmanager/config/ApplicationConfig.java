package de.householdmanager.config;

import de.householdmanager.adapters.inbound.scheduler.NotificationDispatchJob;
import de.householdmanager.adapters.inbound.scheduler.NotificationRetryJob;
import de.householdmanager.adapters.inbound.scheduler.WasteSyncJob;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Composition Root der Planung: hier wird der Tageslauf freigegeben und mit den
 * konfigurierten Werten eingetragen. {@code @Scheduled} kann keinen Platzhalter in
 * der Zeitzone auflösen, deshalb steht der Lauf hier als CronTask mit der
 * konfigurierten Zone (Issue #10, ADR 0005).
 *
 * <p>Diese Klasse liefert **keine** Beans. Sie hängt am Scheduler-Adapter; würden
 * die Use Cases eine Bean von ihr beziehen, entstünde ein Zyklus
 * ({@code ApplicationConfig → WasteSyncJob → Use Case → diese Klasse}). Die technische
 * Verdrahtung liegt deshalb in {@link ClockConfig} und {@link IcsClientConfig}.
 *
 * <p>Der Benachrichtigungskontext bringt zwei weitere Läufe mit: den Lauf um
 * 18:00 Uhr und den stündlichen Wiederholungslauf (ADR 0007, R14). Sie folgen
 * demselben Muster — der Adapter löst genau einen Use Case aus, die Zeitregeln
 * liegen im Use Case, nicht im Adapter.
 *
 * <p>Die Ports ({@code application.port.in}, {@code application.port.out}) werden von
 * den Adaptern implementiert und per Konstruktorinjektion aufgelöst — Spring ist damit
 * der Wiring-Mechanismus, aber nicht die Architekturentscheidung.
 */
@Configuration
@EnableScheduling
public class ApplicationConfig implements SchedulingConfigurer {

    private final WasteSyncJob wasteSyncJob;
    private final NotificationDispatchJob notificationDispatchJob;
    private final NotificationRetryJob notificationRetryJob;
    private final String cron;
    private final String syncZone;
    private final String dispatchCron;
    private final String retryCron;
    private final String telegramZone;

    public ApplicationConfig(
            WasteSyncJob wasteSyncJob,
            NotificationDispatchJob notificationDispatchJob,
            NotificationRetryJob notificationRetryJob,
            @Value("${householdmanager.waste.sync-cron:0 0 3 * * *}") String cron,
            @Value("${householdmanager.waste.sync-zone:Europe/Berlin}") String syncZone,
            @Value("${householdmanager.telegram.dispatch-cron:0 0 18 * * *}") String dispatchCron,
            @Value("${householdmanager.telegram.retry-cron:0 0 * * * *}") String retryCron,
            @Value("${householdmanager.telegram.zone:Europe/Berlin}") String telegramZone) {
        this.wasteSyncJob = wasteSyncJob;
        this.notificationDispatchJob = notificationDispatchJob;
        this.notificationRetryJob = notificationRetryJob;
        this.cron = cron;
        this.syncZone = syncZone;
        this.dispatchCron = dispatchCron;
        this.retryCron = retryCron;
        this.telegramZone = telegramZone;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(new CronTask(wasteSyncJob::syncDaily, new CronTrigger(cron, ZoneId.of(syncZone))));
        // Der Benachrichtigungslauf (18:00 Uhr) und der Wiederholungslauf
        // (stündlich) — beide in der Zone der Benachrichtigung (ADR 0007).
        taskRegistrar.addCronTask(new CronTask(
                notificationDispatchJob::dispatch, new CronTrigger(dispatchCron, ZoneId.of(telegramZone))));
        taskRegistrar.addCronTask(new CronTask(
                notificationRetryJob::retryFailed, new CronTrigger(retryCron, ZoneId.of(telegramZone))));
    }
}
