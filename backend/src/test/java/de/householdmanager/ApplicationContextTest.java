package de.householdmanager;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.adapters.outbound.ics.IcsWasteCalendarFeed;
import de.householdmanager.adapters.outbound.telegram.TelegramApiClient;
import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Der Start der Anwendung — mit dem ganzen Kontext.
 *
 * <p>Bisher bootete kein Test einen Spring-Kontext. Zwei Verdrahtungsfehler
 * (zwei {@code RestClient}-Beans, zwei Konstruktoren an einem Use Case) sind
 * damit erst im Betrieb aufgefallen — die Anwendung startete nicht. Dieser
 * Test schließt die Lücke: Läuft er grün, startet die Anwendung.
 *
 * <p>Geprüft wird mehr als der Start allein:
 *
 * <ul>
 *   <li>die beiden HTTP-Clients sind eindeutig zuordenbar — die Anwendung
 *       braucht einen für die ICS-Quelle und einen für die Telegram-Bot-API;
 *   <li>der Benachrichtigungs-Use Case ist ohne eigene {@code @Bean}-Methode
 *       baubar — die Anwendung darf keine Konstruktor-Hilfe brauchen;
 *   <li>die Migrationen laufen gegen eine echte PostgreSQL-Instanz und
 *       passen zu den Entities ({@code ddl-auto: validate}).
 * </ul>
 *
 * <p>Der OIDC-Issuer bleibt leer: sonst würde der Test einen Decoder gegen
 * den echten Nextcloud bauen und Netzwerkzugriff brauchen.
 */
@SpringBootTest
@ImportTestcontainers
@TestPropertySource(properties = {
        "nextcloud.oidc.issuer-uri=",
        "nextcloud.oidc.client-id=",
        "nextcloud.base-url=",
        "nextcloud.admin-user=",
        "nextcloud.admin-app-password=",
})
class ApplicationContextTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext context;

    @Test
    void theApplicationStarts() {
        assertThat(context.getBean(IcsWasteCalendarFeed.class)).isNotNull();
        assertThat(context.getBean(TelegramApiClient.class)).isNotNull();
    }

    /**
     * Die beiden HTTP-Clients sind eindeutig: ohne Qualifizier gäbe es zwei
     * Kandidaten vom Typ {@code RestClient} und der Start bricht ab.
     */
    @Test
    void theHttpClientsAreDistinguishable() {
        assertThat(context.getBeanNamesForType(RestClient.class)).hasSize(2);
    }

    /**
     * Der Benachrichtigungs-Use Case ist ohne Zutun des Testers baubar: mit
     * zwei Konstruktoren brauchte die Klasse einen Standardkonstruktor und
     * der Start bricht ab.
     */
    @Test
    void theUseCasesAreBuildableWithoutHelp() {
        assertThat(context.getBean(NotificationDispatchUseCase.class)).isNotNull();
        assertThat(context.getBean(TelegramChatUseCase.class)).isNotNull();
    }
}
