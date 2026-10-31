package de.householdmanager.config;

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
 * <p>Die Ports ({@code application.port.in}, {@code application.port.out}) werden von
 * den Adaptern implementiert und per Konstruktorinjektion aufgelöst — Spring ist damit
 * der Wiring-Mechanismus, aber nicht die Architekturentscheidung.
 */
@Configuration
@EnableScheduling
public class ApplicationConfig implements SchedulingConfigurer {

    private final WasteSyncJob wasteSyncJob;
    private final String cron;
    private final String syncZone;

    public ApplicationConfig(
            WasteSyncJob wasteSyncJob,
            @Value("${householdmanager.waste.sync-cron:0 0 3 * * *}") String cron,
            @Value("${householdmanager.waste.sync-zone:Europe/Berlin}") String syncZone) {
        this.wasteSyncJob = wasteSyncJob;
        this.cron = cron;
        this.syncZone = syncZone;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(new CronTask(wasteSyncJob::syncDaily, new CronTrigger(cron, ZoneId.of(syncZone))));
    }
}
