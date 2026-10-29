package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.Household;
import java.util.List;
import java.util.Optional;

/**
 * Ausgehender Port (Outbound Port): die Anwendung weiß, <em>dass</em> sie
 * Haushalte speichern und laden kann, aber nicht <em>wie</em>.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.HouseholdRepositoryAdapter}.
 */
public interface HouseholdRepository {

    List<Household> findAll();

    Optional<Household> findById(long id);

    /** Haushalte, die die Person angelegt hat (Ersteller = Haushaltsleitung). */
    List<Household> findByCreatedBy(String userId);

    Household save(Household household);
}
