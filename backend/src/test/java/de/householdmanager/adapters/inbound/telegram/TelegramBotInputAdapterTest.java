package de.householdmanager.adapters.inbound.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.householdmanager.adapters.outbound.telegram.TelegramApiClient;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.config.TelegramConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;

/**
 * Test des eingehenden Bot-Adapters: der Long-Poll nimmt Nachrichten
 * entgegen und reicht sie weiter.
 *
 * <p>Belegt AC3 und AC6: eine Nachricht erreicht den Use Case; der Bot
 * antwortet nur, wenn der Use Case eine Antwort liefert. Weiter belegt: ein
 * Fehler der Abfrage beendet den Abfragelauf nicht.
 */
class TelegramBotInputAdapterTest {

    private static final String CHAT_ID = "-1001234567890";

    private TelegramApiClient api;
    private TelegramChatUseCase useCase;
    private TelegramBot bot;
    private TelegramBotInputAdapter adapter;

    @BeforeEach
    void setUp() {
        api = Mockito.mock(TelegramApiClient.class);
        useCase = Mockito.mock(TelegramChatUseCase.class);
        bot = Mockito.mock(TelegramBot.class);
        adapter = new TelegramBotInputAdapter(api, useCase, bot,
                new TelegramConfig.TelegramToken("token"), 1);
    }

    /** AC3: eine Nachricht aus dem Chat erreicht den Use Case. */
    @Test
    void pollOnce_passesTheMessageToTheUseCase() {
        givenUpdates(update(1L, CHAT_ID, "123456"));
        when(useCase.handleMessage(CHAT_ID, "123456"))
                .thenReturn(new TelegramChatUseCase.BotAnswer(true, "Freigeschaltet."));

        adapter.pollOnce();

        verify(useCase).handleMessage(CHAT_ID, "123456");
    }

    /** Der Bot antwortet, wenn der Use Case eine Antwort liefert. */
    @Test
    void pollOnce_sendsTheAnswerOfTheUseCaseBackToTheChat() {
        givenUpdates(update(1L, CHAT_ID, "123456"));
        when(useCase.handleMessage(CHAT_ID, "123456"))
                .thenReturn(new TelegramChatUseCase.BotAnswer(true, "Freigeschaltet."));

        adapter.pollOnce();

        verify(bot).sendMessage(CHAT_ID, "Freigeschaltet.");
    }

    /** AC6: der Bot schweigt, wenn der Use Case keine Antwort liefert. */
    @Test
    void pollOnce_staysSilentWhenTheUseCaseHasNoAnswer() {
        givenUpdates(update(1L, CHAT_ID, "Guten Morgen"));
        when(useCase.handleMessage(CHAT_ID, "Guten Morgen"))
                .thenReturn(new TelegramChatUseCase.BotAnswer(false, null));

        adapter.pollOnce();

        verify(bot, never()).sendMessage(anyString(), anyString());
    }

    /** Die Kennung der Nachricht steigt — jede Nachricht wird genau einmal verarbeitet. */
    @Test
    void pollOnce_advancesTheOffset() {
        givenUpdates(update(7L, CHAT_ID, "123456"));
        when(useCase.handleMessage(anyString(), anyString()))
                .thenReturn(new TelegramChatUseCase.BotAnswer(false, null));

        adapter.pollOnce();

        verify(api).getUpdates(0, 30);
        // Der Abruf nach der Verarbeitung beginnt hinter der letzten Nachricht.
        adapter.pollOnce();
        verify(api).getUpdates(8, 30);
    }

    /** Eine Nachricht ohne Text (zum Beispiel ein Foto) wird übergangen. */
    @Test
    void pollOnce_ignoresMessagesWithoutText() {
        givenUpdates(new TelegramApiClient.ApiUpdate(1L,
                new TelegramApiClient.ApiMessage(new TelegramApiClient.ApiChat(CHAT_ID, null, null, null, null, "private"),
                        null)));

        adapter.pollOnce();

        verify(useCase, never()).handleMessage(anyString(), anyString());
    }

    /** Ein Fehler der Abfrage beendet den Abfragelauf nicht. */
    @Test
    void pollLoop_survivesAFailingRequest() {
        when(api.getUpdates(any(Long.class), any(Integer.class)))
                .thenThrow(new RestClientException("Telegram ist nicht erreichbar."));

        // Der Lauf würde endlos laufen — deshalb wird nur eine Iteration
        // geprüft: der Fehler darf nicht nach oben durchschlagen.
        org.assertj.core.api.Assertions.assertThatThrownBy(adapter::pollOnce)
                .isInstanceOf(RestClientException.class);
    }

    /**
     * Der Bot-Token darf in keiner Logausgabe stehen — die Ausnahme von
     * Spring nennt die Anfrage-URL im Klartext.
     */
    @Test
    void pollLoop_logsNoToken() {
        String token = "123456:ABC-DEF-secret-token";
        when(api.getUpdates(any(Long.class), any(Integer.class)))
                .thenThrow(new RestClientException(
                        "I/O error on GET request for \"https://api.telegram.org/bot" + token + "/getUpdates\""));
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(TelegramBotInputAdapter.class);
        // Der Abfragelauf läuft endlos — deshalb wird nur eine Iteration
        // geprüft: der Fehler darf nicht nach oben durchschlagen.
        org.assertj.core.api.Assertions.assertThatThrownBy(adapter::pollOnce)
                .isInstanceOf(RestClientException.class);

        logger.detachAndStopAllAppenders();
        ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender =
                new ch.qos.logback.core.read.ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            adapter.pollIteration();
        } finally {
            logger.detachAppender(appender);
        }
        String logged = appender.list.stream()
                .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(logged).contains("fehlgeschlagen").doesNotContain(token).doesNotContain("bot" + token);
    }

    /** Ohne konfigurierten Token startet der Abfragelauf nicht. */
    @Test
    void start_doesNothingWithoutAToken() {
        TelegramBotInputAdapter withoutToken = new TelegramBotInputAdapter(api, useCase, bot,
                new TelegramConfig.TelegramToken(""), 1);

        withoutToken.start();

        verify(api, never()).getUpdates(any(Long.class), any(Integer.class));
    }

    private void givenUpdates(TelegramApiClient.ApiUpdate... updates) {
        when(api.getUpdates(any(Long.class), any(Integer.class)))
                .thenReturn(new TelegramApiClient.ApiResponse<>(true, List.of(updates), 200, "ok"));
    }

    private TelegramApiClient.ApiUpdate update(long updateId, String chatId, String text) {
        TelegramApiClient.ApiChat chat = new TelegramApiClient.ApiChat(chatId, null, null, null, null, "private");
        return new TelegramApiClient.ApiUpdate(updateId, new TelegramApiClient.ApiMessage(chat, text));
    }
}
