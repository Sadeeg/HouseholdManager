package de.householdmanager.adapters.outbound.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.householdmanager.application.port.out.TelegramBot.ChatKind;
import de.householdmanager.config.TelegramConfig;
import de.householdmanager.domain.model.TelegramDeliveryException;
import de.householdmanager.domain.model.TelegramProtocolException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test des ausgehenden Telegram-Adapters gegen eine vorgespielte Antwort —
 * ohne Netz.
 *
 * <p>Belegt die Unterscheidung der Fehler: ein HTTP-Fehler ist ein
 * <strong>Zustellfehler</strong> (Telegram war nicht erreichbar — er wird
 * wiederholt), ein HTTP-200 mit {@code "ok": false} ist ein
 * <strong>Protokollfehler</strong> (Telegram hat abgelehnt — er wird nicht
 * wiederholt). Ebenso belegt: der Bot-Token erscheint in keiner Logausgabe.
 */
class TelegramBotAdapterTest {

    private static final String TOKEN = "123456:ABC-DEF-secret-token";
    private static final String CHAT_ID = "-1001234567890";

    private MockRestServiceServer server;
    private TelegramBotAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new TelegramBotAdapter(
                new TelegramApiClient(builder.build(), new TelegramConfig.TelegramToken(TOKEN)),
                new TelegramConfig.TelegramToken(TOKEN));
    }

    // --- Der Versand ---------------------------------------------------------

    @Test
    void sendMessage_postsToTheBotApi() {
        server.expect(requestTo("https://api.telegram.org/bot" + TOKEN + "/sendMessage"))
                .andRespond(withSuccess("""
                        {"ok": true, "result": {"id": "1", "type": "private"}}
                        """, MediaType.APPLICATION_JSON));

        adapter.sendMessage(CHAT_ID, "Müllabfuhr morgen");

        server.verify();
    }

    /** Ein HTTP-Fehler ist ein Zustellfehler — er löst den Wiederholungslauf aus. */
    @Test
    void sendMessage_withAServerError_throwsDeliveryException() {
        givenServerError();

        assertThatThrownBy(() -> adapter.sendMessage(CHAT_ID, "Text"))
                .isInstanceOf(TelegramDeliveryException.class);
    }

    /** Ein abgelehnter Aufruf ist ein Protokollfehler — er wird nicht wiederholt. */
    @Test
    void sendMessage_withARejectedRequest_throwsProtocolException() {
        givenRejected();

        assertThatThrownBy(() -> adapter.sendMessage(CHAT_ID, "Text"))
                .isInstanceOf(TelegramProtocolException.class);
    }

    // --- Name und Art des Chats ---------------------------------------------

    @Test
    void chatName_forAGroup_returnsTheTitle() {
        givenOk("""
                {"ok": true, "result": {"id": "1", "type": "group", "title": "WG Flur"}}
                """);

        assertThat(adapter.chatName(CHAT_ID)).isEqualTo("WG Flur");
    }

    @Test
    void chatName_forAPrivateChat_returnsFirstAndLastName() {
        givenOk("""
                {"ok": true, "result": {"id": "1", "type": "private",
                    "first_name": "Alice", "last_name": "Muster"}}
                """);

        assertThat(adapter.chatName(CHAT_ID)).isEqualTo("Alice Muster");
    }

    @Test
    void chatKind_forAGroup_returnsGroup() {
        givenOk("""
                {"ok": true, "result": {"id": "1", "type": "group", "title": "WG Flur"}}
                """);

        assertThat(adapter.chatKind(CHAT_ID)).isEqualTo(ChatKind.GROUP);
    }

    @Test
    void chatKind_forAPrivateChat_returnsPrivate() {
        givenOk("""
                {"ok": true, "result": {"id": "1", "type": "private", "first_name": "Alice"}}
                """);

        assertThat(adapter.chatKind(CHAT_ID)).isEqualTo(ChatKind.PRIVATE);
    }

    /** Ein unbekannter Chat ist ein Protokollfehler. */
    @Test
    void chatName_forAnUnknownChat_throwsProtocolException() {
        givenRejected();

        assertThatThrownBy(() -> adapter.chatName(CHAT_ID))
                .isInstanceOf(TelegramProtocolException.class);
    }

    /** Ein nicht erreichbares Telegram ist ein Zustellfehler. */
    @Test
    void chatName_withAServerError_throwsDeliveryException() {
        givenServerError();

        assertThatThrownBy(() -> adapter.chatName(CHAT_ID))
                .isInstanceOf(TelegramDeliveryException.class);
    }

    // --- Der Token -----------------------------------------------------------

    /** Der Token darf in keiner Meldung stehen — er ist ein Geheimnis. */
    @Test
    void theTokenAppearsInNoMessage() {
        givenServerError();

        Throwable failure = org.junit.jupiter.api.Assertions.assertThrows(
                TelegramDeliveryException.class, () -> adapter.sendMessage(CHAT_ID, "Text"));
        assertThat(failure.getMessage()).doesNotContain(TOKEN);
        assertThat(failure.getCause() == null ? "" : failure.getCause().getMessage())
                .doesNotContain(TOKEN);
    }

    /** Ohne Token ist der Bot nicht konfiguriert. */
    @Test
    void isConfigured_isFalseWithoutAToken() {
        TelegramBotAdapter withoutToken = new TelegramBotAdapter(
                new TelegramApiClient(RestClient.builder().build(), new TelegramConfig.TelegramToken("")),
                new TelegramConfig.TelegramToken(""));

        assertThat(withoutToken.isConfigured()).isFalse();
    }

    @Test
    void isConfigured_isTrueWithAToken() {
        assertThat(adapter.isConfigured()).isTrue();
    }

    // --- Hilfen --------------------------------------------------------------

    private void givenOk(String body) {
        server.expect(requestTo(containsString("/bot")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void givenRejected() {
        server.expect(requestTo(containsString("/bot")))
                .andRespond(withSuccess("""
                        {"ok": false, "error_code": 400, "description": "Bad Request: chat not found"}
                        """, MediaType.APPLICATION_JSON));
    }

    private void givenServerError() {
        server.expect(requestTo(containsString("/bot")))
                .andRespond(withServerError());
    }
}
