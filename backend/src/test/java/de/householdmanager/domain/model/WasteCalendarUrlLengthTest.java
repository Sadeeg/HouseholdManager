package de.householdmanager.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * AC22 (R18) an der Grenze: die zulässige Länge der ICS-Adresse.
 *
 * <p>Die vorhandenen Tests belegen nur den Fall „deutlich zu lang"
 * (`HouseholdUseCaseImplTest.setWasteSource_rejectsUrlLongerThanColumnLimit`).
 * An der Grenze selbst ist die Frage offen: der Fachkern misst den
 * <em>getrimmten</em> Text (so misst es auch die Oberfläche — der Zähler und
 * die Speichern-Aktion). Eine Adresse mit einem Leerzeichen am Ende ist also
 * gültig, solange der getrimmte Text die Grenze einhält.
 *
 * <p>Der Test steht im Paket des Fachmodells, weil die Regel dort liegt
 * („domain importiert nie Spring/JPA/Jackson").
 */
class WasteCalendarUrlLengthTest {

    private static final int MAX = WasteCalendar.MAX_URL_LENGTH;

    private static final String PREFIX = "https://a.example/";

    /** Genau an der Grenze: die Adresse ist zulässig und bleibt unverändert. */
    @Test
    void anAddressOfExactlyThePermittedLengthIsAccepted() {
        String url = PREFIX + "x".repeat(MAX - PREFIX.length());

        assertThat(WasteCalendar.validatedSourceUrl(url)).isEqualTo(url);
    }

    /** Ein Zeichen über der Grenze: abgelehnt, und die Meldung nennt die Grenze. */
    @Test
    void anAddressOneCharacterOverTheLimitIsRejectedAndTheLimitIsNamed() {
        String url = PREFIX + "x".repeat(MAX - PREFIX.length() + 1);

        assertThatThrownBy(() -> WasteCalendar.validatedSourceUrl(url))
                .isInstanceOf(InvalidWasteCalendarUrlException.class)
                .hasMessageContaining("höchstens " + MAX + " Zeichen");
    }

    /**
     * Die Grenze misst den getrimmten Text: ein Leerzeichen am Ende zählt
     * nicht, weil es nicht gespeichert wird. Sonst wäre die Oberfläche
     * (Validator, Zähler, Speichern-Aktion) strenger als der Fachkern — oder
     * umgekehrt.
     */
    @Test
    void trailingWhitespaceDoesNotCountTowardsTheLimit() {
        String padded = PREFIX + "x".repeat(MAX - PREFIX.length()) + " ";

        assertThat(padded).hasSize(MAX + 1);
        assertThat(WasteCalendar.validatedSourceUrl(padded)).hasSize(MAX);
    }
}
