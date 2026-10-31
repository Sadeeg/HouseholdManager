package de.householdmanager;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.adapters.outbound.ics.HostResolver;
import de.householdmanager.adapters.outbound.ics.IcsWasteCalendarFeed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Der volle Anwendungskontext startet — der Test, der es vorher nicht gab.
 *
 * <p>Ohne ihn blieb ein Fehler beim Verdrahten unbemerkt: die Bean des
 * ICS-Adapters war nicht erzeugbar (zwei Konstruktoren), und keine
 * Testsuite lud den Kontext (Issue #16).
 *
 * <p>Die Anwendung braucht für den Start eine Datenbank — hier eine
 * PostgreSQL-Instanz aus Testcontainers.
 */
@SpringBootTest
@ImportTestcontainers
class ApplicationContextTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext context;

    @Test
    void theApplicationStartsAndCreatesItsBeans() {
        assertThat(context.getBean(IcsWasteCalendarFeed.class)).isNotNull();
        assertThat(context.getBean(HostResolver.class)).isNotNull();
    }
}
