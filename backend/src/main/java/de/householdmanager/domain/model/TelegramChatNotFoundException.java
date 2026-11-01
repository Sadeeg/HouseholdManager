package de.householdmanager.domain.model;

/**
 * Fachfehler: Ein Telegram-Chat mit dieser Kennung ist für den Haushalt
 * nicht freigeschaltet.
 *
 * <p>Der eingehende Adapter übersetzt ihn in 404. Die Meldung verrät
 * <em>nicht</em>, ob die Chat-ID zu einem anderen Haushalt gehört — die
 * Antwort bleibt in beiden Fällen gleich.
 */
public class TelegramChatNotFoundException extends RuntimeException {

    public TelegramChatNotFoundException(String message) {
        super(message);
    }
}
