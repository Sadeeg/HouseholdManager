package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.MemberRequest;
import de.householdmanager.adapters.inbound.rest.dto.MemberResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link MembershipUseCase}.
 *
 * <p>Erennen und Entfernen dürfen nur Haushaltsleitungen (403 sonst);
 * listen darf jedes Mitglied.
 */
@RestController
@RequestMapping("api/households/{householdId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MembershipUseCase membershipUseCase;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    public List<MemberResponse> listMembers(@PathVariable long householdId) {
        return membershipUseCase.listMembers(currentUserId(), householdId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(
            @PathVariable long householdId,
            @Valid @RequestBody MemberRequest request) {
        return MemberResponse.from(membershipUseCase.addMember(currentUserId(), householdId, request.userId()));
    }

    @DeleteMapping("{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable long householdId, @PathVariable String userId) {
        membershipUseCase.removeMember(currentUserId(), householdId, userId);
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
