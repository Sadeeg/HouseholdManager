package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import de.householdmanager.domain.model.WasteCollection;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test des ausgehenden Adapters gegen eine echte PostgreSQL-Instanz
 * (Testcontainers). Prüft das Mapping, die Sortierung der Anzeige und die
 * Eindeutigkeit über Haushalt, Datum und Tonnenart (AC21).
 */
@DataJpaTest
@Import(WasteCollectionRepositoryAdapter.class)
@ImportTestcontainers
class WasteCollectionRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private WasteCollectionRepositoryAdapter adapter;

    @Autowired
    private HouseholdJpaRepository householdJpaRepository;

    private long householdA;
    private long householdB;

    @BeforeEach
    void createHouseholds() {
        householdA = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus A").createdAt(Instant.now()).createdBy("alice").build()).getId();
        householdB = householdJpaRepository.save(HouseholdJpaEntity.builder()
                .name("Haus B").createdAt(Instant.now()).createdBy("alice").build()).getId();
    }

    /** AC21: ein erneuter Lauf legt keinen zweiten Termin für Haushalt, Datum und Tonnenart an. */
    @Test
    void replaceTwiceWithSameTermsKeepsOneTermPerHouseholdDateAndType() {
        List<WasteCollection> terms = List.of(
                term(householdA, LocalDate.of(2026, 1, 12), "Restmüll"),
                term(householdA, LocalDate.of(2026, 1, 19), "Restmüll"));

        adapter.replace(householdA, terms);
        adapter.replace(householdA, terms);

        assertThat(adapter.findByHousehold(householdA)).hasSize(2);
        assertThat(adapter.findByHousehold(householdA))
                .extracting(WasteCollection::getCollectionDate)
                .containsExactly(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 19));
    }

    /** Der Bestand wird vollständig ersetzt — ein leerer Lauf löscht die Termine (R8). */
    @Test
    void replaceOverwritesStoredTermsAndEmptyListRemovesThem() {
        adapter.replace(householdA, List.of(term(householdA, LocalDate.of(2026, 1, 10), "Papier")));
        adapter.replace(householdA, List.of(term(householdA, LocalDate.of(2026, 2, 2), "Glas")));

        assertThat(adapter.findByHousehold(householdA))
                .extracting(WasteCollection::getWasteType)
                .containsExactly("Glas");

        adapter.replace(householdA, List.of());
        assertThat(adapter.findByHousehold(householdA)).isEmpty();
    }

    /** Annahme 8: aufsteigend nach Datum, bei gleichem Datum nach Tonnenart. */
    @Test
    void findByHousehold_sortsByDateThenByWasteType() {
        adapter.replace(householdA, List.of(
                term(householdA, LocalDate.of(2026, 1, 15), "Papier"),
                term(householdA, LocalDate.of(2026, 1, 8), "Restmüll"),
                term(householdA, LocalDate.of(2026, 1, 8), "Bio")));

        assertThat(adapter.findByHousehold(householdA))
                .extracting(WasteCollection::getCollectionDate, WasteCollection::getWasteType)
                .containsExactly(
                        tuple(LocalDate.of(2026, 1, 8), "Bio"),
                        tuple(LocalDate.of(2026, 1, 8), "Restmüll"),
                        tuple(LocalDate.of(2026, 1, 15), "Papier"));
    }

    /** Ein Termin gehört genau einem Haushalt — der andere sieht ihn nicht. */
    @Test
    void termsStayWithinTheirHousehold() {
        adapter.replace(householdA, List.of(term(householdA, LocalDate.of(2026, 1, 8), "Restmüll")));

        assertThat(adapter.findByHousehold(householdB)).isEmpty();
        assertThat(adapter.findByHousehold(householdA)).hasSize(1);
    }

    private static WasteCollection term(long householdId, LocalDate date, String wasteType) {
        return WasteCollection.builder().householdId(householdId).collectionDate(date).wasteType(wasteType).build();
    }
}
