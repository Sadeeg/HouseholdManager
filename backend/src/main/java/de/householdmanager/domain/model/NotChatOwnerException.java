package de.householdmanager.domain.model;

/**
 * Fachfehler: Die Verbindung zu dem Telegram-Chat gehört einer anderen
 * Person.
 *
 * <p>Getrennt werden darf nur die Verbindung, deren freischaltende Person
 * der aktuellen Benutzer ist (AC13). Wer die Verbindung nicht freigeschaltet
 * hat, sieht sie, darf sie aber nicht trennen — das ist ein fachlicher
 * Fehler, kein technischer Ausfall. Der eingehende Adapter übersetzt ihn in
 * 403.
 */
public class NotChatOwnerException extends RuntimeException {

    public NotChatOwnerException(String message) {
        super(message);
    }
}
