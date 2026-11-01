package de.householdmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Der Client für die Telegram-Bot-API: hier stehen der Bot-Token und die
 * Abrufzeit.
 *
 * <p>Bewusst nicht in {@link ApplicationConfig}: der Adapter hängt an dieser
 * Bean, die Verdrahtung der Planung hängt am Scheduler-Adapter — über den
 * Use Case würde daraus ein Zyklus (R14, ADR 0007).
 *
 * <p>Der Bot-Token ist ein Geheimnis: er kommt aus der Umgebung, wird nie
 * protokolliert und nie in der Oberfläche ausgegeben. Ohne Token ist der
 * Bot nicht konfiguriert — der Eingangs-Adapter startet dann nicht, und die
 * Oberfläche weist darauf hin.
 */
@Configuration
public class TelegramConfig {

    /**
     * Der Bot-Token als eigener Typ — kein nackter {@code String} als Bean.
     *
     * <p>Ein {@code String}-Bean würde mit jeder anderen String-Bean im
     * Kontext kollidieren. Der eigene Typ macht den Token unmissverständlich
     * und verhindert, dass er versehentlich in eine andere Bean gerät.
     */
    public record TelegramToken(String value) {

        /** Ist ein Token hinterlegt? */
        public boolean isPresent() {
            return value != null && !value.isBlank();
        }

        /**
         * Der Token für die Anfrage-URL — oder {@code null}, wenn keiner
         * hinterlegt ist. Der Wert erscheint in <strong>keiner</strong>
         * Logausgabe.
         */
        @Override
        public String toString() {
            return "TelegramToken[" + (isPresent() ? "gesetzt" : "nicht gesetzt") + "]";
        }
    }

    /** Der Bot-Token — aus der Umgebung, nie protokolliert. */
    @Bean
    public TelegramToken telegramBotToken(@Value("${householdmanager.telegram.bot-token:}") String token) {
        return new TelegramToken(token == null ? "" : token.trim());
    }

    /**
     * Der HTTP-Client der Bot-API. Die Zeitgrenze verhindert, dass ein
     * hängender Telegram-Server den Abfragelauf blockiert.
     */
    @Bean
    public RestClient telegramRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${householdmanager.telegram.timeout:10s}") java.time.Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
