package de.householdmanager.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring-Data-Repository des ausgehenden Adapters.
 *
 * <p>Technisches Detail des Persistenz-Adapters — die Anwendung kennt diesen
 * Typ nicht.
 */
public interface HouseholdJpaRepository extends JpaRepository<HouseholdJpaEntity, Long> {
}
