package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.UserDirectory;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdMember;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotHouseholdAdminException;
import de.householdmanager.domain.model.UnknownUserException;
import de.householdmanager.domain.model.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Mitgliedschaft".
 *
 * <p>Die Haushaltsleitung (Ersteller/in des Haushalts, {@code
 * household.created_by}) ernennt und entfernt Mitglieder. Nur Personen aus
 * dem Nutzerverzeichnis kommen in Frage. Die Haushaltsleitung selbst ist
 * immer Mitglied und kann nicht entfernt werden.
 *
 * <p>Entfernte Mitglieder verlieren nur den Zugang zur Tafel — Aufgaben und
 * Zuweisungen bleiben erhalten (Kennzeichnung durch die Task-Use-Case).
 */
@Service
@RequiredArgsConstructor
public class MembershipUseCaseImpl implements MembershipUseCase {

    private final HouseholdMemberRepository memberRepository;
    private final HouseholdRepository householdRepository;
    private final UserDirectory userDirectory;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<MemberView> listMembers(String currentUserId, long householdId) {
        Household household = requireHousehold(householdId);
        requireMembership(householdId, currentUserId);
        String adminId = household.getCreatedBy();
        return memberRepository.findByHousehold(householdId).stream()
                .sorted(Comparator.comparing((HouseholdMember m) -> !m.userId().equals(adminId))
                        .thenComparing(HouseholdMember::id))
                .map(member -> toView(member.userId(), adminId))
                .toList();
    }

    @Override
    @Transactional
    public MemberView addMember(String currentUserId, long householdId, String userId) {
        Household household = requireHousehold(householdId);
        requireAdmin(household, currentUserId);
        if (userId == null || userId.isBlank()) {
            throw new UnknownUserException("Eine Person muss benannt werden.");
        }
        userDirectory.findById(userId)
                .orElseThrow(() -> new UnknownUserException("Die Person ist im Nutzerverzeichnis unbekannt."));
        if (memberRepository.exists(householdId, userId)) {
            // Ernennung ist idempotent — doppeltes Klicken erzeugt kein zweites Mitglied.
            return toView(userId, household.getCreatedBy());
        }
        memberRepository.save(new HouseholdMember(null, householdId, userId, Instant.now(clock)));
        return toView(userId, household.getCreatedBy());
    }

    @Override
    @Transactional
    public void removeMember(String currentUserId, long householdId, String userId) {
        Household household = requireHousehold(householdId);
        requireAdmin(household, currentUserId);
        if (userId != null && userId.equals(household.getCreatedBy())) {
            throw new IllegalArgumentException("Die Haushaltsleitung kann nicht entfernt werden.");
        }
        // Idempotent: ein nicht vorhandenes Mitglied zu entfernen ist kein Fehler.
        memberRepository.delete(householdId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireMembership(long householdId, String userId) {
        if (userId == null || userId.isBlank() || !memberRepository.exists(householdId, userId)) {
            throw new MembershipRequiredException(
                    "Diese Person ist kein ernanntes Mitglied des Haushalts.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMember(long householdId, String userId) {
        return userId != null && !userId.isBlank() && memberRepository.exists(householdId, userId);
    }

    private Household requireHousehold(long householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Der Haushalt existiert nicht."));
    }

    private void requireAdmin(Household household, String currentUserId) {
        if (currentUserId == null || !currentUserId.equals(household.getCreatedBy())) {
            throw new NotHouseholdAdminException(
                    "Nur die Haushaltsleitung ernennt und entfernt Mitglieder.");
        }
    }

    private MemberView toView(String userId, String adminId) {
        String displayName = userDirectory.findById(userId).map(UserAccount::displayName).orElse(null);
        return new MemberView(userId, displayName, userId.equals(adminId));
    }
}
