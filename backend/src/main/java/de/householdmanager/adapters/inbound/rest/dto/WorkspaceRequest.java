package de.householdmanager.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotNull;

/** Eingehendes DTO: Wahl des Workspace (Haushalt) für die angemeldete Person. */
public record WorkspaceRequest(
        @NotNull(message = "Ein Haushalt muss gewählt werden.")
        Long householdId) {
}
