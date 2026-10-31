package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.out.HouseholdMemberRepository;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.HouseholdMember;
import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import de.householdmanager.domain.model.WasteCalendar;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Haushalte verwalten".
 *
 * <p>Orchestriert nur: Fachlogik lebt im Modell, Persistenz im ausgehenden
 * Adapter. Der {@link Clock} kommt als Bean herein — die Zeit ist eine
 * Abhängigkeit der Anwendung, keine der Uhr-Komponente.
 *
 * <p>Sichtbar sind nur Haushalte, deren Mitglied oder Ersteller/in die
 * Person ist. Wer einen Haushalt anlegt, wird Haushaltsleitung und
 * automatisch Mitglied — die Haushaltsleitung ist immer Mitglied.
 */
@Service
@RequiredArgsConstructor
public class HouseholdUseCaseImpl implements HouseholdUseCase {

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final MembershipUseCase membershipUseCase;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<Household> listHouseholds(String userId) {
        // id-Rangfolge = Entstehungsreihenfolge; nach beiden Quellen vereinheitlicht.
        Map<Long, Household> visible = new LinkedHashMap<>();
        householdMemberRepository.findByUser(userId).stream()
                .map(HouseholdMember::householdId)
                .forEach(id -> householdRepository.findById(id).ifPresent(h -> visible.put(id, h)));
        householdRepository.findByCreatedBy(userId).forEach(h -> visible.put(h.getId(), h));
        return visible.values().stream()
                .sorted(Comparator.comparing(Household::getId))
                .toList();
    }

    @Override
    @Transactional
    public Household createHousehold(String userId, String name) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Ein Haushalt braucht eine Erstellerin oder einen Ersteller.");
        }
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidHouseholdNameException("Der Name des Haushalts darf nicht leer sein.");
        }
        if (trimmed.length() > Household.MAX_NAME_LENGTH) {
            throw new InvalidHouseholdNameException(
                    "Der Name des Haushalts darf höchstens " + Household.MAX_NAME_LENGTH + " Zeichen lang sein.");
        }
        Household household = Household.builder()
                .name(trimmed)
                .createdAt(Instant.now(clock))
                .createdBy(userId)
                .build();
        Household saved = householdRepository.save(household);
        householdMemberRepository.save(new HouseholdMember(
                null, saved.getId(), userId, Instant.now(clock)));
        return saved;
    }

    /**
     * Müllkalender (Issue #10): jedes Mitglied darf die Quelle pflegen (R2,
     * AC9) — anders als der Mitgliederbereich, der der Haushaltsleitung
     * vorbehalten ist. Die Fachgrenze der Adresse wacht im Fachmodell
     * ({@link WasteCalendar#validatedSourceUrl}).
     */
    @Override
    @Transactional
    public Household setWasteSource(String userId, long householdId, String url) {
        membershipUseCase.requireMembership(householdId, userId);
        Household household = requireHousehold(householdId);
        return householdRepository.save(household.withWasteCalendarUrl(WasteCalendar.validatedSourceUrl(url)));
    }

    /** Entfernt die Quelle; die gespeicherten Termine bleiben (Annahme 5). */
    @Override
    @Transactional
    public void removeWasteSource(String userId, long householdId) {
        membershipUseCase.requireMembership(householdId, userId);
        Household household = requireHousehold(householdId);
        householdRepository.save(household.withWasteCalendarUrl(null));
    }

    /** Haushalte mit hinterlegter Quelle — die Grundlage des Tageslaufs (R4). */
    @Override
    @Transactional(readOnly = true)
    public List<Household> listHouseholdsWithSource() {
        return householdRepository.findWithSource();
    }

    private Household requireHousehold(long householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException("Der Haushalt existiert nicht."));
    }
}
