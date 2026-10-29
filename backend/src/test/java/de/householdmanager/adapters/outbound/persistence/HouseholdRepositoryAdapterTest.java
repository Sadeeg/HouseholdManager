package de.householdmanager.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.domain.model.Household;
import java.time.Instant;
import java.util.List;
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
 * (Testcontainers). Prüft das Mapping zwischen Persistenz- und Fachmodell
 * sowie das Verhalten der Migration.
 */
@DataJpaTest
@Import(HouseholdRepositoryAdapter.class)
@ImportTestcontainers
class HouseholdRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private HouseholdRepositoryAdapter adapter;

    @Test
    void save_persistsAndReturnsDomainModel() {
        Household saved = adapter.save(Household.builder()
                .name("Haushalt Müller")
                .createdAt(Instant.parse("2026-01-01T10:15:30Z"))
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Haushalt Müller");

        List<Household> all = adapter.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getName()).isEqualTo("Haushalt Müller");
        // Roundtrip Instant <-> TIMESTAMPTZ
        assertThat(all.getFirst().getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T10:15:30Z"));
    }

    @Test
    void save_updatesExistingHouseholdAndKeepsCreatedAt() {
        Household created = adapter.save(Household.builder()
                .name("Haushalt Müller")
                .createdAt(Instant.parse("2026-01-01T10:15:30Z"))
                .build());

        // Update-Zweig: id != null, geänderter Name, abweichender createdAt.
        // createdAt muss trotzdem aus der Datenbank stammen.
        adapter.save(Household.builder()
                .id(created.getId())
                .name("Haushalt Schmidt")
                .createdAt(Instant.parse("2026-06-01T08:00:00Z"))
                .build());

        List<Household> all = adapter.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.getFirst().getName()).isEqualTo("Haushalt Schmidt");
        assertThat(all.getFirst().getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T10:15:30Z"));
    }
}
