package de.householdmanager.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * R13, R17 und AC6 an der Regel selbst: wann ist ein Stand veraltet?
 *
 * <p>Die Regel liegt im Fachkern („domain importiert nie Spring/JPA/Jackson"),
 * also gehört ihr Test in dieses Paket — ohne Mocks, ohne Spring-Kontext. Die
 * Use-Case-Tests belegen den Weg durch die Anwendung; dieser Test belegt die
 * Grenzfälle, die dort nicht vorkommen: ein Versuch ohne vorherigen
 * erfolgreichen Lauf, ein Versuch genau auf dem letzten erfolgreichen Lauf und
 * ein Versuch davor.
 */
class WasteCalendarStaleTest {

    private static final long HOUSEHOLD = 7L;

    private static final Instant SYNCED = Instant.parse("2026-01-05T03:00:00Z");

    /** Ohne jeden Versuch gibt es keinen veralteten Stand — die Anzeige nennt „Noch keine Synchronisation“ (R16). */
    @Test
    void withoutAnyAttemptTheStandIsNotStale() {
        WasteCalendar calendar = WasteCalendar.builder()
                .householdId(HOUSEHOLD)
                .sourceUrl("https://entsorger.example/feed.ics")
                .build();

        assertThat(calendar.isStale()).isFalse();
    }

    /** Ein erfolgreicher Lauf ohne weiteren Versuch: der Stand ist aktuell. */
    @Test
    void afterASuccessfulRunWithoutAFurtherAttemptTheStandIsNotStale() {
        WasteCalendar calendar = WasteCalendar.builder()
                .householdId(HOUSEHOLD)
                .sourceUrl("https://entsorger.example/feed.ics")
                .lastSyncedAt(SYNCED)
                .lastAttemptAt(SYNCED)
                .build();

        assertThat(calendar.isStale()).isFalse();
    }

    /** Ein Versuch nach dem letzten erfolgreichen Lauf: der Stand ist veraltet (R13). */
    @Test
    void anAttemptAfterTheLastSuccessfulRunMakesTheStandStale() {
        WasteCalendar calendar = WasteCalendar.builder()
                .householdId(HOUSEHOLD)
                .sourceUrl("https://entsorger.example/feed.ics")
                .lastSyncedAt(SYNCED)
                .lastAttemptAt(SYNCED.plusSeconds(60))
                .build();

        assertThat(calendar.isStale()).isTrue();
    }

    /**
     * Der Sonderfall: es gab noch keinen erfolgreichen Lauf, aber einen
     * fehlgeschlagenen. Der Stand ist veraltet — die Anzeige weist den
     * Fehlschlag aus, statt eine leere Quelle zu behaupten (AC6).
     */
    @Test
    void aFailedFirstAttemptMakesTheStandStaleEvenWithoutASuccessfulRun() {
        WasteCalendar calendar = WasteCalendar.builder()
                .householdId(HOUSEHOLD)
                .sourceUrl("https://entsorger.example/feed.ics")
                .lastAttemptAt(SYNCED)
                .build();

        assertThat(calendar.getLastSyncedAt()).isNull();
        assertThat(calendar.isStale()).isTrue();
    }

    /** Ein Versuch vor dem letzten erfolgreichen Lauf: der erfolgreiche Lauf ist der Stand. */
    @Test
    void anAttemptBeforeTheLastSuccessfulRunDoesNotMakeTheStandStale() {
        WasteCalendar calendar = WasteCalendar.builder()
                .householdId(HOUSEHOLD)
                .sourceUrl("https://entsorger.example/feed.ics")
                .lastSyncedAt(SYNCED)
                .lastAttemptAt(SYNCED.minusSeconds(60))
                .build();

        assertThat(calendar.isStale()).isFalse();
    }
}
