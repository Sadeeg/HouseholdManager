package de.householdmanager.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistenzmodell des Labels.
 *
 * <p>{@code name} hält die zuerst gespeicherte Schreibweise,
 * {@code normalized_name} die Identität (lower-case, getrimmt) —
 * eindeutig je Haushalt.
 */
@Entity
@Table(name = "label")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "household_id", nullable = false)
    private Long householdId;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 60)
    private String normalizedName;
}
