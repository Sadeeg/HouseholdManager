package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Eingehendes DTO: Statuswechsel einer Aufgabe (Drag & Drop).
 * {@code position} optional — ohne sie steht die Karte am Ende der Zielbahn.
 */
public record TaskMoveRequest(
        @NotNull(message = "Ein Zielstatus ist erforderlich.")
        TaskStatus status,
        Integer position) {
}
