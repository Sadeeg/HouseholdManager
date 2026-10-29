package de.householdmanager.adapters.inbound.security;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.domain.model.UserAccount;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class SecurityContextCurrentUserTest {

    private final SecurityContextCurrentUser currentUser = new SecurityContextCurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUser_isEmpty_withoutAuthentication() {
        assertThat(currentUser.currentUser()).isEmpty();
    }

    @Test
    void currentUser_isEmpty_forAnonymousAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(currentUser.currentUser()).isEmpty();
    }

    @Test
    void currentUser_mapsOidcClaims() {
        authenticate(Map.of("sub", "sascha", "name", "Sascha Deeg", "email", "sascha@example.org"));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user).contains(new UserAccount("sascha", "Sascha Deeg", "sascha@example.org"));
    }

    @Test
    void currentUser_fallsBackToPreferredUsername() {
        authenticate(Map.of("sub", "sascha", "preferred_username", "sascha", "email", "sascha@example.org"));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user.map(UserAccount::displayName)).contains("sascha");
    }

    @Test
    void currentUser_handlesMissingClaims() {
        authenticate(Map.of("sub", "sascha"));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user).isPresent();
        assertThat(user.get().id()).isEqualTo("sascha");
        assertThat(user.get().displayName()).isNull();
        assertThat(user.get().email()).isNull();
    }

    private void authenticate(Map<String, Object> claims) {
        // Der Name des OAuth2-Users muss einem Attribut entsprechen — "sub" ist
        // immer vorhanden.
        OAuth2User oauth2User = new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"), claims, "sub");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oauth2User, null, oauth2User.getAuthorities()));
    }
}
