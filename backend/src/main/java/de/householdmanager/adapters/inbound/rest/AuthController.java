package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.adapters.inbound.rest.dto.CurrentUserResponse;
import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.domain.model.UserAccount;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: meldet den angemeldeten Nutzer.
 *
 * <p>Das Frontend fragt damit nach dem Login, wer angemeldet ist. Der
 * Adapter liest den eingehenden Port — er kennt weder Security-Klassen noch
 * Token-Format.
 */
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUserPort currentUserPort;

    @GetMapping("me")
    public CurrentUserResponse currentUser() {
        Optional<UserAccount> user = currentUserPort.currentUser();
        if (user.isEmpty()) {
            throw new NotAuthenticatedException();
        }
        return CurrentUserResponse.from(user.get());
    }

    /** Wird vom eingehenden Adapter in 401 übersetzt (siehe SecurityConfig). */
    public static class NotAuthenticatedException extends RuntimeException {
    }
}
