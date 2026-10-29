package de.householdmanager.adapters.inbound.security;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.domain.model.UserAccount;
import java.time.Instant;
import java.util.List;
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
import org.springframework.security.oauth2.jwt.Jwt;

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
    void currentUser_mapsJwtClaims() {
        // So kommen die Claims des Nextcloud-OIDC-JWT an (RFC 9068)
        authenticate(jwt(Map.of(
                "sub", "hm-test",
                "name", "HM Test",
                "preferred_username", "hm-test",
                "email", "hm-test@example.org")));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user).contains(new UserAccount("hm-test", "HM Test", "hm-test@example.org"));
    }

    @Test
    void currentUser_fallsBackToPreferredUsername_forJwt() {
        authenticate(jwt(Map.of("sub", "hm-test", "preferred_username", "hm-test")));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user.map(UserAccount::displayName)).contains("hm-test");
    }

    @Test
    void currentUser_handlesMissingJwtClaims() {
        authenticate(jwt(Map.of("sub", "hm-test")));

        Optional<UserAccount> user = currentUser.currentUser();

        assertThat(user).isPresent();
        assertThat(user.get().id()).isEqualTo("hm-test");
        assertThat(user.get().displayName()).isNull();
        assertThat(user.get().email()).isNull();
    }

    @Test
    void currentUser_mapsOidcUserClaims() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                AuthorityUtils.createAuthorityList("ROLE_USER"),
                Map.of("sub", "sascha", "name", "Sascha Deeg", "email", "sascha@example.org"),
                "sub");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oauth2User, null, oauth2User.getAuthorities()));

        assertThat(currentUser.currentUser())
                .contains(new UserAccount("sascha", "Sascha Deeg", "sascha@example.org"));
    }

    private void authenticate(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        jwt, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                Map.of("alg", "RS256", "typ", "at+jwt"),
                claims);
    }
}
