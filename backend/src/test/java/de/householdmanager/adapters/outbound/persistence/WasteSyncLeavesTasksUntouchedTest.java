package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.WasteCalendarFeed;
import de.householdmanager.application.port.out.WasteCalendarFeed.FeedTerm;
import de.householdmanager.application.usecase.WasteCollectionUseCaseImpl;
import de.householdmanager.domain.model.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
 * AC7: aus einem Mülltermin entsteht keine Aufgabe, keine Ausgabe und keine
 * Benachrichtigung.
 *
 * <p>Der Beweis läuft gegen eine echte PostgreSQL-Instanz: vor dem Lauf liegt
 * eine Aufgabe des Haushalts in der Datenbank, nach dem Lauf liegt sie
 * unverändert dort — und es ist keine weitere Zeile dazugekommen. Ein Fehler,
 * der beim Ersetzen des Müllbestands fremde Zeilen des Haushalts mitnimmt,
 * fällt hier auf.
 */
@DataJpaTest
@Import({
    WasteCollectionRepositoryAdapter.class,
    WasteSyncStateRepositoryAdapter.class,
    HouseholdRepositoryAdapter.class,
})
@ImportTestcontainers
class WasteSyncLeavesTasksUntouchedTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private HouseholdRepositoryAdapter householdRepository;

    @Autowired
    private WasteCollectionRepositoryAdapter collectionRepository;

    @Autowired
    private WasteSyncStateRepositoryAdapter syncStateRepository;

    @Autowired
    private HouseholdJpaRepository householdJpaRepository;

    @Autowired
    private TaskJpaRepository taskJpaRepository;

    private long householdId;

    @BeforeEach
    void createHouseholdWithTask() {
        householdId = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus A")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .createdBy("alice")
                .wasteCalendarUrl("https://entsorger.example/feed.ics")
                .build()).getId();

        taskJpaRepository.save(TaskJpaEntity.builder()
                .householdId(householdId)
                .title("Reste entsorgen")
                .content("")
                .status(TaskStatus.TODO)
                .position(0)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build());
    }

    @Test
    void syncHousehold_leavesTheTasksOfTheHouseholdUnchanged() {
        WasteCollectionUseCaseImpl useCase = new WasteCollectionUseCaseImpl(
                new StubFeed(List.of(new FeedTerm(LocalDate.of(2026, 1, 5), "Restmüll", "uid-1"))),
                collectionRepository,
                syncStateRepository,
                householdRepository,
                new StubMembership(),
                Clock.fixed(Instant.parse("2026-01-01T10:15:30Z"), ZoneOffset.UTC));

        useCase.syncHousehold(householdId);

        assertThat(taskJpaRepository.findAll())
                .extracting(TaskJpaEntity::getTitle, TaskJpaEntity::getStatus)
                .containsExactly(tuple("Reste entsorgen", TaskStatus.TODO));
        assertThat(collectionRepository.findByHousehold(householdId)).hasSize(1);
    }

    /** Der Feed liefert einen Termin — mehr braucht der Test nicht. */
    private record StubFeed(List<FeedTerm> terms) implements WasteCalendarFeed {
        @Override
        public List<FeedTerm> fetch(String url, LocalDate from, LocalDate to) {
            return terms;
        }
    }

    /** Jedes Mitglied darf synchronisieren; der Test prüft die Aufgaben, nicht die Rechte. */
    private static class StubMembership implements MembershipUseCase {
        @Override
        public List<MemberView> listMembers(String currentUserId, long householdId) {
            return List.of(new MemberView("alice", "Alice", true));
        }

        @Override
        public Optional<String> displayNameOf(String userId) {
            return "alice".equals(userId) ? Optional.of("Alice") : Optional.empty();
        }

        @Override
        public MemberView addMember(String currentUserId, long householdId, String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            // Jede Person gilt hier als Mitglied — der Test prüft die Aufgaben.
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return true;
        }
    }
}
