package de.householdmanager.application.port.out;

import de.householdmanager.domain.model.HouseholdMember;
import java.util.List;

/**
 * Ausgehender Port: ernannte Mitgliedschaften.
 *
 * <p>Implementierung: {@code adapters.outbound.persistence.HouseholdMemberRepositoryAdapter}.
 */
public interface HouseholdMemberRepository {

    /** Die Mitglieder eines Haushalts, in Reihenfolge der Ernennung. */
    List<HouseholdMember> findByHousehold(long householdId);

    /** Die Haushalte, in denen die Person ernannt ist. */
    List<HouseholdMember> findByUser(String userId);

    boolean exists(long householdId, String userId);

    HouseholdMember save(HouseholdMember member);

    void delete(long householdId, String userId);
}
