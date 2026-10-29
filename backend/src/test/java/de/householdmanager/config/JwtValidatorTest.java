package de.householdmanager.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
    private static final String CLIENT_ID = "client-id-of-the-frontend";

    private final OAuth2TokenValidator<Jwt> validator = SecurityConfig.jwtValidator(ISSUER, CLIENT_ID);

    @Test
    void acceptsAccessTokenWithAtJwtType() {
        OAuth2TokenValidatorResult result = validator.validate(token("at+jwt", ISSUER, CLIENT_ID));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void acceptsPlainJwt() {
        OAuth2TokenValidatorResult result = validator.validate(token("JWT", ISSUER, CLIENT_ID));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsUnknownType() {
        OAuth2TokenValidatorResult result = validator.validate(token("some-other-type", ISSUER, CLIENT_ID));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsForeignIssuer() {
        OAuth2TokenValidatorResult result =
                validator.validate(token("at+jwt", "https://other.example.org", CLIENT_ID));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenForForeignClient() {
        OAuth2TokenValidatorResult result =
                validator.validate(token("at+jwt", ISSUER, "client-id-of-another-app"));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void acceptsAudienceAsList() {
        // RFC 7519 erlaubt für aud einen String oder eine Liste
        Jwt withAudienceList = new Jwt(
                "token-value",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256", "typ", "at+jwt"),
                Map.of("iss", ISSUER, "sub", "hm-test", "aud", List.of(CLIENT_ID, "other")));

        assertThat(validator.validate(withAudienceList).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenWithoutAudience() {
        Jwt withoutAudience = new Jwt(
                "token-value",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256", "typ", "at+jwt"),
                Map.of("iss", ISSUER, "sub", "hm-test"));

        assertThat(validator.validate(withoutAudience).hasErrors()).isTrue();
    }

    @Test
    void rejectsExpiredToken() {
        Jwt expired = new Jwt(
                "token-value",
                Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-01T01:00:00Z"),
                Map.of("alg", "RS256", "typ", "at+jwt"),
                Map.of("iss", ISSUER, "sub", "hm-test", "aud", CLIENT_ID));

        assertThat(validator.validate(expired).hasErrors()).isTrue();
    }

    @Test
    void withoutConfiguredClient_anyAudienceIsAccepted() {
        OAuth2TokenValidator<Jwt> withoutClient = SecurityConfig.jwtValidator(ISSUER, "");

        OAuth2TokenValidatorResult result =
                withoutClient.validate(token("at+jwt", ISSUER, "client-id-of-another-app"));

        assertThat(result.hasErrors()).isFalse();
    }

    private Jwt token(String typ, String issuer, String audience) {
        return new Jwt(
                "token-value",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256", "typ", typ),
                Map.of("iss", issuer, "sub", "hm-test", "aud", audience));
    }
}
