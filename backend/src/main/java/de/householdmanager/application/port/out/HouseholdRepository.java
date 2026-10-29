package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.Household;
import java.util.List;

/**
 * Ausgehender Port (Outbound Port): die Anwendung weiß, <em>dass</em> sie
 * Haushalte speichern und laden kann, aber nicht <em>wie</em>.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.HouseholdRepositoryAdapter}.
 */
public interface HouseholdRepository {

    List<Household> findAll();

    Household save(Household household);
}
