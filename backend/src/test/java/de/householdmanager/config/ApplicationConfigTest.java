package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.householdmanager.adapters.inbound.scheduler.WasteSyncJob;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Test der Freigabe der Planung (AC2): der Zeitpunkt des Tageslaufs kommt aus der
 * Konfiguration — der registrierte Lauf trägt den konfigurierten Cron-Ausdruck und
 * die konfigurierte Zeitzone, nicht die Zeitzone der Ausführung.
 */
class ApplicationConfigTest {

    private static final String CRON = "0 0 3 * * *";
    private static final String ZONE = "Europe/Berlin";

    @Test
    void registersTheDailyRunAtTheConfiguredTimeAndZone() {
        WasteSyncJob job = mock(WasteSyncJob.class);
        ApplicationConfig config = new ApplicationConfig(job, CRON, ZONE);

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        config.configureTasks(registrar);

        List<CronTask> tasks = registrar.getCronTaskList();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.getFirst().getExpression()).isEqualTo(CRON);

        // 03:00 in Europe/Berlin ist 02:00 UTC — belegt ist die konfigurierte Zone.
        Instant next = tasks.getFirst().getTrigger().nextExecution(lastRunAt(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(next).isEqualTo(Instant.parse("2026-01-01T02:00:00Z"));

        // Der registrierte Lauf ruft den Adapter, nicht eine eigene Umsetzung.
        tasks.getFirst().getRunnable().run();
        verify(job).syncDaily();
    }

    /**
     * Der Defekt beim ersten Start: diese Klasse darf keine Bean liefern, die eine
     * Use Case braucht — sonst schließt sie den Zyklus
     * ({@code ApplicationConfig → WasteSyncJob → Use Case → ApplicationConfig}).
     * Die technische Verdrahtung gehört in eigene Config-Klassen.
     */
    @Test
    void providesNoBeansSoNoUseCaseCanDependOnIt() {
        assertThat(Arrays.stream(ApplicationConfig.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Bean.class)))
                .isEmpty();
    }

    private static TriggerContext lastRunAt(Instant lastRun) {
        return new TriggerContext() {
            @Override
            public Instant lastScheduledExecution() {
                return lastRun;
            }

            @Override
            public Instant lastActualExecution() {
                return lastRun;
            }

            @Override
            public Instant lastCompletion() {
                return lastRun;
            }
        };
    }
}
