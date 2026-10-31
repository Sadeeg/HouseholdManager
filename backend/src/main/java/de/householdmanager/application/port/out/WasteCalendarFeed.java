package de.householdmanager.application.port.out;

import java.time.LocalDate;
import java.util.List;

/**
 * Ausgehender Port: die Anwendung kann eine ICS-Quelle lesen, aber nicht
 * <em>wie</em> — die Auswertung der Kalenderdatei gehört zum Adapter.
 *
 * <p>Implementierung: {@code adapters.outbound.ics.IcsWasteCalendarFeed}.
 *
 * <p>Die Anwendung definiert das Zeitfenster (R5), nicht der Adapter: der
 * Feed liefert das ganze Kalenderjahr, die Anwendung wählt das Fenster. Ein
 * Adapter, der die Quelle nicht lesen kann, wirft
 * {@code WasteFeedUnavailableException} — der Bestand des Haushalts bleibt
 * dann unverändert (R7).
 */
public interface WasteCalendarFeed {

    /** Ein Term, wie die Quelle ihn liefert: Datum, Tonnenart (unverändert), Kennung. */
    record FeedTerm(LocalDate date, String wasteType, String sourceUid) {
    }

    /** Termine der Quelle im Fenster vom (inclusive) bis zum (inclusive). */
    List<FeedTerm> fetch(String url, LocalDate from, LocalDate to);
}
