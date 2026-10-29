package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.WorkspaceUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.WorkspaceSelectionRepository;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Workspace-Auswahl" (ADR 0004): der zuletzt gewählte Haushalt
 * einer Person wird serverseitig gemerkt.
 *
 * <p>Gewählt werden kann nur ein Haushalt, in dem die Person ernannt ist.
 */
@Service
@RequiredArgsConstructor
public class WorkspaceUseCaseImpl implements WorkspaceUseCase {

    private final WorkspaceSelectionRepository workspaceSelectionRepository;
    private final MembershipUseCase membershipUseCase;
    private final HouseholdRepository householdRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> currentWorkspace(String userId) {
        return workspaceSelectionRepository.findHouseholdId(userId);
    }

    @Override
    @Transactional
    public void selectWorkspace(String userId, long householdId) {
        householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Der Haushalt existiert nicht."));
        membershipUseCase.requireMembership(householdId, userId);
        workspaceSelectionRepository.save(userId, householdId);
    }
}
