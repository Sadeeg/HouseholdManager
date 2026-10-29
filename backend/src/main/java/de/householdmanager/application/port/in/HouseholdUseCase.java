package de.householdmanager.application.port.in;

import de.householdmanager.domain.model.Household;
import java.util.List;

/**
 * Eingehender Port (Inbound Port): die Use Cases, die die Anwendung anbietet.
 *
 * <p>Implementierung: {@code application.usecase.HouseholdUseCaseImpl}.
 * Eingehende Adapter (REST, CLI, Messaging) sprechen nur diesen Port an.
 *
 * <p>Seit Issue #1 ist die Person Kontext jeder Operation: sichtbar sind
 * nur Haushalte, deren Mitglied oder Ersteller sie ist; der Ersteller wird
 * Haushaltsleitung.
 */
public interface HouseholdUseCase {

    /** Die Haushalte, die die Person sehen darf (Mitglied oder Ersteller/in). */
    List<Household> listHouseholds(String userId);

    /** Legt einen Haushalt an; die anlegende Person wird Haushaltsleitung und Mitglied. */
    Household createHousehold(String userId, String name);
}
