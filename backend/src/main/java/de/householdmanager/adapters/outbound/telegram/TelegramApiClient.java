package de.householdmanager.adapters.outbound.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.householdmanager.config.TelegramConfig;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Der HTTP-Zugriff auf die Telegram-Bot-API — die einzige Stelle, die die
 * Bot-API kennt (ADR 0007).
 *
 * <p>Der Client kennt den Bot-Token, aber die <strong>fachliche</strong>
 * Bedeutung der Antworten nicht: er liefert Rohdaten, die
 * {@link TelegramBotAdapter} übersetzt. Der Token wird nie protokolliert und
 * nie ausgegeben — er steht nur in der Anfrage-URL.
 *
 * <p>Der Client wirft bewusst die {@code RestClientException} der Bot-API
 * weiter: die Unterscheidung zwischen <em>Zustellfehler</em> (Telegram war
 * nicht erreichbar) und <em>Protokollfehler</em> (Telegram hat die Anfrage
 * abgelehnt) ist fachlich und gehört in den Adapter.
 */
@Component
public class TelegramApiClient {

    /**
     * Die Typen der Antworten. Ohne sie bliebe {@code result} eine
     * {@code LinkedHashMap} — die generische Hülle braucht ihre Parameter.
     */
    private static final ParameterizedTypeReference<ApiResponse<ApiChat>> CHAT_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<ApiResponse<List<ApiUpdate>>> UPDATE_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient http;
    private final TelegramConfig.TelegramToken token;

    public TelegramApiClient(RestClient telegramRestClient, TelegramConfig.TelegramToken token) {
        this.http = telegramRestClient;
        this.token = token;
    }

    /**
     * Die Basis-URL der Bot-API mit dem Token. Der Token steht im Pfad — die
     * URL darf deshalb nicht in eine Logausgabe gelangen.
     */
    private String url(String method) {
        return "https://api.telegram.org/bot" + token.value() + "/" + method;
    }

    /**
     * Die Antwort der Bot-API: ein Hüllenobjekt mit „ok" und „result".
     *
     * <p>Ein HTTP-200 mit {@code "ok": false} ist ein **Protokollfehler** —
     * Telegram hat die Anfrage abgelehnt. Der Client reicht das Ergebnis
     * weiter; der Adapter entscheidet, was es bedeutet.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResponse<T>(boolean ok, T result, @JsonProperty("error_code") Integer errorCode,
            String description) {
    }

    /**
     * Die Chat-Beschreibung der Bot-API: eine Gruppe trägt einen Titel, ein
     * Einzelchat Vor- und Nachnamen.
     *
     * <p>Die Bot-API liefert die Felder in {@code snake_case} — die
     * {@code JsonProperty}-Annotationen bilden sie auf die Felder des Records
     * ab.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiChat(@JsonProperty("id") String id, String title,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String username, String type) {
    }

    /**
     * Die Chat-Beschreibung zu einer Chat-Kennung.
     *
     * @throws org.springframework.web.client.RestClientException bei einem
     *     HTTP- oder Verbindungsfehler.
     */
    public ApiResponse<ApiChat> getChat(String chatId) {
        return http.post()
                .uri(url("getChat"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId))
                .retrieve()
                .body(CHAT_RESPONSE);
    }

    /**
     * Sendet eine Nachricht an einen Chat.
     *
     * @throws org.springframework.web.client.RestClientException bei einem
     *     HTTP- oder Verbindungsfehler.
     */
    public ApiResponse<ApiChat> sendMessage(String chatId, String text) {
        return http.post()
                .uri(url("sendMessage"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .body(CHAT_RESPONSE);
    }

    /**
     * Die ausstehenden Nachrichten des Bots — der Long-Poll.
     *
     * <p>{@code offset} ist die Nummer der nächsten gesuchten Nachricht: die
     * API liefert nur Nachrichten, die noch nicht bestätigt wurden. Mit
     * {@code timeout} wartet Telegram die angegebene Zahl Sekunden auf eine
     * neue Nachricht, bevor es leer antwortet.
     *
     * @throws org.springframework.web.client.RestClientException bei einem
     *     HTTP- oder Verbindungsfehler.
     */
    public ApiResponse<List<ApiUpdate>> getUpdates(long offset, int timeoutSeconds) {
        return http.get()
                .uri(url("getUpdates") + "?offset=" + offset + "&timeout=" + timeoutSeconds)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(UPDATE_RESPONSE);
    }

    /**
     * Eine Nachricht aus Telegram. Nur die Felder, die die Anwendung
     * braucht: die Chat-Kennung, die Kennung der Nachricht und der Text.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiUpdate(@JsonProperty("update_id") Long updateId, ApiMessage message) {
    }

    /** Die Nachricht innerhalb eines Updates. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiMessage(@JsonProperty("chat") ApiChat chat, @JsonProperty("text") String text) {
    }
}
