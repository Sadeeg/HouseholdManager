package de.householdmanager.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Der Client für die ICS-Quelle: hier steht die Abrufzeit (Annahme 7), damit ein
 * hängender Entsorger den Tageslauf nicht blockiert. Der Adapter bekommt den
 * fertigen Client und bleibt dadurch im Test prüfbar.
 *
 * <p>Bewusst nicht in {@link ApplicationConfig}: der Adapter hängt an dieser Bean,
 * die Verdrahtung der Planung hängt am Scheduler-Adapter — über den Use Case würde
 * daraus ein Zyklus.
 */
@Configuration
public class IcsClientConfig {

    /**
     * Der Qualifizier des ICS-Clients — zugleich sein Bean-Name.
     *
     * <p>Die Anwendung hat zwei HTTP-Clients: diesen hier und den der
     * Telegram-Bot-API. Beide sind vom Typ {@link RestClient}, beide werden
     * über eine {@code @Bean}-Methode erzeugt. Ohne Qualifizier findet Spring
     * für die Abhängigkeit keinen eindeutigen Kandidaten und der Start der
     * Anwendung bricht ab.
     */
    public static final String ICS_CLIENT = "icsRestClient";

    /**
     * Der Client für die ICS-Quelle.
     *
     * <p>Der Qualifizier macht den Client eindeutig — siehe
     * {@link #ICS_CLIENT}.
     */
    @Bean
    @Qualifier(ICS_CLIENT)
    public RestClient icsRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${householdmanager.waste.timeout:5s}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
