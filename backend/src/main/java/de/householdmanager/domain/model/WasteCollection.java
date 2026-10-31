package de.householdmanager.domain.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell eines Mülltermins.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine JPA-Importe.
 * Ein Termin gehört zu genau einem Haushalt und entsteht aus der ICS-Quelle
 * dieses Haushalts — Tonnenart und Datum werden unverändert übernommen
 * (AC8). Ein Termin ist ganztägig: das Modell trägt ein Datum, keine
 * Uhrzeit (R12).
 *
 * <p>Termine sind der Anzeigestand des letzten erfolgreichen Laufs. Aus
 * ihnen entsteht nichts weiter — keine Aufgabe, keine Ausgabe, keine
 * Benachrichtigung (AC7).
 */
@Getter
@Builder(toBuilder = true)
public class WasteCollection {

    /** Grenze der Spalte {@code waste_collection.waste_type}. */
    public static final int MAX_WASTE_TYPE_LENGTH = 200;

    /** Grenze der Spalte {@code waste_collection.source_uid}. */
    public static final int MAX_SOURCE_UID_LENGTH = 255;

    private final Long id;
    private final long householdId;
    private final LocalDate collectionDate;
    private final String wasteType;

    /** Kennung des Ereignisses in der Quelle; null, wenn die Quelle keine UID liefert. */
    private final String sourceUid;

    public WasteCollection withId(Long newId) {
        return toBuilder().id(newId).build();
    }

    public WasteCollection atHousehold(long householdId) {
        return toBuilder().householdId(householdId).build();
    }
}
