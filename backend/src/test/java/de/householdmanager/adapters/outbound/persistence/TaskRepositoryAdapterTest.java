package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.householdmanager.domain.model.Label;
import de.householdmanager.domain.model.Task;
import de.householdmanager.domain.model.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test des ausgehenden Adapters gegen eine echte PostgreSQL-Instanz
 * (Testcontainers). Prüft das Mapping, die Positions-Sortierung, das
 * Spalten-Löschen und die Label-Eindeutigkeit (AC17).
 */
@DataJpaTest
@Import(TaskRepositoryAdapter.class)
@ImportTestcontainers
class TaskRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private TaskRepositoryAdapter adapter;

    @Autowired
    private HouseholdJpaRepository householdJpaRepository;

    private long householdA;
    private long householdB;

    @BeforeEach
    void giveHouseholds() {
        // Die Fremdschlüssel der Aufgaben-Tabellen brauchen bestehende Haushalte.
        householdA = givenHousehold("Haushalt A");
        householdB = givenHousehold("Haushalt B");
    }

    private long givenHousehold(String name) {
        return householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name(name)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .createdBy("alice")
                .build()).getId();
    }

    @Test
    void save_persistsTaskAndMapsItBackIncludingLabels() {
        Label label = adapter.saveLabel(householdA, new Label(null, "Hausarbeit"));

        Task saved = adapter.save(Task.builder()
                .householdId(householdA)
                .title("Müll raus")
                .content("**tonnen** raus")
                .status(TaskStatus.TODO)
                .assigneeUserId("bob")
                .dueDate(LocalDate.parse("2026-11-12"))
                .position(0)
                .createdAt(Instant.parse("2026-01-01T10:15:30Z"))
                .labels(List.of(label))
                .build());

        assertThat(saved.getId()).isNotNull();
        Task loaded = adapter.findById(householdA, saved.getId()).orElseThrow();
        assertThat(loaded.getTitle()).isEqualTo("Müll raus");
        assertThat(loaded.getContent()).isEqualTo("**tonnen** raus");
        assertThat(loaded.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(loaded.getAssigneeUserId()).isEqualTo("bob");
        assertThat(loaded.getDueDate()).isEqualTo(LocalDate.parse("2026-11-12"));
        assertThat(loaded.getLabels()).extracting(Label::name).containsExactly("Hausarbeit");
        // Roundtrip Instant <-> TIMESTAMPTZ
        assertThat(loaded.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T10:15:30Z"));
    }

    @Test
    void findByHouseholdAndStatus_sortByStatusThenPosition() {
        givenTask(householdA, "B1", TaskStatus.TODO, 1);
        givenTask(householdA, "A1", TaskStatus.TODO, 0);
        givenTask(householdA, "D1", TaskStatus.DONE, 0);
        givenTask(householdA, "G1", TaskStatus.DOING, 0);

        List<Task> board = adapter.findByHousehold(householdA);
        assertThat(board).extracting(Task::getTitle)
                .containsExactly("A1", "B1", "G1", "D1"); // TODO, DOING, DONE je Position

        assertThat(adapter.findByStatus(householdA, TaskStatus.TODO)).extracting(Task::getPosition)
                .containsExactly(0, 1);
    }

    @Test
    void deleteByStatus_removesOnlyThatColumnAndReportsCount() {
        givenTask(householdA, "Offen", TaskStatus.TODO, 0);
        givenTask(householdA, "Erledigt 1", TaskStatus.DONE, 0);
        givenTask(householdA, "Erledigt 2", TaskStatus.DONE, 1);

        int removed = adapter.deleteByStatus(householdA, TaskStatus.DONE);

        assertThat(removed).isEqualTo(2);
        assertThat(adapter.findByHousehold(householdA)).extracting(Task::getTitle).containsExactly("Offen");
    }

    @Test
    void findById_isScopedToTheHousehold() {
        Task foreign = givenTask(householdB, "Anderer Haushalt", TaskStatus.TODO, 0);

        assertThat(adapter.findById(householdA, foreign.getId())).isEmpty();
    }

    @Test
    void labels_sameNormalizedNameIsOneLabel_firstSpellingWins_uniquePerHousehold() {
        Label created = adapter.saveLabel(householdA, new Label(null, "Bad"));
        Label found = adapter.findLabel(householdA, "bad").orElseThrow();

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.name()).isEqualTo("Bad"); // zuerst gespeicherte Schreibweise

        // AC17: derselbe normalisierte Name zweimal direkt gespeichert → DB-Unique.
        assertThatThrownBy(() -> {
            adapter.saveLabel(householdA, new Label(null, "BAD"));
            adapter.findByHousehold(householdA); // Flush, der die Verletzung sichtbar macht
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theSameLabelNameInAnotherHouseholdIsADifferentLabel() {
        Label inA = adapter.saveLabel(householdA, new Label(null, "Bad"));
        Label inB = adapter.saveLabel(householdB, new Label(null, "Bad"));

        assertThat(inB.id()).isNotEqualTo(inA.id());
    }

    /**
     * Die Grenze des Fachmodells ({@link Label#MAX_NAME_LENGTH}) muss zur
     * Spaltenbreite der Datenbank passen — sie ist der Grund für die Grenze.
     * Ein Name an der Grenze muss in PostgreSQL hineinpassen; einer darüber
     * muss an der Spalte scheitern (und nicht erst am Fachmodell).
     */
    @Test
    void labelName_fitsTheColumnExactlyUpToTheDomainLimit() {
        String atLimit = "x".repeat(Label.MAX_NAME_LENGTH);

        Label saved = adapter.saveLabel(householdA, new Label(null, atLimit));

        assertThat(saved.name()).isEqualTo(atLimit);
        assertThat(adapter.findLabel(householdA, atLimit).orElseThrow().name()).isEqualTo(atLimit);

        String tooLong = "x".repeat(Label.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> {
            adapter.saveLabel(householdA, new Label(null, tooLong));
            adapter.findByHousehold(householdA); // Flush, der die Verletzung sichtbar macht
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Task givenTask(long householdId, String title, TaskStatus status, int position) {
        return adapter.save(Task.builder()
                .householdId(householdId)
                .title(title)
                .status(status)
                .position(position)
                .createdAt(Instant.parse("2026-01-01T10:15:30Z"))
                .labels(List.of())
                .build());
    }
}
