package de.householdmanager.adapters.outbound.telegram;

import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.config.TelegramConfig;
import de.householdmanager.domain.model.TelegramDeliveryException;
import de.householdmanager.domain.model.TelegramProtocolException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Ausgehender Adapter: Telegram als Nachrichtenkanal (ADR 0007).
 *
 * <p>Die einzige Stelle, die das Telegram-Protokoll kennt: die Bot-API, ihre
 * Feldnamen und ihre Fehlerbilder. Der Port {@link TelegramBot} bleibt
 * protokollfrei.
 *
 * <p>Die Unterscheidung der Fehler ist fachlich und entscheidet über die
 * Wiederholung:
 *
 * <ul>
 *   <li><strong>Zustellfehler</strong> — HTTP-Fehler, Verbindungsfehler,
 *       Zeitüberschreitung: Telegram war nicht erreichbar. Der Zustellvermerk
 *       zählt den Versuch, der stündliche Lauf versucht es erneut (AC26).
 *   <li><strong>Protokollfehler</strong> — HTTP-200 mit {@code ok: false}:
 *       Telegram hat die Anfrage abgelehnt (unbekannter Chat, ungültige
 *       Kennung). Ein weiterer Versuch würde scheitern — er wird protokolliert
 *       und nicht wiederholt.
 * </ul>
 *
 * <p>Der Bot-Token erscheint in keiner Logausgabe: die Meldungen nennen nur
 * die Chat-Kennung und den Grund.
 */
@Slf4j
@Component
public class TelegramBotAdapter implements TelegramBot {

    private final TelegramApiClient api;
    private final TelegramConfig.TelegramToken token;

    public TelegramBotAdapter(TelegramApiClient api, TelegramConfig.TelegramToken token) {
        this.api = api;
        this.token = token;
    }

    @Override
    public boolean isConfigured() {
        return token.isPresent();
    }

    @Override
    public void sendMessage(String chatId, String text) {
        try {
            TelegramApiClient.ApiResponse<TelegramApiClient.ApiChat> response = api.sendMessage(chatId, text);
            if (response == null || !response.ok()) {
                // Telegram hat die Anfrage abgelehnt — ein weiterer Versuch
                // würde scheitern.
                throw new TelegramProtocolException("Telegram hat die Nachricht abgelehnt: "
                        + reasonOf(response));
            }
        } catch (TelegramApiException failure) {
            throw new TelegramDeliveryException("Telegram ist nicht erreichbar" + statusOf(failure) + ".", failure);
        }
    }

    /**
     * Name und Art des Chats — aus <strong>einem</strong> Aufruf.
     *
     * <p>Die Chat-Beschreibung bei Telegram trägt beides; zwei Aufrufe wären
     * zwei Requests für dieselbe Information.
     */
    @Override
    public ChatInfo chatInfo(String chatId) {
        TelegramApiClient.ApiChat chat = readChat(chatId);
        return new ChatInfo(nameOf(chat), kindOf(chat));
    }

    @Override
    public String chatName(String chatId) {
        return chatInfo(chatId).name();
    }

    @Override
    public ChatKind chatKind(String chatId) {
        return chatInfo(chatId).kind();
    }

    /**
     * Der Anzeigename des Chats: eine Gruppe trägt einen Titel, ein
     * Einzelchat Vor- und Nachnamen; der Benutzername ist der letzte Ausweg.
     */
    private String nameOf(TelegramApiClient.ApiChat chat) {
        String title = chat.title();
        if (title != null && !title.isBlank()) {
            return title;
        }
        String firstName = chat.firstName() == null ? "" : chat.firstName();
        String lastName = chat.lastName() == null ? "" : chat.lastName();
        String name = (firstName + " " + lastName).trim();
        if (!name.isEmpty()) {
            return name;
        }
        return chat.username() == null ? "" : chat.username();
    }

    /** Die Art des Chats — eine Eigenschaft, kein Akzent (AC11). */
    private ChatKind kindOf(TelegramApiClient.ApiChat chat) {
        String type = chat.type();
        if ("group".equals(type) || "supergroup".equals(type) || "channel".equals(type)) {
            return ChatKind.GROUP;
        }
        return ChatKind.PRIVATE;
    }

    /**
     * Die Chat-Beschreibung von Telegram. Ein HTTP-Fehler oder ein abgelehnter
     * Aufruf ist ein Fehler — hier immer ein Zustellfehler, denn die
     * Beschreibung wird bei der Freischaltung gebraucht.
     */
    private TelegramApiClient.ApiChat readChat(String chatId) {
        try {
            TelegramApiClient.ApiResponse<TelegramApiClient.ApiChat> response = api.getChat(chatId);
            if (response == null || !response.ok() || response.result() == null) {
                throw new TelegramProtocolException("Telegram kennt den Chat nicht: " + reasonOf(response));
            }
            return response.result();
        } catch (TelegramApiException failure) {
            throw new TelegramDeliveryException("Telegram ist nicht erreichbar" + statusOf(failure) + ".", failure);
        }
    }

    /**
     * Der HTTP-Status des Fehlers — als Zusatz zur Meldung.
     *
     * <p>Die Meldung des technischen Fehlers ist tokenfrei; der Status kommt
     * aus {@link TelegramApiException}. Die Ausnahme von Spring wird nicht
     * mehr abgefangen: sie nennt die URL mit dem Token.
     */
    private String statusOf(TelegramApiException failure) {
        return failure.httpStatus() == null ? "" : " (HTTP " + failure.httpStatus() + ")";
    }

    /** Der Grund der Ablehnung — ohne den Token, ohne die URL. */
    private String reasonOf(TelegramApiClient.ApiResponse<?> response) {
        if (response == null) {
            return "keine Antwort";
        }
        if (response.description() != null && !response.description().isBlank()) {
            return response.description();
        }
        return "unbekannter Grund";
    }
}
