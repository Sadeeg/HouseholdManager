package de.householdmanager.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell des Synchronisationsstands je Haushalt.
 *
 * <p>Bleibt im ausgehenden Adapter. Der Haushalt ist der Schlüssel — genau
 * eine Zeile je Haushalt (Muster der Workspace-Auswahl). Die ICS-Quelle steht
 * am Haushalt ({@code household.waste_calendar_url}) und wird hier nicht
 * gespeichert: der Stand ist nur die Auskunft darüber, wann zuletzt gelesen
 * wurde.
 */
@Entity
@Table(name = "waste_sync_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WasteSyncStateJpaEntity {

    /** Kein Generierungs-Key: der Schlüssel ist der Haushalt selbst. */
    @Id
    @Column(name = "household_id")
    private Long householdId;

    /** Zeitpunkt des letzten erfolgreichen Laufs; null, wenn es keinen gab. */
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /** Zeitpunkt des letzten Versuchs, auch eines fehlgeschlagenen. */
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
}
