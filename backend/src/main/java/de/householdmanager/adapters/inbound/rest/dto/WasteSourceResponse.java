package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.Household;

/**
 * Ausgehendes DTO: die gespeicherte Adresse des Haushalts.
 */
public record WasteSourceResponse(String sourceUrl) {

    public static WasteSourceResponse from(Household household) {
        return new WasteSourceResponse(household.getWasteCalendarUrl());
    }
}
