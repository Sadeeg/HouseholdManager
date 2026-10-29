package de.householdmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Security-Konfiguration: die API ist ein Token-geschützter Resource Server.
 *
 * <p>Authentifiziert wird über **Nextcloud/OIDC**. Der OIDC-Client des
 * Nextcloud stellt **JWT-Access-Tokens** aus (RFC 9068) — der Resource Server
 * validiert deren Signatur über den JWKS des Identity Providers. Damit ist im
 * Backend **kein Client-Secret** nötig (anders als bei der Introspection
 * opaker Tokens).
 *
 * <p>Ist kein Issuer konfiguriert, startet die Anwendung **ohne**
 * Authentifizierung (lauter Hinweis im Log). Das ist der Zustand eines
 * frischen, noch nicht eingerichteten Development-Setups — kein
 * Produktivbetrieb.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Endpunkte, die ohne Anmeldung erreichbar sind. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info");

    private final boolean authenticationEnabled;
    private final List<String> allowedOrigins;
    private final String issuerUri;
    private final String clientId;

    public SecurityConfig(
            @Value("${nextcloud.oidc.issuer-uri:}") String issuerUri,
            @Value("${nextcloud.oidc.client-id:}") String clientId,
            @Value("${householdmanager.security.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.authenticationEnabled = issuerUri != null && !issuerUri.isBlank();
        this.allowedOrigins = allowedOrigins;
        if (!authenticationEnabled) {
            log.warn("""
                    [SICHERHEIT] Kein OIDC-Issuer konfiguriert (NEXTCLOUD_OIDC_ISSUER) — \
                    die API läuft OHNE Authentifizierung. Nur für die lokale Entwicklung, \
                    niemals für den Betrieb.""");
        } else if (clientId == null || clientId.isBlank()) {
            log.warn("""
                    [SICHERHEIT] Keine OIDC-Client-ID konfiguriert (NEXTCLOUD_OIDC_CLIENT_ID) — \
                    die API prüft nicht, für welchen Client ein Token ausgestellt wurde. \
                    Damit dürfen Tokens aller Clients desselben Issuers verwendet werden.""");
        }
    }

    /**
     * Decoder für die JWT-Access-Tokens des Identity Providers.
     *
     * <p>Der Nextcloud-OIDC-Client stellt Access-Tokens nach
     * <a href="https://www.rfc-editor.org/rfc/rfc9068">RFC 9068</a> aus —
     * deren {@code typ}-Header lautet {@code at+jwt}. Spring Security
     * akzeptiert per Vorgabe nur {@code JWT}; ohne Anpassung wird jedes
     * Token mit 401 abgewiesen. Deshalb wird der Typ-Validator hier
     * ausdrücklich auf beide Werte gesetzt.
     *
     * <p>Die Bedingung prüft auf einen **leeren** Issuer: ohne Issuer darf
     * kein Decoder gebaut werden, sonst bricht der Start mit
     * „URI is not absolute" ab, statt wie dokumentiert ohne Authentifizierung
     * zu laufen.
     */
    @Bean
    @ConditionalOnExpression("!'${nextcloud.oidc.issuer-uri:}'.isBlank()")
    public JwtDecoder jwtDecoder() {
        log.info("JWT-Validierung gegen Nextcloud/OIDC: {}", issuerUri);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(jwtValidator(issuerUri, clientId));
        return decoder;
    }

    /**
     * Prüfkette für die Access-Tokens: Typ (JWT oder at+jwt), Ablaufzeit,
     * Issuer und — falls konfiguriert — Audience.
     *
     * <p>Die Audience-Prüfung bindet ein Token an den Client, für den es
     * ausgestellt wurde: sonst würde ein Token eines anderen Nextcloud-Client
     * desselben Issuers ebenfalls die API öffnen.
     *
     * <p>Extrahiert, damit die Regeln ohne Netzwerkzugriff testbar sind.
     */
    static OAuth2TokenValidator<Jwt> jwtValidator(String issuerUri, String clientId) {
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtIssuerValidator(issuerUri));
        validators.add(new JwtTypeValidator("JWT", "at+jwt"));
        if (clientId != null && !clientId.isBlank()) {
            validators.add(audienceValidator(clientId));
        }
        return JwtValidators.createDefaultWithValidators(validators);
    }

    /**
     * Prüft, ob die Audience des Tokens den erwarteten Client enthält.
     *
     * <p>RFC 7519 erlaubt für {@code aud} einen einzelnen String oder eine
     * Liste — der Nextcloud-OIDC-Client liefert einen String, während
     * Spring Securitys {@code JwtAudienceValidator} ausschließlich Listen
     * erwartet. Deshalb wird der Claim hier selbst ausgewertet.
     */
    private static OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
        return new JwtClaimValidator<>("aud", value -> value instanceof Collection<?> audiences
                ? audiences.contains(expectedAudience)
                : expectedAudience.equals(value));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Bearer-Token statt Cookie: kein CSRF-Risiko
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (authenticationEnabled) {
            http.authorizeHttpRequests(requests -> requests
                            .requestMatchers(PUBLIC_PATHS.toArray(String[]::new)).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        }
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
