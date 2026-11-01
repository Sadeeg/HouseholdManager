package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.config.ClockConfig;
import de.householdmanager.domain.model.NotificationDelivery;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test des ausgehenden Adapters gegen eine echte PostgreSQL-Instanz
 * (Testcontainers). Prüft das Mapping und die Suche nach den Vermerken, die
 * einen weiteren Versuch brauchen (AC26, AC27).
 */
@DataJpaTest
@Import({NotificationDeliveryRepositoryAdapter.class, ClockConfig.class})
@ImportTestcontainers
class NotificationDeliveryRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private NotificationDeliveryRepositoryAdapter adapter;

    @Autowired
    private NotificationDeliveryJpaRepository deliveryJpaRepository;

    @Autowired
    private HouseholdJpaRepository householdJpaRepository;

    private long householdA;

    @BeforeEach
    void createHousehold() {
        householdA = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus A").createdAt(Instant.now()).createdBy("alice").build()).getId();
    }

    /** Der Vermerk wird gespeichert und wiedergegeben — Inhalt und Zähler bleiben erhalten. */
    @Test
    void save_storesContentAndAttemptCount() {
        adapter.save(delivery(householdA, "chat-1", "Müllabfuhr morgen", 2,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false));

        NotificationDeliveryJpaEntity stored = deliveryJpaRepository.findAll().getFirst();
        assertThat(stored.getContent()).isEqualTo("Müllabfuhr morgen");
        assertThat(stored.getAttemptCount()).isEqualTo(2);
        assertThat(stored.getStatus()).isEqualTo(NotificationDelivery.Status.FAILED);
        assertThat(stored.getLastError()).isEqualTo("nicht erreichbar");
        assertThat(stored.getCompleted()).isFalse();
    }

    /** AC26: ein fehlgeschlagener, offener Vermerk wird erneut versucht. */
    @Test
    void findRetryable_returnsTheFailedAndOpenDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 1,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false));

        assertThat(adapter.findRetryable()).hasSize(1);
    }

    /** Ein zugestellter Vermerk wird nicht erneut versucht. */
    @Test
    void findRetryable_ignoresDeliveredDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 1,
                NotificationDelivery.Status.DELIVERED, null, true));

        assertThat(adapter.findRetryable()).isEmpty();
    }

    /** AC27: nach drei Fehlversuchen wird der Vermerk abgeschlossen — kein weiterer Versuch. */
    @Test
    void findRetryable_ignoresCompletedDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 3,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", true));

        assertThat(adapter.findRetryable()).isEmpty();
    }

    /** AC27: unter dem Limit von drei Versuchen wird der Vermerk erneut versucht. */
    @Test
    void findRetryable_keepsDeliveriesBelowTheAttemptLimit() {
        adapter.save(delivery(householdA, "chat-1", "Text", 2,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false));

        assertThat(adapter.findRetryable()).hasSize(1);
    }

    /** Der Zähler steigt mit jedem Versuch — der Zustand der Wiederholung bleibt erhalten. */
    @Test
    void save_keepsTheRaisedAttemptCount() {
        NotificationDelivery saved = adapter.save(delivery(householdA, "chat-1", "Text", 0,
                NotificationDelivery.Status.PENDING, null, false));

        adapter.save(saved.failed(Instant.now(), "nicht erreichbar"));

        NotificationDeliveryJpaEntity stored = deliveryJpaRepository.findAll().getFirst();
        assertThat(stored.getAttemptCount()).isEqualTo(1);
        assertThat(stored.getStatus()).isEqualTo(NotificationDelivery.Status.FAILED);
    }

    private NotificationDelivery delivery(long householdId, String chatId, String content,
            int attempts, NotificationDelivery.Status status, String error, boolean completed) {
        return NotificationDelivery.builder()
                .householdId(householdId)
                .chatId(chatId)
                .content(content)
                .attemptCount(attempts)
                .status(status)
                .lastError(error)
                .completed(completed)
                .build();
    }
}
