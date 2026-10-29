package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.Household;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Persistenzmodell des Haushalts.
 *
 * <p>Bleibt im ausgehenden Adapter: das Fachmodell
 * ({@code domain.model.Household}) kennt weder JPA noch Tabellen.
 */
@Entity
@Table(name = "household")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HouseholdJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = Household.MAX_NAME_LENGTH)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Kennung der Erstellerin/des Erstellers — die Haushaltsleitung. */
    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
