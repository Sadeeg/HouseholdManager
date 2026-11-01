package de.householdmanager.application.port.out;

/**
 * Ausgehender Port: Telegram als Nachrichtenkanal.
 *
 * <p>Implementierung: {@code adapters.outbound.telegram.TelegramBotAdapter}.
 *
 * <p>Der Port kennt <strong>kein Telegram-Protokoll</strong>: keine Bot-API,
 * keine Bot-Token, keine Aufrufpfade, keine JSON-Felder. Er kann Text an
 * eine Chat-Kennung senden und Name und Art eines Chats liefern. Das
 * Fremdwissen endet an der Adaptergrenze (ADR 0007, analog ADR 0006 für
 * ICS).
 *
 * <p>Der Port kennt auch keine Zeitrechnung und keine Fenster: „morgen" und
 * „kommende Woche" sind Regeln der Anwendung und stehen im Use Case.
 */
public interface TelegramBot {

    /**
     * Die Art eines Chats bei Telegram.
     *
     * <p>Die Übersetzung von Telegram-Werten in diese fachliche Form
     * passiert im Adapter; die Anwendung kennt nur „Gruppe" und
     * „Einzelchat".
     */
    enum ChatKind {
        /** Eine Telegram-Gruppe. */
        GROUP,
        /** Ein privater Einzelchat. */
        PRIVATE
    }

    /**
     * Ist der Bot konfiguriert?
     *
     * <p>Ohne Bot-Token kann der Bot weder Nachrichten entgegennehmen noch
     * senden. Der Adapter weiß, ob ein Token hinterlegt ist — die
     * Anwendung muss das nicht wissen.
     */
    boolean isConfigured();

    /**
     * Sendet eine Nachricht an einen Chat.
     *
     * @throws de.householdmanager.domain.model.TelegramDeliveryException
     *     wenn der Chat nicht erreichbar ist (Netzwerk, Zeitüberschreitung,
     *     Telegram-Serverfehler) — das ist ein **Zustellfehler** und löst
     *     den Wiederholungslauf aus (AC26).
     * @throws de.householdmanager.domain.model.TelegramProtocolException
     *     wenn Telegram die Anfrage fachlich ablehnt (unbekannter Chat,
     *     ungültige Kennung) — das ist ein **fachlicher Fehler** und wird
     *     protokolliert, aber nicht wiederholt.
     */
    void sendMessage(String chatId, String text);

    /**
     * Der Anzeigename eines Chats, wie von Telegram geliefert (AC11).
     *
     * @throws de.householdmanager.domain.model.TelegramDeliveryException
     *     wenn Telegram nicht erreichbar ist.
     */
    String chatName(String chatId);

    /**
     * Die Art eines Chats (AC11).
     *
     * @throws de.householdmanager.domain.model.TelegramDeliveryException
     *     wenn Telegram nicht erreichbar ist.
     */
    ChatKind chatKind(String chatId);
}
