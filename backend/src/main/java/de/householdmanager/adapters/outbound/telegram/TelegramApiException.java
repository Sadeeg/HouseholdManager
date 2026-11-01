package de.householdmanager.adapters.outbound.telegram;

/**
 * Technischer Fehler eines Aufrufs der Telegram-Bot-API.
 *
 * <p>Der Bot-Token steht im <strong>Pfad</strong> jeder Anfrage-URL. Die
 * Ausnahmen von Spring nennen diese URL im Klartext — als Meldung oder als
 * Ursache. Beides wäre ein Geheimnis im Log.
 *
 * <p>Diese Ausnahme trägt deshalb nur eine <strong>tokenfreie Meldung</strong>
 * und den HTTP-Status. Der ursprüngliche Fehler wird <strong>nicht</strong>
 * als Ursache weitergereicht: sonst trüge die Ausnahmekette das Token doch
 * wieder. Erhalten bleiben der Stacktrace des Originals und der Status —
 * genug zum Diagnostizieren, ohne das Geheimnis.
 *
 * <p>Die fachliche Bedeutung (Zustellfehler oder Protokollfehler) entscheidet
 * der {@link TelegramBotAdapter}, nicht diese Klasse.
 */
public class TelegramApiException extends RuntimeException {

    /** Der HTTP-Status der Antwort; null, wenn die Verbindung scheiterte. */
    private final Integer httpStatus;

    /**
     * @param message die Meldung — sie nennt weder URL noch Token
     * @param httpStatus der HTTP-Status; null bei einem Verbindungsfehler
     * @param original der ursprüngliche Fehler; sein Stacktrace bleibt, seine
     *     Meldung nicht
     */
    public TelegramApiException(String message, Integer httpStatus, Throwable original) {
        super(message);
        this.httpStatus = httpStatus;
        if (original != null) {
            // Der Stacktrace des Originals bleibt erhalten — die Meldung mit
            // der URL nicht.
            setStackTrace(original.getStackTrace());
        }
    }

    /** Der HTTP-Status der Antwort; null, wenn die Verbindung scheiterte. */
    public Integer httpStatus() {
        return httpStatus;
    }
}
