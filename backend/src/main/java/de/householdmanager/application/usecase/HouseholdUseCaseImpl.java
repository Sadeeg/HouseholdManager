package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Haushalte verwalten".
 *
 * <p>Orchestriert nur: Fachlogik lebt im Modell, Persistenz im ausgehenden
 * Adapter. Der {@link Clock} kommt als Bean herein — die Zeit ist eine
 * Abhängigkeit der Anwendung, keine der Uhr-Komponente.
 */
@Service
@RequiredArgsConstructor
public class HouseholdUseCaseImpl implements HouseholdUseCase {

    private final HouseholdRepository householdRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<Household> listHouseholds() {
        return householdRepository.findAll();
    }

    @Override
    @Transactional
    public Household createHousehold(String name) {
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
                .build();
        return householdRepository.save(household);
    }
}
