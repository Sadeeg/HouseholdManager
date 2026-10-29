package de.householdmanager.application.port.in;

import java.util.Optional;

/**
 * Eingehender Port: Workspace-Auswahl — der zuletzt gewählte Haushalt einer
 * Person (ADR 0004).
 *
 * <p>Implementierung: {@code application.usecase.WorkspaceUseCaseImpl}.
 */
public interface WorkspaceUseCase {

    /** Der gemerkte Haushalt der Person, oder {@code empty}, wenn sie nie einen wählte. */
    Optional<Long> currentWorkspace(String userId);

    /** Merkt den Haushalt — nur wenn die Person Mitglied dieses Haushalts ist. */
    void selectWorkspace(String userId, long householdId);
}
