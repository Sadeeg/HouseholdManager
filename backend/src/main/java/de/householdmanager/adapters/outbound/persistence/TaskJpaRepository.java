package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.domain.model.TaskStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, Long> {

    List<TaskJpaEntity> findAllByHouseholdIdOrderByStatusAscPositionAsc(long householdId);

    Optional<TaskJpaEntity> findByHouseholdIdAndId(long householdId, long id);

    List<TaskJpaEntity> findAllByHouseholdIdAndStatusOrderByPositionAsc(long householdId, TaskStatus status);

    int deleteAllByHouseholdIdAndStatus(long householdId, TaskStatus status);
}
