package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Test der Token-Prüfkette.
 *
 * <p>Der Nextcloud-OIDC-Client stellt Access-Tokens nach RFC 9068 aus —
 * {@code typ: at+jwt}. Spring Security akzeptiert per Vorgabe nur
 * {@code JWT}; ohne Anpassung lehnt der Resource Server jedes solche Token
 * mit 401 ab.
 */
class JwtValidatorTest {

    private static final String ISSUER = "https://cloud.example.org";

    private final OAuth2TokenValidator<Jwt> validator = SecurityConfig.jwtValidator(ISSUER);

    @Test
    void acceptsAccessTokenWithAtJwtType() {
        OAuth2TokenValidatorResult result = validator.validate(token("at+jwt", ISSUER));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void acceptsPlainJwt() {
        OAuth2TokenValidatorResult result = validator.validate(token("JWT", ISSUER));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsUnknownType() {
        OAuth2TokenValidatorResult result = validator.validate(token("some-other-type", ISSUER));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsForeignIssuer() {
        OAuth2TokenValidatorResult result = validator.validate(token("at+jwt", "https://other.example.org"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        Jwt expired = new Jwt(
                "token-value",
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-01T01:00:00Z"),
                Map.of("alg", "RS256", "typ", "at+jwt"),
                Map.of("iss", ISSUER, "sub", "hm-test"));

        assertThat(validator.validate(expired).hasErrors()).isTrue();
    }

    private Jwt token(String typ, String issuer) {
        return new Jwt(
                "token-value",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256", "typ", typ),
                Map.of("iss", issuer, "sub", "hm-test", "aud", "client-id"));
    }
}
