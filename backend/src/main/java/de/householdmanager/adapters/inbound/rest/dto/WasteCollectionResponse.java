package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.WasteCollection;
import java.time.LocalDate;

/**
 * Ausgehendes DTO: ein Termin der Liste.
 *
 * <p>Datum und Tonnenart stehen unverändert, wie die Quelle sie liefert
 * (AC8, AC19). Ein Termin ist ganztägig — das DTO trägt kein Uhrzeitfeld
 * (R12).
 */
public record WasteCollectionResponse(LocalDate date, String wasteType) {

    public static WasteCollectionResponse from(WasteCollection collection) {
        return new WasteCollectionResponse(collection.getCollectionDate(), collection.getWasteType());
    }
}
