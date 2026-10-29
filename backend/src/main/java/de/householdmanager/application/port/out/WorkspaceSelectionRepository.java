package de.householdmanager.application.port.out;

import java.util.Optional;

/**
 * Ausgehender Port: Merkung der Workspace-Auswahl pro Person.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.WorkspaceSelectionRepositoryAdapter}.
 */
public interface WorkspaceSelectionRepository {

    Optional<Long> findHouseholdId(String userId);

    /** Setzt oder überschreibt den gewählten Haushalt der Person. */
    void save(String userId, long householdId);
}
