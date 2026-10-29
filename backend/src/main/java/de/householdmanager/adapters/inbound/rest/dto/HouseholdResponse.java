package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.Household;
import java.time.Instant;

/**
 * Ausgehende DTOs des REST-Adapters. Gehören zum Transport, nicht zur
 * Anwendung.
 */
public record HouseholdResponse(Long id, String name, Instant createdAt) {

    public static HouseholdResponse from(Household household) {
        return new HouseholdResponse(household.getId(), household.getName(), household.getCreatedAt());
    }
}
