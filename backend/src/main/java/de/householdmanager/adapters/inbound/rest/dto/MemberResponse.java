package de.householdmanager.adapters.inbound.rest.dto;

import de.householdmanager.application.port.in.MembershipUseCase.MemberView;

/** Ausgehendes DTO: ein Haushaltsmitglied (Name serverseitig aufgelöst). */
public record MemberResponse(String userId, String displayName, boolean householdAdmin) {

    public static MemberResponse from(MemberView view) {
        return new MemberResponse(view.userId(), view.displayName(), view.householdAdmin());
    }
}
