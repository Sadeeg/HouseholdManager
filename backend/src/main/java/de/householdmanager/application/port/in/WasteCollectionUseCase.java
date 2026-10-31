package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import java.util.List;

/**
 * Eingehender Port: die Mülltermine eines Haushalts.
 *
 * <p>Implementierung: {@code application.usecase.WasteCollectionUseCaseImpl}.
 * Der ausgehend Adapter spricht nur diesen Port an; der Tageslauf
 * ({@code adapters.inbound.scheduler.WasteSyncJob}) ebenso.
 *
 * <p>Das Lesen der Übersicht ist eine Lese-Operation. Die Synchronisation ist
 * der einzige Schreibpfad — aus einem Termin entsteht nichts weiter (AC7).
 */
public interface WasteCollectionUseCase {

    /**
     * Übersicht der Seite „Mülltermine": Quelle, Stand und Termine des
     * Haushalts. Der Stand wird nicht aus dem Alter abgeleitet — er ist die
     * Aussage des Servers (R13).
     */
    record WasteOverview(WasteCalendar calendar, List<WasteCollection> collections) {
    }

    /** Übersicht des Haushalts; ohne Mitgliedschaft {@code MembershipRequiredException} (R1, AC10). */
    WasteOverview overview(String userId, long householdId);

    /**
     * Synchronisiert einen Haushalt: das Fenster ab heute, Ersetzen des
     * Bestands, Stand (R5, R6, AC11). Ein fehlgeschlagener Abruf lässt den
     * Bestand unverändert und den Stand als veraltet (R7, AC6).
     */
    void syncHousehold(long householdId);

    // Der Tageslauf (R4, AC2) ruft syncHousehold je Haushalt auf — damit ein
    // fehlerhafter Haushalt die anderen nicht stoppt und jeder Lauf seine
    // eigene Transaktion hat. Die Liste der Haushalte mit Quelle liefert
    // HouseholdUseCase.listHouseholdsWithSource(). See adapters.inbound.scheduler.WasteSyncJob.
}
