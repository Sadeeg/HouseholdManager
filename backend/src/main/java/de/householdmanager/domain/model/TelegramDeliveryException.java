package de.householdmanager.domain.model;

/**
 * Fachfehler: Telegram war nicht erreichbar — der Versand ist fehlgeschlagen.
 *
 * <p>Ein <strong>Zustellfehler</strong>: Netzwerk, Zeitüberschreitung,
 * Telegram-Serverfehler oder ein nicht erreichbarer Chat. Der Zustellvermerk
 * zählt den Versuch, und der stündliche Lauf versucht es erneut (AC26).
 *
 * <p>Die Abgrenzung zum {@link TelegramProtocolException} ist fachlich
 * relevant: nur der Zustellfehler wird wiederholt (AC26, AC27).
 */
public class TelegramDeliveryException extends RuntimeException {

    public TelegramDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public TelegramDeliveryException(String message) {
        super(message);
    }
}
