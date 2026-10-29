package de.householdmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTypeValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    public SecurityConfig(
            @Value("${nextcloud.oidc.issuer-uri:}") String issuerUri,
            @Value("${householdmanager.security.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        this.issuerUri = issuerUri;
        this.authenticationEnabled = issuerUri != null && !issuerUri.isBlank();
        this.allowedOrigins = allowedOrigins;
        if (!authenticationEnabled) {
            log.warn("""
                    [SICHERHEIT] Kein OIDC-Issuer konfiguriert (NEXTCLOUD_OIDC_ISSUER) — \
                    die API läuft OHNE Authentifizierung. Nur für die lokale Entwicklung, \
                    niemals für den Betrieb.""");
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
     * <p>Geprüft werden Signatur (JWKS des Issuers), Ablaufzeit und Issuer.
     */
    @Bean
    @ConditionalOnProperty(name = "nextcloud.oidc.issuer-uri")
    public JwtDecoder jwtDecoder() {
        log.info("JWT-Validierung gegen Nextcloud/OIDC: {}", issuerUri);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(jwtValidator(issuerUri));
        return decoder;
    }

    /**
     * Prüfkette für die Access-Tokens: Typ (JWT oder at+jwt), Ablaufzeit
     * und Issuer.
     *
     * <p>Extrahiert, damit die Regeln ohne Netzwerkzugriff testbar sind.
     */
    static OAuth2TokenValidator<Jwt> jwtValidator(String issuerUri) {
        return JwtValidators.createDefaultWithValidators(
                List.of(new JwtIssuerValidator(issuerUri), new JwtTypeValidator("JWT", "at+jwt")));
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
