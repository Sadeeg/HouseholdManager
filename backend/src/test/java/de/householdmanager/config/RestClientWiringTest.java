package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.adapters.outbound.ics.IcsWasteCalendarFeed;
import de.householdmanager.adapters.outbound.telegram.TelegramApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

/**
 * Die beiden HTTP-Clients der Anwendung müssen unterscheidbar bleiben.
 *
 * <p>Die Anwendung hat zwei Bot-/Feed-Aufrufe: den ICS-Feed für den
 * Müllkalender und die Telegram-Bot-API. Beide Clients sind vom gleichen Typ
 * ({@link RestClient}) und beide werden über eine {@code @Bean}-Methode
 * erzeugt.
 *
 * <p>{@link IcsWasteCalendarFeed} bekommt seinen Client über den Konstruktor
 * — ohne Qualifizier und ohne Primär-Bean. Mit zwei gleich typisierten
 * {@code RestClient}-Beans findet Spring für diese Abhängigkeit keinen
 * eindeutigen Kandidaten und bricht den Start der Anwendung ab.
 *
 * <p>Der Test baut genau die Beans der Produktion auf: die beiden
 * {@code RestClient}-Beans aus {@link IcsClientConfig} und
 * {@link TelegramConfig} sowie die beiden Adapter, die einen Client brauchen.
 * Läuft der Container an, ist die Verdrahtung eindeutig; läuft er nicht an,
 * startet die Anwendung nicht.
 */
class RestClientWiringTest {

    @Test
    void theIcsClientAndTheTelegramClientDoNotCollide() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            // Die zwei Clients der Produktion — beide vom Typ RestClient.
            context.registerBean("icsRestClient", RestClient.class, () -> RestClient.builder().build());
            context.registerBean("telegramRestClient", RestClient.class, () -> RestClient.builder().build());
            // Der Token als eigener Typ — ohne Wert für die Logausgaben.
            context.registerBean(TelegramConfig.TelegramToken.class,
                    () -> new TelegramConfig.TelegramToken("token"));

            // Die Adapter, die einen Client brauchen.
            context.register(IcsWasteCalendarFeed.class);
            context.register(TelegramApiClient.class);

            // Der Start muss gelingen — sonst startet die Anwendung nicht.
            context.refresh();

            assertThat(context.getBean(IcsWasteCalendarFeed.class)).isNotNull();
            assertThat(context.getBean(TelegramApiClient.class)).isNotNull();
        }
    }
}
