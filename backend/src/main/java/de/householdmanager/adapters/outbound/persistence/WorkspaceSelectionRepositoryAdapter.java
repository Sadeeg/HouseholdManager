package de.householdmanager.adapters.outbound.persistence;

import de.householdmanager.application.port.out.WorkspaceSelectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Ausgehender Adapter: setzt den ausgehenden Port
 * {@link WorkspaceSelectionRepository} auf JPA um. Die Auswahl wird pro
 * Person überschrieben (Upsert über den Primärschlüssel user_id).
 */
@Repository
@RequiredArgsConstructor
public class WorkspaceSelectionRepositoryAdapter implements WorkspaceSelectionRepository {

    private final WorkspaceSelectionJpaRepository workspaceSelectionJpaRepository;
    private final Clock clock;

    @Override
    public Optional<Long> findHouseholdId(String userId) {
        return workspaceSelectionJpaRepository.findById(userId)
                .map(WorkspaceSelectionJpaEntity::getHouseholdId);
    }

    @Override
    public void save(String userId, long householdId) {
        workspaceSelectionJpaRepository.save(WorkspaceSelectionJpaEntity.builder()
                .userId(userId)
                .householdId(householdId)
                .updatedAt(Instant.now(clock))
                .build());
    }
}
