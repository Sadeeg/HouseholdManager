package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.WorkspaceRequest;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.application.port.in.WorkspaceUseCase;
import de.householdmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link WorkspaceUseCase}.
 *
 * <p>GET liefert die gemerkte Haushaltskennung (404 ohne Auswahl — das
 * Frontend behandelt sie dann unbestimmt). PUT merkt einen neuen Haushalt,
 * Mitgliedschaft vorausgesetzt (403 sonst).
 */
@RestController
@RequestMapping("api/me/workspace")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceUseCase workspaceUseCase;
    private final CurrentUserPort currentUserPort;

    @GetMapping
    public Long currentWorkspace() {
        return workspaceUseCase.currentWorkspace(currentUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Kein Haushalt gewählt."));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void selectWorkspace(@Valid @RequestBody WorkspaceRequest request) {
        workspaceUseCase.selectWorkspace(currentUserId(), request.householdId());
    }

    private String currentUserId() {
        return currentUserPort.currentUser().map(UserAccount::id)
                .orElseThrow(AuthController.NotAuthenticatedException::new);
    }
}
