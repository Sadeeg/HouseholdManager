package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.application.port.in.WasteCollectionUseCase.WasteOverview;
import de.householdmanager.domain.model.WasteCalendar;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Instant;
import java.util.List;

/**
 * Ausgehendes DTO: die Übersicht der Seite „Mülltermine".
 *
 * <p>Quelle, Stand und Termine — die Anzeige leitet nichts daraus ab. Der
 * veraltete Stand ist die Aussage der Anwendung (R13), nicht ein Vergleich
 * mit der Uhr.
 */
public record WasteOverviewResponse(
        String sourceUrl,
        Instant lastSyncedAt,
        boolean stale,
        List<WasteCollectionResponse> collections) {

    public static WasteOverviewResponse from(WasteOverview overview) {
        WasteCalendar calendar = overview.calendar();
        return new WasteOverviewResponse(
                calendar.getSourceUrl(),
                calendar.getLastSyncedAt(),
                calendar.isStale(),
                overview.collections().stream().map(WasteCollectionResponse::from).toList());
    }
}
