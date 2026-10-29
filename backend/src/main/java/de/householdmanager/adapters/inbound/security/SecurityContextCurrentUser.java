package de.householdmanager.adapters.inbound.security;

import de.householdmanager.application.port.in.CurrentUserPort;
import de.householdmanager.domain.model.UserAccount;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Eingehender Adapter: liest den angemeldeten Nutzer aus dem
 * SecurityContext.
 *
 * <p>Übersetzt die Claims des Identity Providers (Nextcloud/OIDC) in das
 * Fachmodell. Fehlende Claims werden nicht geraten — das Displayname-Feld
 * ist optional, E-Mail und ID nicht.
 */
@Component
public class SecurityContextCurrentUser implements CurrentUserPort {

    @Override
    public Optional<UserAccount> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        // Der OIDC-Principal steckt im Principal der Authentication
        Object principal = authentication.getPrincipal();
        if (principal instanceof OAuth2User oauth2User) {
            return Optional.of(toUserAccount(oauth2User));
        }
        // Authentifiziert, aber ohne OIDC-Claims (z. B. anonymer Aufruf)
        return Optional.empty();
    }

    private UserAccount toUserAccount(OAuth2User oauth2User) {
        String id = oauth2User.getAttribute("sub");
        String displayName = firstNonBlank(
                oauth2User.getAttribute("name"),
                oauth2User.getAttribute("preferred_username"),
                oauth2User.getAttribute("email"));
        String email = oauth2User.getAttribute("email");
        return new UserAccount(id, displayName, email);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
