package de.householdmanager.adapters.inbound.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import org.junit.jupiter.api.Test;

/**
 * Test des geplanten Benachrichtigungslaufs: der Adapter löst genau einen
 * Use Case aus und enthält keine fachliche Logik (ADR 0005, ADR 0007).
 */
class NotificationDispatchJobTest {

    /** Der 18:00-Uhr-Lauf löst den Benachrichtigungslauf aus. */
    @Test
    void dispatch_triggersTheUseCase() {
        NotificationDispatchUseCase useCase = mock(NotificationDispatchUseCase.class);
        NotificationDispatchJob job = new NotificationDispatchJob(useCase);

        job.dispatch();

        verify(useCase).dispatch();
    }

    /** Der stündliche Wiederholungslauf löst den Wiederholungslauf aus (AC26). */
    @Test
    void retryFailed_triggersTheUseCase() {
        NotificationDispatchUseCase useCase = mock(NotificationDispatchUseCase.class);
        NotificationRetryJob job = new NotificationRetryJob(useCase);

        job.retryFailed();

        verify(useCase).retryFailed();
    }

    /**
     * Ein Fehler des Use Cases endet nicht im Scheduler — der Lauf beendet
     * sich ohne Fehler, damit der nächste Lauf wieder stattfindet (Q15).
     */
    @Test
    void dispatch_swallowsTheFailureOfTheUseCase() {
        NotificationDispatchUseCase useCase = mock(NotificationDispatchUseCase.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("kaputt")).when(useCase).dispatch();
        NotificationDispatchJob job = new NotificationDispatchJob(useCase);

        job.dispatch();

        verify(useCase).dispatch();
    }
}
