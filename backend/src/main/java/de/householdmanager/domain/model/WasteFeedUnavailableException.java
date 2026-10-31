package de.householdmanager.domain.model;

/**
 * Fachfehler: die ICS-Quelle ist nicht lesbar.
 *
 * <p>Ein rein technischer Ausfall der Quelle — nicht erreichbar oder nicht als
 * Kalender lesbar. Er ist kein Eingabefehler des Nutzers und wird nicht
 * in einen HTTP-Status übersetzt: der Lauf gilt als fehlgeschlagen, der
 * Bestand des Haushalts bleibt unverändert und der Stand wird als veraltet
 * ausgewiesen (R7, R17, AC6).
 */
public class WasteFeedUnavailableException extends RuntimeException {

    public WasteFeedUnavailableException(String message) {
        super(message);
    }

    public WasteFeedUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
