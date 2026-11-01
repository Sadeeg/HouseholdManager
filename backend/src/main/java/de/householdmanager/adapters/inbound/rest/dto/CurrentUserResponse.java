package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.domain.model.UserAccount;

/**
 * Ausgehendes DTO: der angemeldete Nutzer.
 *
 * <p>Alle ankommenden und ausgehenden DTOs liegen in diesem Ordner — nicht
 * als verschachteltes Record im Controller (Issue #5).
 */
public record CurrentUserResponse(String id, String displayName, String email) {

    public static CurrentUserResponse from(UserAccount user) {
        return new CurrentUserResponse(user.id(), user.displayName(), user.email());
    }
}
