package de.householdmanager.adapters.inbound.scheduler;

import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Eingehender Adapter: der geplante Benachrichtigungslauf um 18:00 Uhr.
 *
 * <p>Der Adapter löst genau einen Use Case aus und enthält <strong>keine</strong>
 * Datums- und keine Wochentagslogik (ADR 0005, ADR 0007). Der Zeitpunkt
 * kommt aus der Konfiguration ({@code householdmanager.telegram.dispatch-cron},
 * {@code zone}); die Freigabe der Planung steht im Composition Root.
 *
 * <p>Der Use Case isoliert die Fehler selbst: ein fehlgeschlagener Haushalt
 * stoppt die anderen nicht. Dieser Adapter fängt nur den Rest ab, damit der
 * Lauf nie mit einem Fehler endet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchJob {

    private final NotificationDispatchUseCase notificationDispatchUseCase;

    /** Der Lauf: Wochenübersicht und Erinnerung für den Folgetag. */
    public void dispatch() {
        try {
            notificationDispatchUseCase.dispatch();
        } catch (RuntimeException failure) {
            log.warn("Der Benachrichtigungslauf ist fehlgeschlagen: {}", failure.getMessage());
        }
    }
}
