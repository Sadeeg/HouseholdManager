package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.HouseholdRequest;
import de.householdmanager.adapters.inbound.rest.dto.HouseholdResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.HouseholdUseCase;
import de.householdmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port.
 *
 * <p>Der Adapter übersetzt nur. Fachlogik steht im Use Case, Persistenz im
 * ausgehenden Adapter.
 */
@RestController
@RequestMapping("api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdUseCase householdUseCase;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    public List<HouseholdResponse> listHouseholds() {
        return householdUseCase.listHouseholds(currentUserId()).stream()
                .map(HouseholdResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse createHousehold(@Valid @RequestBody HouseholdRequest request) {
        return HouseholdResponse.from(householdUseCase.createHousehold(currentUserId(), request.name()));
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
