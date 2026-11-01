package de.householdmanager.adapters.outbound.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import de.householdmanager.config.TelegramConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Test des HTTP-Zugriffs auf die Bot-API.
 *
 * <p>Belegt den Geheimhalt des Tokens: der Fehler der Verbindung nennt weder
 * die Anfrage-URL noch den Token. Die Ausnahme von Spring nennt die URL im
 * Klartext — im Pfad steht der Token. Deshalb kapselt der Client den Fehler.
 */
class TelegramApiClientTest {

    private static final String TOKEN = "123456:ABC-DEF-secret-token";
    private static final String CHAT_ID = "-1001234567890";

    private MockRestServiceServer server;
    private TelegramApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramApiClient(builder.build(), new TelegramConfig.TelegramToken(TOKEN));
    }

    /** Ein HTTP-Fehler wird gekapselt: der Status bleibt, der Token verschwindet. */
    @Test
    void aServerError_isWrappedWithoutTheToken() {
        server.expect(requestTo(containsString("/bot")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sendMessage(CHAT_ID, "Text"))
                .isInstanceOf(TelegramApiException.class)
                .satisfies(failure -> {
                    TelegramApiException api = (TelegramApiException) failure;
                    assertThat(api.httpStatus()).isEqualTo(500);
                    assertThat(api.getMessage()).doesNotContain(TOKEN).doesNotContain("bot" + TOKEN);
                    assertThat(api.getCause()).isNull();
                });
    }

    /** Auch die Ursache des Fehlers darf den Token nicht nennen. */
    @Test
    void theStacktraceOfTheOriginalFailureCarriesNoToken() {
        server.expect(requestTo(containsString("/bot")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.sendMessage(CHAT_ID, "Text"))
                .satisfies(failure -> {
                    StringBuilder all = new StringBuilder();
                    Throwable current = failure;
                    while (current != null) {
                        all.append(current.getMessage()).append('\n');
                        current = current.getCause();
                    }
                    assertThat(all.toString()).doesNotContain(TOKEN).doesNotContain("bot" + TOKEN);
                });
    }
}
