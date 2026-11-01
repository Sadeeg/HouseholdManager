package de.householdmanager.adapters.inbound.scheduler;

import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Eingehender Adapter: der geplante Wiederholungslauf — stündlich.
 *
 * <p>Der Adapter löst genau einen Use Case aus und enthält <strong>keine</strong>
 * fachliche Logik: der Use Case entscheidet, welche Vermerke erneut
 * versucht werden und wann ein weiterer Versuch ausbleibt (AC26, AC27).
 *
 * <p>Der Zeitpunkt kommt aus der Konfiguration
 * ({@code householdmanager.telegram.retry-cron}); die Freigabe der Planung
 * steht im Composition Root.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetryJob {

    private final NotificationDispatchUseCase notificationDispatchUseCase;

    /** Der Lauf: alle Zustellvermerke, die noch einen Versuch brauchen. */
    public void retryFailed() {
        try {
            notificationDispatchUseCase.retryFailed();
        } catch (RuntimeException failure) {
            log.warn("Der Wiederholungslauf ist fehlgeschlagen: {}", failure.getMessage());
        }
    }
}
