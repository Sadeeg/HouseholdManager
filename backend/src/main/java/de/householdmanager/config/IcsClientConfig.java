package de.householdmanager.config;

import java.time.Duration;
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

    @Bean
    public RestClient icsRestClient(
            RestClient.Builder restClientBuilder,
            @Value("${householdmanager.waste.timeout:5s}") Duration timeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
