package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.application.port.in.UserSearchUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Eingehender Adapter: HTTP → eingehender Port {@link UserSearchUseCase}.
 *
 * <p>GET api/users/search?q= liefert Personenvorschläge aus dem
 * Nutzerverzeichnis (id + Anzeigename, keine E-Mail) für den UserPicker.
 */
@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserSearchController {

    private final UserSearchUseCase userSearchUseCase;

    @GetMapping("search")
    public List<UserRefResponse> search(@RequestParam(name = "q", defaultValue = "") String query) {
        return userSearchUseCase.searchUsers(query).stream()
                .map(UserRefResponse::from)
                .toList();
    }

    public record UserRefResponse(String id, String displayName) {

        static UserRefResponse from(UserSearchUseCase.UserRef ref) {
            return new UserRefResponse(ref.id(), ref.displayName());
        }
    }
}
