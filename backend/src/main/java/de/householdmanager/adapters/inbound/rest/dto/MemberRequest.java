package de.householdmanager.adapters.inbound.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Eingehendes DTO: Ernennung einer Person zum Haushaltsmitglied. */
public record MemberRequest(
        @NotBlank(message = "Eine Person muss benannt werden.")
        String userId) {
}
