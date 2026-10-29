package de.householdmanager.adapters.outbound.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface LabelJpaRepository extends JpaRepository<LabelJpaEntity, Long> {

    Optional<LabelJpaEntity> findByHouseholdIdAndNormalizedName(long householdId, String normalizedName);
}
