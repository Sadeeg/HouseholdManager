package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.NotificationDelivery;
import java.util.List;

/**
 * Ausgehender Port: die Zustellvermerke verwalten.
 *
 * <p>Implementierung:
 * {@code adapters.outbound.persistence.NotificationDeliveryRepositoryAdapter}.
 *
 * <p>Je Nachricht und Ziel-Chat ein Vermerk mit Versuchszähler und letztem
 * Zustand (AC26). Der Vermerk ist der persistente Zustand der Wiederholung:
 * der stündliche Lauf sucht alle Vermerke mit
 * {@code attempt_count < 3} und fehlgeschlagenem letzten Versuch.
 */
public interface NotificationDeliveryRepository {

    /** Die Vermerke, die einen weiteren Versuch brauchen (AC26). */
    List<NotificationDelivery> findRetryable();

    NotificationDelivery save(NotificationDelivery delivery);
}
