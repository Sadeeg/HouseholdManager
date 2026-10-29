package de.householdmanager.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell des Haushalts.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine JPA-Importe.
 * Die Abbildung auf die Datenbank übernimmt der ausgehende Adapter
 * ({@code adapters.outbound.persistence.HouseholdRepositoryAdapter}).
 */
@Getter
@Builder
public class Household {

    /**
     * Maximale Länge des Namens — Grenze der Spalte {@code household.name}.
     * <p>
     * Single Source of Truth: Use Case und Persistenzmodell referenzieren
     * diese Konstante, damit die Grenze nicht auseinanderlaufen kann.
     */
    public static final int MAX_NAME_LENGTH = 120;

    private final Long id;
    private final String name;
    private final Instant createdAt;

    public Household withId(Long id) {
        return new Household(id, name, createdAt);
    }
}
