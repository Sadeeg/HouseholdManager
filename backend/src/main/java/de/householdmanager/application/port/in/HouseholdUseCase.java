package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.Household;
import java.util.List;

/**
 * Eingehender Port (Inbound Port): die Use Cases, die die Anwendung anbietet.
 *
 * <p>Implementierung: {@code application.usecase.HouseholdUseCaseImpl}.
 * Eingehende Adapter (REST, CLI, Messaging) sprechen nur diesen Port an.
 */
public interface HouseholdUseCase {

    List<Household> listHouseholds();

    Household createHousehold(String name);
}
