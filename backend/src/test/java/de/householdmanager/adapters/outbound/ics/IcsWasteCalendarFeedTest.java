package de.householdmanager.adapters.outbound.ics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import de.householdmanager.application.port.out.WasteCalendarFeed.FeedTerm;
import de.householdmanager.domain.model.WasteFeedUnavailableException;
import java.net.InetAddress;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test des ausgehenden Adapters (AC8, AC19) gegen eine vorgespielte Antwort —
 * ohne Netz. Der Adapter liefert Roh-Termine; das Fenster kommt von der
 * Anwendung.
 */
class IcsWasteCalendarFeedTest {

    private static final String URL = "https://entsorger.example/ics";
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = FROM.plusDays(42);

    private MockRestServiceServer server;
    private IcsWasteCalendarFeed feed;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        // Der Host löst auf eine öffentliche Adresse auf — der Test kommt ohne
        // Netz aus (Issue #16).
        feed = new IcsWasteCalendarFeed(builder.build(), host -> new InetAddress[] {
            InetAddress.getByName("93.184.216.34")
        });
    }

    /**
     * R19 (Issue #16): ein Ziel im Netz des Servers wird nicht abgerufen. Der
     * Hostname zeigt auf eine interne Adresse — die Prüfung des Textes allein
     * würde das nicht sehen.
     */
    @Test
    void fetch_refusesAnInternalTarget_withoutSendingARequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer internalServer = MockRestServiceServer.bindTo(builder).build();
        IcsWasteCalendarFeed internal = new IcsWasteCalendarFeed(builder.build(), host ->
                new InetAddress[] {InetAddress.getByName("192.168.1.10")});

        assertThatThrownBy(() -> internal.fetch(URL, FROM, TO))
                .isInstanceOf(WasteFeedUnavailableException.class)
                .hasMessageContaining("internes Ziel");
        internalServer.verify(); // es ging kein Request raus
    }

    // --- AC8: Datum und Tonnenart unverändert, Wiederholung im Fenster ------

    @Test
    void fetch_keepsSummaryAndDateAsTheFeedDeliversThem() {
        given("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:74cce590-56d
                SUMMARY:Bio (abfall) / Biotonne
                DTSTART;VALUE=DATE:20260115
                END:VEVENT
                END:VCALENDAR
                """);

        assertThat(feed.fetch(URL, FROM, TO)).containsExactly(
                new FeedTerm(LocalDate.of(2026, 1, 15), "Bio (abfall) / Biotonne", "74cce590-56d"));
    }

    @Test
    void fetch_expandsRepeatsInsideTheWindow() {
        given("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:weekly-1
                SUMMARY:Restmüll
                DTSTART;VALUE=DATE:20260107
                RRULE:FREQ=WEEKLY;BYDAY=WE;COUNT=4
                END:VEVENT
                END:VCALENDAR
                """);

        assertThat(feed.fetch(URL, FROM, TO))
                .extracting(FeedTerm::date)
                .containsExactly(
                        LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14),
                        LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28));
    }

    /** Der Repeat reicht über das Fenster hinaus — nur die Termine im Fenster zählen. */
    @Test
    void fetch_stopsExpansionAtTheWindow() {
        given("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:weekly-2
                SUMMARY:Papier
                DTSTART;VALUE=DATE:20260120
                RRULE:FREQ=WEEKLY;BYDAY=TH;COUNT=6
                END:VEVENT
                END:VCALENDAR
                """);

        List<FeedTerm> terms = feed.fetch(URL, FROM, TO);
        assertThat(terms).extracting(FeedTerm::date).containsExactly(
                LocalDate.of(2026, 1, 22), LocalDate.of(2026, 1, 29),
                LocalDate.of(2026, 2, 5), LocalDate.of(2026, 2, 12));
        assertThat(terms).allMatch(term -> !term.date().isBefore(FROM) && !term.date().isAfter(TO));
    }

    /** Ein Term mit Uhrzeit gilt als sein Kalendertag (AC8). */
    @Test
    void fetch_mapsDateTimeTermToItsCalendarDay() {
        given("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:timed-1
                SUMMARY:Gelbe Tonne
                DTSTART;TZID=Europe/Berlin:20260122T070000
                END:VEVENT
                END:VCALENDAR
                """);

        assertThat(feed.fetch(URL, FROM, TO))
                .extracting(FeedTerm::date)
                .containsExactly(LocalDate.of(2026, 1, 22));
    }

    /** Ereignisse ohne Startdatum oder ohne Bezeichnung werden ignoriert. */
    @Test
    void fetch_ignoresEventsWithoutDateOrName() {
        given("""
                BEGIN:VCALENDAR
                VERSION:2.0
                BEGIN:VEVENT
                UID:no-date
                SUMMARY:Restmüll
                END:VEVENT
                BEGIN:VEVENT
                UID:no-name
                DTSTART;VALUE=DATE:20260120
                END:VEVENT
                END:VCALENDAR
                """);

        assertThat(feed.fetch(URL, FROM, TO)).isEmpty();
    }

    // --- Fehler: die Quelle ist nicht lesbar ---------------------------------

    @Test
    void fetch_whenSourceIsNotReachable_throwsFeedUnavailable() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> feed.fetch(URL, FROM, TO))
                .isInstanceOf(WasteFeedUnavailableException.class);
    }

    @Test
    void fetch_whenBodyIsNotACalendar_throwsFeedUnavailable() {
        given("<html>kein Kalender</html>");

        assertThatThrownBy(() -> feed.fetch(URL, FROM, TO))
                .isInstanceOf(WasteFeedUnavailableException.class);
    }

    private void given(String body) {
        server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.TEXT_PLAIN));
    }
}
