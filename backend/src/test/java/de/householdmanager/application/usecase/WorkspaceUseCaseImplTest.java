package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.WorkspaceSelectionRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test des Use Cases „Workspace-Auswahl" gegen gemockte Ports (AC23).
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceUseCaseImplTest {

    @Mock
    private WorkspaceSelectionRepository workspaceSelectionRepository;

    @Mock
    private MembershipUseCase membershipUseCase;

    @Mock
    private HouseholdRepository householdRepository;

    private WorkspaceUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new WorkspaceUseCaseImpl(
                workspaceSelectionRepository, membershipUseCase, householdRepository);
    }

    @Test
    void currentWorkspace_withoutSelection_isEmpty() {
        when(workspaceSelectionRepository.findHouseholdId("alice")).thenReturn(Optional.empty());

        assertThat(useCase.currentWorkspace("alice")).isEmpty();
    }

    @Test
    void currentWorkspace_returnsThePersistedSelection() {
        when(workspaceSelectionRepository.findHouseholdId("alice")).thenReturn(Optional.of(7L));

        assertThat(useCase.currentWorkspace("alice")).contains(7L);
    }

    @Test
    void selectWorkspace_persistsTheSelectionForThePerson() {
        when(householdRepository.findById(7L)).thenReturn(Optional.of(Household.builder()
                .id(7L).name("Haushalt").createdBy("alice").build()));

        useCase.selectWorkspace("alice", 7L);

        verify(workspaceSelectionRepository).save("alice", 7L);
    }

    @Test
    void selectWorkspace_byNonMember_isRejectedAndNotPersisted() {
        when(householdRepository.findById(7L)).thenReturn(Optional.of(Household.builder()
                .id(7L).name("Haushalt").createdBy("bob").build()));
        org.mockito.Mockito.doThrow(new MembershipRequiredException("kein Mitglied"))
                .when(membershipUseCase).requireMembership(eq(7L), anyString());

        assertThatThrownBy(() -> useCase.selectWorkspace("alice", 7L))
                .isInstanceOf(MembershipRequiredException.class);
        verify(workspaceSelectionRepository, never()).save(anyString(), eq(7L));
    }

    @Test
    void selectWorkspace_ofAMissingHousehold_isNotFound() {
        when(householdRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.selectWorkspace("alice", 999L))
                .isInstanceOf(HouseholdNotFoundException.class);
    }
}
