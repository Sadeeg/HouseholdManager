package de.householdmanager.domain.model;

/**
 * Fachfehler: Telegram hat die Anfrage abgelehnt — der Versand ist fachlich
 * fehlgeschlagen.
 *
 * <p>Ein <strong>Protokollfehler</strong>: unbekannter Chat, ungültige
 * Kennung, ein abgelehnter Text. Er wird protokolliert, aber <em>nicht</em>
 * wiederholt — ein weiterer Versuch würde beim selben Fehler scheitern
 * (AC26, AC27).
 */
public class TelegramProtocolException extends RuntimeException {

    public TelegramProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public TelegramProtocolException(String message) {
        super(message);
    }
}
