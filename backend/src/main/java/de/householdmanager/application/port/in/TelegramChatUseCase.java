package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.TelegramChat;
import java.util.List;
import java.util.Optional;

/**
 * Eingehender Port: die Telegram-Chats eines Haushalts.
 *
 * <p>Implementierung: {@code application.usecase.TelegramChatUseCaseImpl}.
 * Der REST-Adapter ({@code adapters.inbound.rest.TelegramChatController}) und
 * der Bot-Eingang ({@code adapters.inbound.telegram.TelegramBotInputAdapter})
 * sprechen nur diesen Port an.
 *
 * <p>Der Freischalt-Code ist ein Anzeigewert: er wird im Chat an den Bot
 * gesendet, nicht über die Oberfläche. Die Oberfläche zeigt ihn und kopiert
 * ihn (AC1); die Freischaltung selbst passiert im Telegram-Chat (AC3).
 */
public interface TelegramChatUseCase {

    /**
     * Ist der Telegram-Bot konfiguriert?
     *
     * <p>Ohne Bot-Token lässt sich kein Chat freischalten: der Bot nimmt
     * keine Nachrichten entgegen und der Code würde ins Leere laufen. Die
     * Oberfläche weist darauf hin, statt eine wirkungslose Aktion anzubieten.
     */
    boolean isBotConfigured();

    /**
     * Der Freischalt-Code eines Haushalts. Beim ersten Anfordern wird er
     * erzeugt; danach bleibt er unverändert (AC1, AC2). Der Code ist für
     * jedes Mitglied sichtbar.
     *
     * <p>Ohne konfigurierten Bot wird <strong>kein</strong> Code erzeugt:
     * der Code liefe ins Leere, weil kein Bot ihn annimmt. In dem Fall ist
     * das Ergebnis leer — die Oberfläche weist stattdessen auf den fehlenden
     * Bot-Token hin.
     *
     * @return der Code des Haushalts oder leer, wenn kein Bot konfiguriert ist.
     */
    Optional<String> code(String userId, long householdId);

    /** Die freigeschalteten Chats des Haushalts (AC11, AC12). */
    List<ChatView> listChats(String userId, long householdId);

    /**
     * Ein freigeschalteter Chat als Anzeigewert.
     *
     * <p>Die Oberfläche nennt das freischaltende <strong>Mitglied</strong>, nicht
     * die technische Kennung — deshalb trägt die Sicht den aufgelösten
     * Anzeigenamen. Lässt er sich nicht auflösen, bleibt die Kennung stehen.
     */
    record ChatView(
            String chatId,
            String chatName,
            TelegramChat.ChatKind kind,
            String activatedBy,
            String activatedByName) {
    }

    /**
     * Verarbeitet eine Nachricht aus einem Telegram-Chat.
     *
     * <p>Der Aufrufer hat den Text bereits aufbereitet; die Anwendung
     * entscheidet, ob es sich um einen Freischalt-Code handelt (AC4, AC5)
     * und schaltet den Chat dann frei (AC3). Alle anderen Nachrichten werden
     * ignoriert — der Bot antwortet nicht (AC6).
     *
     * @param chatId die Kennung des Chats bei Telegram.
     * @param text der aufbereitete Text der Nachricht.
     * @return die Antwort des Bots: leer, wenn der Bot nicht antwortet.
     */
    BotAnswer handleMessage(String chatId, String text);

    /**
     * Trennt eine eigene Verbindung (AC13). Eine fremde Verbindung wird mit
     * {@code NotChatOwnerException} abgelehnt (AC14).
     */
    void disconnect(String userId, long householdId, String chatId);

    /**
     * Die Antwort des Bots auf eine Nachricht aus einem Chat.
     *
     * @param recognized ob die Nachricht als Freischalt-Code erkannt wurde.
     * @param message der Text, den der Bot in den Chat schreibt; leer, wenn
     *     der Bot nicht antwortet.
     */
    record BotAnswer(boolean recognized, String message) {

        /** Antwortet der Bot in diesem Chat? */
        public boolean hasMessage() {
            return message != null && !message.isBlank();
        }
    }
}
