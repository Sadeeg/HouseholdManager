package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.WasteCollection;
import java.util.List;

/**
 * Ausgehender Port: die Termine eines Haushalts lesen und ersetzen.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.WasteCollectionRepositoryAdapter}.
 *
 * <p>Der Bestand ist der Anzeigestand des letzten Laufs: jeder Lauf ersetzt
 * ihn vollständig (R5). Ein leerer Bestand ist kein Fehler — die Anzeige
 * nennt den Grund (R15). Für dasselbe Datum und dieselbe Tonnenart entsteht
 * kein zweiter Termin (AC21).
 */
public interface WasteCollectionRepository {

    /** Termine des Haushalts, aufsteigend nach Datum. */
    List<WasteCollection> findByHousehold(long householdId);

    /** Ersetzt den Bestand des Haushalts vollständig — auch mit einer leeren Liste (R8). */
    void replace(long householdId, List<WasteCollection> collections);
}
