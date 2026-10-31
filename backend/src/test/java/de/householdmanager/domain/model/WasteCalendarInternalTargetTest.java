package de.householdmanager.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * R19 (Issue #16): die ICS-Adresse darf nicht auf ein internes Ziel zeigen.
 *
 * <p>Der Test steht im Paket des Fachmodells, weil die Regel dort liegt
 * („domain importiert nie Spring/JPA/Jackson"). Geprüft wird die Adresse als
 * Text: der Host muss kein Literal sein, um abgewiesen zu werden — aber ein
 * Literal wird sofort erkannt.
 */
class WasteCalendarInternalTargetTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1/ics",
        "https://127.0.0.1:8443/ics",
        "http://10.0.0.5/ics",
        "http://172.16.0.1/ics",
        "http://172.31.255.254/ics",
        "http://192.168.1.10/ics",
        "http://169.254.169.254/latest/meta-data",
        "http://0.0.0.0/ics",
        "http://[::1]/ics",
        "http://[fd00::1]/ics",
        "http://[fe80::1]/ics",
        "http://[::ffff:127.0.0.1]/ics",
        "webcal://127.0.0.1/ics",
    })
    void anInternalTargetIsRejectedAndTheReasonIsNamed(String url) {
        assertThatThrownBy(() -> WasteCalendar.validatedSourceUrl(url))
                .isInstanceOf(InvalidWasteCalendarUrlException.class)
                .hasMessageContaining("internes Ziel");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://entsorger.example/ics",
        "https://93.184.216.34/ics",
        "https://[2001:db8::1]/ics",
        "https://172.32.0.1/ics",
        "https://192.169.0.1/ics",
    })
    void aPublicTargetIsAccepted(String url) {
        assertThat(WasteCalendar.validatedSourceUrl(url)).isEqualTo(url);
    }

    /**
     * Die aufgelöste Form einer öffentlichen Adresse bleibt öffentlich — auch
     * mit Scope-Anteil.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "0:0:0:0:0:0:101:101", // 1.1.1.1
        "2001:db8:0:0:0:0:0:1%2",
    })
    void aResolvedPublicAddressIsNotBlocked(String host) {
        assertThat(WasteCalendar.isInternalTarget(host)).isFalse();
    }

    /**
     * Die aufgelöste Form einer IPv4-kompatiblen Adresse (::127.0.0.1) ist
     * intern — das JDK liefert sie ausgeschrieben.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "0:0:0:0:0:0:7f00:1", // ::127.0.0.1
        "0:0:0:0:0:0:a00:1", // ::10.0.0.1
        "0:0:0:0:0:0:0:1", // ::1
        "0:0:0:0:0:0:0:0", // ::
        "fe80:0:0:0:0:0:0:1%2", // Link-Local mit Scope
    })
    void aResolvedInternalAddressIsRecognised(String host) {
        assertThat(WasteCalendar.isInternalTarget(host)).isTrue();
    }

    /**
     * Ein Hostname ist kein Literal: er wird beim Abruf geprüft (der Adapter
     * löst ihn auf). Deshalb bleibt er beim Speichern zulässig.
     */
    @Test
    void aHostnameIsAcceptedAndCheckedWhenItIsFetched() {
        assertThat(WasteCalendar.validatedSourceUrl("https://router.example/ics"))
                .isEqualTo("https://router.example/ics");
    }

    @Test
    void theHostOfAnAddressIsReadWithoutPortAndPath() {
        assertThat(WasteCalendar.hostOf("https://entsorger.example:8443/ics?x=1"))
                .isEqualTo("entsorger.example");
        assertThat(WasteCalendar.hostOf("http://[::1]:8080/ics")).isEqualTo("[::1]");
    }
}
