package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.application.port.in.UserSearchUseCase.UserRef;

/**
 * Ausgehendes DTO: ein Vorschlag aus dem Nutzerverzeichnis (keine E-Mail).
 *
 * <p>Alle ankommenden und ausgehenden DTOs liegen in diesem Ordner — nicht
 * als verschachteltes Record im Controller (Issue #5).
 */
public record UserRefResponse(String id, String displayName) {

    public static UserRefResponse from(UserRef ref) {
        return new UserRefResponse(ref.id(), ref.displayName());
    }
}
