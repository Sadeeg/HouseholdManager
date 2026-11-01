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
    void findRetryCandidates_returnsTheFailedAndOpenDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 1,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false));

        assertThat(adapter.findRetryCandidates()).hasSize(1);
    }

    /** Ein zugestellter Vermerk wird nicht erneut versucht. */
    @Test
    void findRetryCandidates_ignoresDeliveredDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 1,
                NotificationDelivery.Status.DELIVERED, null, true));

        assertThat(adapter.findRetryCandidates()).isEmpty();
    }

    /** AC27: nach drei Fehlversuchen wird der Vermerk abgeschlossen — kein weiterer Versuch. */
    @Test
    void findRetryCandidates_ignoresCompletedDeliveries() {
        adapter.save(delivery(householdA, "chat-1", "Text", 3,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", true));

        assertThat(adapter.findRetryCandidates()).isEmpty();
    }

    /** AC27: unter dem Limit von drei Versuchen wird der Vermerk erneut versucht. */
    @Test
    void findRetryCandidates_keepsDeliveriesBelowTheAttemptLimit() {
        adapter.save(delivery(householdA, "chat-1", "Text", 2,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false));

        assertThat(adapter.findRetryCandidates()).hasSize(1);
    }

    /**
     * Der Erstellzeitpunkt bleibt über die Fortschreibungen des Vermerks
     * unverändert — er ist der Zeitpunkt der Anlage, nicht der des letzten
     * Versuchs.
     */
    @Test
    void save_keepsTheCreationTimestampAcrossUpdates() {
        NotificationDelivery saved = adapter.save(delivery(householdA, "chat-1", "Text", 0,
                NotificationDelivery.Status.PENDING, null, false));
        Instant createdAt = deliveryJpaRepository.findAll().getFirst().getCreatedAt();

        adapter.save(saved.failed(Instant.now(), "nicht erreichbar"));

        assertThat(deliveryJpaRepository.findAll().getFirst().getCreatedAt()).isEqualTo(createdAt);
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

    /**
     * AC26: ein Vermerk, dessen letzter Versuch mehr als eine Stunde
     * zurückliegt, kommt als Kandidat in Frage.
     *
     * <p>Die Fälligkeit selbst entscheidet die Anwendung
     * ({@code NotificationDelivery.isDueAt}) — der Adapter liefert die
     * Kandidaten. Der Test steht deshalb beim Use Case.
     */
    @Test
    void findRetryCandidates_returnsADeliveryThatFailedMoreThanAnHourAgo() {
        adapter.save(delivery(householdA, "chat-1", "Text", 1,
                NotificationDelivery.Status.FAILED, "nicht erreichbar", false,
                Instant.now().minusSeconds(2 * 60 * 60)));

        assertThat(adapter.findRetryCandidates()).hasSize(1);
    }

    private NotificationDelivery delivery(long householdId, String chatId, String content,
            int attempts, NotificationDelivery.Status status, String error, boolean completed) {
        return delivery(householdId, chatId, content, attempts, status, error, completed, null);
    }

    private NotificationDelivery delivery(long householdId, String chatId, String content,
            int attempts, NotificationDelivery.Status status, String error, boolean completed,
            Instant lastAttemptAt) {
        return NotificationDelivery.builder()
                .householdId(householdId)
                .chatId(chatId)
                .content(content)
                .attemptCount(attempts)
                .status(status)
                .lastError(error)
                .lastAttemptAt(lastAttemptAt)
                .completed(completed)
                .build();
    }
}
