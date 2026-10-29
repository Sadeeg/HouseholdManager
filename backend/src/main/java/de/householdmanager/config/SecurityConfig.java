package de.householdmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security-Konfiguration: die API ist ein Token-geschützter Resource Server.
 *
 * <p>Authentifiziert wird über **Nextcloud/OIDC** (opake Tokens, Introspection
 * über {@code /apps/oidc/introspect}). Der Client ist confidential, weil die
 * OIDC-App ab 2.6.0 Public Clients nicht zur Introspection zulässt.
 *
 * <p>Ist kein Client konfiguriert, startet die Anwendung **ohne** Authentifizierung
 * (lauter Hinweis im Log). Das ist der Zustand eines frischen, noch nicht
 * eingerichteten Development-Setups — kein Produktivbetrieb.
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

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.opaque-token.client-id:}") String clientId,
            @Value("${householdmanager.security.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        this.authenticationEnabled = clientId != null && !clientId.isBlank();
        this.allowedOrigins = allowedOrigins;
        if (!authenticationEnabled) {
            log.warn("""
                    [SICHERHEIT] Kein OIDC-Client konfiguriert (NEXTCLOUD_OIDC_CLIENT_ID) — \
                    die API läuft OHNE Authentifizierung. Nur für die lokale Entwicklung, \
                    niemals für den Betrieb.""");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Bearer-Token statt Cookie: kein CSRF-Risiko
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (authenticationEnabled) {
            // Der Introspection-Client kommt aus Boot
            // (spring.security.oauth2.resourceserver.opaque-token.* aus .env).
            http.authorizeHttpRequests(requests -> requests
                            .requestMatchers(PUBLIC_PATHS.toArray(String[]::new)).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(Customizer.withDefaults()));
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
