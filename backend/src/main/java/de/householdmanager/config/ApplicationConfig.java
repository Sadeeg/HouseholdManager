package de.householdmanager.config;

import de.householdmanager.adapters.inbound.scheduler.WasteSyncJob;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.client.RestClient;

/**
 * Composition Root: hier werden die Beans der Anwendung verdrahtet.
 *
 * <p>Die Ports ({@code application.port.in}, {@code application.port.out})
 * werden von den Adaptern implementiert und per Konstruktorinjektion
 * aufgelöst — Spring ist damit der Wiring-Mechanismus, aber nicht die
 * Architekturentscheidung.
 *
 * <p>Die Planung des Tageslaufs (Issue #10, ADR 0005) wird hier freigegeben
 * und mit den konfigurierten Werten eingetragen: {@code @Scheduled} kann
 * keinen Platzhalter in der Zeitzone auflösen, deshalb steht der Lauf hier
 * als CronTask mit der konfigurierten Zone.
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

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Der Client für die ICS-Quelle: hier steht die Abrufzeit (Annahme 7),
     * damit ein hängender Entsorger den Tageslauf nicht blockiert. Der Adapter
     * bekommt den fertigen Client und bleibt dadurch im Test prüfbar.
     */
    @Bean
    public RestClient icsRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${householdmanager.waste.timeout:5s}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addCronTask(new CronTask(wasteSyncJob::syncDaily, new CronTrigger(cron, ZoneId.of(syncZone))));
    }
}
