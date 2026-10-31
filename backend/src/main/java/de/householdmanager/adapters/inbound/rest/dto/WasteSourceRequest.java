package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.WasteCalendar;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Eingehendes DTO: die ICS-Adresse eines Haushalts.
 *
 * <p>Die Grenze ist die Spaltengrenze {@code household.waste_calendar_url};
 * die Meldung nennt sie (AC22). Die eigentliche Fachvalidierung — Pflichtfeld
 * und http/https — übernimmt der Use Case über
 * {@link WasteCalendar#validatedSourceUrl}.
 */
public record WasteSourceRequest(
        @NotBlank(message = "Die ICS-Adresse fehlt.")
        @Size(max = WasteCalendar.MAX_URL_LENGTH,
                message = "Die ICS-Adresse darf höchstens " + WasteCalendar.MAX_URL_LENGTH + " Zeichen lang sein.")
        String url) {
}
