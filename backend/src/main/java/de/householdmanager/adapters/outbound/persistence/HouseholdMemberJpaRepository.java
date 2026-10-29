package de.householdmanager.adapters.outbound.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface HouseholdMemberJpaRepository extends JpaRepository<HouseholdMemberJpaEntity, Long> {

    List<HouseholdMemberJpaEntity> findAllByHouseholdIdOrderByIdAsc(long householdId);

    List<HouseholdMemberJpaEntity> findAllByUserId(String userId);

    boolean existsByHouseholdIdAndUserId(long householdId, String userId);

    void deleteAllByHouseholdIdAndUserId(long householdId, String userId);
}
