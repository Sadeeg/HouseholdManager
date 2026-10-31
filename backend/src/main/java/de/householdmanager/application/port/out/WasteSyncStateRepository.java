package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.WasteCalendar;
import java.time.Instant;
import java.util.Optional;

/**
 * Ausgehender Port: der Stand der Synchronisation je Haushalt.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.WasteSyncStateRepositoryAdapter}.
 *
 * <p>Der Stand ist die einzige Aussage darüber, wie aktuell die angezeigten
 * Termine sind (AC5). Er wird auch bei einem fehlgeschlagenen Lauf
 * geschrieben — sonst wäre der Fehler nicht sichtbar (AC6, R13).
 */
public interface WasteSyncStateRepository {

    /** Stand des Haushalts; leer, wenn noch nie ein Stand geschrieben wurde. */
    Optional<WasteCalendar> findByHousehold(long householdId);

    /**
     * Schreibt den Stand. Die Quelle gehört zum Haushalt und wird hier nicht
     * gespeichert — das Persistenzmodell kennt nur die zwei Zeitstempel.
     */
    WasteCalendar save(long householdId, Instant lastSyncedAt, Instant lastAttemptAt);
}
