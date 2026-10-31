package de.householdmanager.adapters.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring-Data-Repository des Stand-Adapters — genau eine Zeile je Haushalt.
 */
interface WasteSyncStateJpaRepository extends JpaRepository<WasteSyncStateJpaEntity, Long> {
}
