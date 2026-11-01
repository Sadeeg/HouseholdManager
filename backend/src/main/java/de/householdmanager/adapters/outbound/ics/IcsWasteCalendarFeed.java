package de.householdmanager.adapters.outbound.ics;

import de.householdmanager.application.port.out.WasteCalendarFeed;
import de.householdmanager.config.IcsClientConfig;
import de.householdmanager.domain.model.WasteFeedUnavailableException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RRule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Ausgehender Adapter: eine öffentliche ICS-Quelle (der Entsorger) wird über
 * HTTP gelesen und über ical4j ausgewertet — ADR 0006.
 *
 * <p>Der Adapter liefert Roh-Termine; das Fenster bestimmt die Anwendung
 * (R5), nicht der Adapter. Er ist tolerant gegenüber Formatvarianten (Runde 3,
 * F6): ein Term mit Uhrzeit wird auf seinen Kalendertag abgebildet,
 * Wiederholungen werden auf das Fenster expandiert, Ereignisse ohne
 * verwendbares Startdatum werden ignoriert. Tonnenart und Datum werden
 * unverändert übernommen (AC8, AC19) — das Tool kennt keine feste Artenliste.
 *
 * <p>Die Zeitzone des Feeds wird nicht ausgewertet: die Ereignisse tragen kein
 * TZID. Für Termine mit Uhrzeit gilt die Zeitzone der Anwendung. Der Abruf
 * bricht nach der konfigurierten Zeit ab (Annahme 7), damit ein hängender
 * Entsorger den Tageslauf nicht blockiert.
 */
@Slf4j
@Component
public class IcsWasteCalendarFeed implements WasteCalendarFeed {

    /** Kalendertag-Zeitzone für Termine, die mit Uhrzeit geliefert werden (ADR 0006). */
    private static final ZoneId FALLBACK_ZONE = ZoneId.of("Europe/Berlin");

    private final RestClient http;

    /**
     * Der RestClient kommt aus dem Composition Root, wo die Abrufzeit
     * ({@code householdmanager.waste.timeout}, Annahme 7) gesetzt wird. Der
     * Adapter nimmt den fertigen Client — dadurch ist er im Test über
     * {@code MockRestServiceServer} prüfbar.
     */
    public IcsWasteCalendarFeed(@Qualifier(IcsClientConfig.ICS_CLIENT) RestClient http) {
        this.http = http;
    }

    @Override
    public List<FeedTerm> fetch(String url, LocalDate from, LocalDate to) {
        Calendar calendar = parse(read(url, from, to), url);
        List<FeedTerm> terms = new ArrayList<>();
        for (CalendarComponent component : calendar.getComponents()) {
            if (component instanceof VEvent event) {
                terms.addAll(termsOf(event, from, to));
            }
        }
        return terms;
    }

    private String read(String url, LocalDate from, LocalDate to) {
        log.debug("ICS-Quelle {} lesen für Fenster {} bis {}", url, from, to);
        try {
            return http.get().uri(URI.create(url)).retrieve().body(String.class);
        } catch (RestClientException | IllegalArgumentException failure) {
            throw new WasteFeedUnavailableException("Die ICS-Adresse ist nicht erreichbar: " + url, failure);
        }
    }

    private Calendar parse(String body, String url) {
        try {
            return new CalendarBuilder().build(new StringReader(body));
        } catch (IOException | ParserException failure) {
            throw new WasteFeedUnavailableException("Die Quelle ist nicht als Kalender lesbar: " + url, failure);
        }
    }

    /** Ein Ereignis: Tonnenart und Kennung unverändert, Datum auf den Kalendertag. */
    private List<FeedTerm> termsOf(VEvent event, LocalDate from, LocalDate to) {
        Temporal startRaw = event.getStartDate().map(start -> (Temporal) start.getDate()).orElse(null);
        LocalDate start = asLocalDate(startRaw);
        if (start == null) {
            return List.of(); // Ereignis ohne verwendbares Startdatum wird ignoriert
        }
        String summary = event.getProperty(Property.SUMMARY).map(Property::getValue).orElse(null);
        if (summary == null || summary.isBlank()) {
            return List.of(); // ohne Bezeichnung erfindet das Tool keinen Namen
        }
        String uid = event.getProperty(Property.UID).map(Property::getValue).orElse(null);

        List<LocalDate> dates = new ArrayList<>();
        RRule<?> rule = (RRule<?>) event.getProperty(Property.RRULE).orElse(null);
        if (rule == null || rule.getRecur() == null) {
            dates.add(start);
        } else {
            dates.addAll(expandInWindow(rule, start, from, to));
        }

        return dates.stream()
                .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                .distinct()
                .sorted()
                .map(date -> new FeedTerm(date, summary, uid))
                .toList();
    }

    /**
     * Wiederholung auf das Fenster expandieren — in ical4j 4.x über
     * {@link Recur}, nicht über die aus 3.x bekannte {@code CalendarExpander}.
     */
    @SuppressWarnings("unchecked")
    private List<LocalDate> expandInWindow(RRule<?> rule, Temporal start, LocalDate from, LocalDate to) {
        Recur<Temporal> recur = (Recur<Temporal>) rule.getRecur();
        Period<Temporal> window = new Period<>(from, to);
        List<?> occurrences = (List<?>) recur.getDates(start, window);
        List<LocalDate> dates = new ArrayList<>();
        for (Object occurrence : occurrences) {
            LocalDate date = asLocalDate(occurrence);
            if (date != null) {
                dates.add(date);
            }
        }
        return dates;
    }

    /**
     * Der Wert eines Datums auf den Kalendertag abbilden: Ganztagsdaten
     * kommen als {@code LocalDate}, Termine mit Uhrzeit als zeitbezogene
     * Temporal — sie zählen als ihr Kalendertag (AC8).
     */
    private LocalDate asLocalDate(Object raw) {
        if (raw instanceof LocalDate date) {
            return date;
        }
        if (raw instanceof Instant instant) {
            return instant.atZone(FALLBACK_ZONE).toLocalDate();
        }
        if (raw instanceof Temporal temporal) {
            try {
                return LocalDate.from(temporal);
            } catch (DateTimeException ignored) {
                return null;
            }
        }
        return null;
    }
}
