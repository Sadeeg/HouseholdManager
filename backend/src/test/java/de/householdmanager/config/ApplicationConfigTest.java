package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.householdmanager.adapters.inbound.scheduler.NotificationDispatchJob;
import de.householdmanager.adapters.inbound.scheduler.NotificationRetryJob;
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
 *
 * <p>Der Benachrichtigungskontext bringt zwei weitere Läufe mit: der 18:00-Uhr-Lauf
 * und der stündliche Wiederholungslauf (ADR 0007, R14). Beide werden hier
 * geprüft — sie folgen demselben Muster wie der Tageslauf.
 */
class ApplicationConfigTest {

    private static final String CRON = "0 0 3 * * *";
    private static final String ZONE = "Europe/Berlin";
    private static final String DISPATCH_CRON = "0 0 18 * * *";
    private static final String RETRY_CRON = "0 0 * * * *";

    @Test
    void registersTheDailyRunAtTheConfiguredTimeAndZone() {
        WasteSyncJob job = mock(WasteSyncJob.class);
        ApplicationConfig config = config(job, mock(NotificationDispatchJob.class),
                mock(NotificationRetryJob.class));

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        config.configureTasks(registrar);

        List<CronTask> tasks = registrar.getCronTaskList();
        assertThat(tasks).hasSize(3);
        assertThat(tasks.getFirst().getExpression()).isEqualTo(CRON);

        // 03:00 in Europe/Berlin ist 02:00 UTC — belegt ist die konfigurierte Zone.
        Instant next = tasks.getFirst().getTrigger().nextExecution(lastRunAt(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(next).isEqualTo(Instant.parse("2026-01-01T02:00:00Z"));

        // Der registrierte Lauf ruft den Adapter, nicht eine eigene Umsetzung.
        tasks.getFirst().getRunnable().run();
        verify(job).syncDaily();
    }

    /**
     * Der Benachrichtigungslauf um 18:00 Uhr — der zweite Zeitgeber der
     * Anwendung (ADR 0007). Er trägt seinen eigenen Cron-Ausdruck und die
     * Zone der Benachrichtigung.
     */
    @Test
    void registersTheDispatchRunAtTheConfiguredTime() {
        NotificationDispatchJob job = mock(NotificationDispatchJob.class);
        ApplicationConfig config = config(mock(WasteSyncJob.class), job, mock(NotificationRetryJob.class));

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        config.configureTasks(registrar);

        List<CronTask> tasks = registrar.getCronTaskList();
        assertThat(tasks).hasSize(3);

        CronTask dispatch = tasks.get(1);
        assertThat(dispatch.getExpression()).isEqualTo(DISPATCH_CRON);

        // 18:00 in Europe/Berlin ist 17:00 UTC — belegt ist die konfigurierte Zone.
        Instant next = dispatch.getTrigger().nextExecution(lastRunAt(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(next).isEqualTo(Instant.parse("2026-01-01T17:00:00Z"));

        dispatch.getRunnable().run();
        verify(job).dispatch();
    }

    /**
     * Der stündliche Wiederholungslauf (AC26). Er läuft stündlich — der
     * Cron-Ausdruck trägt die Stunde.
     */
    @Test
    void registersTheRetryRunEveryHour() {
        NotificationRetryJob job = mock(NotificationRetryJob.class);
        ApplicationConfig config = config(mock(WasteSyncJob.class), mock(NotificationDispatchJob.class), job);

        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        config.configureTasks(registrar);

        List<CronTask> tasks = registrar.getCronTaskList();
        assertThat(tasks).hasSize(3);

        CronTask retry = tasks.get(2);
        assertThat(retry.getExpression()).isEqualTo(RETRY_CRON);

        Instant next = retry.getTrigger().nextExecution(lastRunAt(Instant.parse("2026-01-01T00:00:00Z")));
        assertThat(next).isEqualTo(Instant.parse("2026-01-01T01:00:00Z"));

        retry.getRunnable().run();
        verify(job).retryFailed();
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

    private ApplicationConfig config(WasteSyncJob syncJob, NotificationDispatchJob dispatchJob,
            NotificationRetryJob retryJob) {
        return new ApplicationConfig(syncJob, dispatchJob, retryJob,
                CRON, ZONE, DISPATCH_CRON, RETRY_CRON, ZONE);
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
