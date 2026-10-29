package de.householdmanager.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Eingehendes DTO: Anlegen oder Bearbeiten einer Aufgabe.
 *
 * <p>Nur der Titel ist Pflicht; Inhalt, Zuständige, Frist und Labels sind
 * optional. {@code dueDate} im ISO-Format (das Frontend stellt TT.MM.JJJJ
 * um). Die eigentliche Fachvalidierung übernimmt der Use Case.
 */
public record TaskRequest(
        @NotBlank(message = "Ein Titel ist erforderlich.")
        @Size(max = 200, message = "Der Titel ist zu lang.")
        String title,
        String content,
        String assigneeUserId,
        LocalDate dueDate,
        List<String> labels) {
}
