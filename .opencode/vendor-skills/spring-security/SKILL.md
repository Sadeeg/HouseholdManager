---
name: spring-security
description: Guide Spring Security configuration including OAuth2 Resource Server, JWT, SecurityFilterChain, method security, and CORS. Use when securing Spring Boot APIs, configuring authentication/authorization, or testing secured endpoints.
version: 0.1.0
tags: [security, oauth2, jwt, authentication, authorization]
triggers:
  - configuring Spring Security filter chain
  - setting up OAuth2 resource server with JWT
  - implementing method-level security
  - configuring CORS for secured endpoints
  - customizing authentication or authorization logic
  - testing secured endpoints
spring_versions:
  boot: ">=3.2"
  project: "Spring Security >=6.2"
depends_on: [spring-boot]
---

# Spring Security

Conventions for securing Spring Boot 3.2+ applications with Spring Security 6.2+. Uses the component-based `SecurityFilterChain` approach — `WebSecurityConfigurerAdapter` was removed in 6.0.

**Do NOT Load** for general Spring Boot development without security concerns.

## SecurityFilterChain

Every security configuration is a `@Bean` returning `SecurityFilterChain`:

```java
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .csrf(csrf -> csrf.disable())       // Stateless API — no session, no CSRF
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}
```

### Rule Ordering

Rules are evaluated top to bottom — **most specific first**:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/**").hasRole("ADMIN")       // 1. Specific paths first
    .requestMatchers("/api/**").authenticated()               // 2. Broader paths second
    .requestMatchers("/public/**").permitAll()                // 3. Public paths
    .anyRequest().denyAll()                                   // 4. Deny everything else
)
```

Use `denyAll()` as the default catch-all for APIs. Use `authenticated()` only if you genuinely want all authenticated users to access unmatched paths.

## OAuth2 Resource Server (JWT)

The standard pattern for securing REST APIs. The authorization server (Keycloak, Auth0, Entra ID, etc.) issues JWTs; your app validates them.

### Minimal Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com/realms/my-realm
```

Spring Boot auto-configures the `JwtDecoder` using the issuer's JWKS endpoint. No custom beans needed for the common case.

### Extracting Roles from JWT Claims

Most identity providers put roles in a custom claim. Map them to Spring Security authorities:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");  // Keycloak example
    grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return converter;
}
```

For deeply nested claims or non-standard structures, implement a custom converter:

```java
@Bean
JwtAuthenticationConverter jwtAuthenticationConverter() {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return List.of();
        @SuppressWarnings("unchecked")
        var roles = (List<String>) realmAccess.get("roles");
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    });
    return converter;
}
```

### Accessing the Authenticated User

```java
@GetMapping("/api/me")
UserInfo currentUser(@AuthenticationPrincipal Jwt jwt) {
    return new UserInfo(
        jwt.getSubject(),
        jwt.getClaimAsString("preferred_username"),
        jwt.getClaimAsString("email")
    );
}
```

## Method Security

Use `@EnableMethodSecurity` (on your config class) + `@PreAuthorize` on service or controller methods:

```java
@Service
class OrderService {

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOrder(String orderId) { /* ... */ }

    @PreAuthorize("#userId == authentication.token.subject")
    public List<Order> getOrdersForUser(String userId) { /* ... */ }
}
```

| Annotation | Use Case |
|------------|----------|
| `@PreAuthorize` | SpEL-based, most flexible — **prefer this** |
| `@Secured` | Simple role checks only, no SpEL — use for readability when SpEL isn't needed |
| `@RolesAllowed` | JSR-250 equivalent of `@Secured` — use if you prefer the Jakarta standard |
| `@PostAuthorize` | Check after method execution (e.g., verify return value ownership) — use sparingly |

### URL Security vs Method Security

- **URL security** (`authorizeHttpRequests`): coarse-grained, path-based. Use for public/authenticated/denied boundaries
- **Method security** (`@PreAuthorize`): fine-grained, business-logic-aware. Use for role checks and ownership validation

Combine both: URL security as the outer gate, method security for business rules.

## CORS

Configure CORS **in the SecurityFilterChain**, not via `@CrossOrigin` or `WebMvcConfigurer` alone — otherwise the security filter rejects preflight requests before CORS headers are set:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // ... other config
        .build();
}

@Bean
CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://app.example.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);

    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

## CSRF

| Application Type | CSRF | Reason |
|-----------------|------|--------|
| Stateless REST API (JWT) | **Disable** | No cookies/session = no CSRF risk |
| Server-rendered (Thymeleaf, sessions) | **Keep enabled** (default) | Session cookie is vulnerable to CSRF |
| SPA with cookie-based auth | **Keep enabled** + configure `CookieCsrfTokenRepository` | Cookie is sent automatically by browser |

```java
// Stateless API — safe to disable
.csrf(csrf -> csrf.disable())

// SPA with cookies — CSRF token in a cookie readable by JavaScript
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
```

## Key Decisions

### OAuth2 Resource Server vs OAuth2 Login

| Pattern | Use Case | Dependencies |
|---------|----------|-------------|
| Resource Server (JWT) | REST APIs consumed by SPAs, mobile, other services | `spring-boot-starter-oauth2-resource-server` |
| OAuth2 Login | Server-rendered web apps with browser login flow | `spring-boot-starter-oauth2-client` |

Most modern Spring Boot apps are APIs — use **Resource Server**.

### JWT vs Opaque Tokens

| Token Type | Validation | Use Case |
|------------|-----------|----------|
| JWT | Local (JWKS public key) — no network call per request | **Default choice** — fast, self-contained |
| Opaque | Introspection endpoint call per request | When tokens must be immediately revocable (rare) |

## NEVER

- **Never use `permitAll()` as default with selective protection** — start locked down (`denyAll()` or `authenticated()`), then open specific paths. The inverse is a security hole waiting to happen
- **Never disable CSRF without understanding why** — it's only safe for stateless APIs with no cookie-based authentication. Cargo-culting `csrf().disable()` on session-based apps is a vulnerability
- **Never hardcode secrets or signing keys in source** — use environment variables or a secrets manager. Symmetric JWT signing keys in `application.yml` leak to version control
- **Never roll your own authentication filter** — Spring Security's `oauth2ResourceServer()`, `formLogin()`, and `httpBasic()` cover standard patterns. Custom filters are error-prone and hard to audit
- **Never check roles inside controller method bodies** — use `@PreAuthorize` or URL authorization. Security logic scattered in business code is invisible and untestable
- **Never use `@CrossOrigin` on controllers alongside `SecurityFilterChain`** — CORS must be configured in the security filter chain; otherwise preflight `OPTIONS` requests are rejected before reaching the controller
- **Never configure `allowedOrigins("*")` with `allowCredentials(true)`** — browsers reject this combination. Enumerate specific origins when credentials are needed
- **Never expose actuator endpoints without authorization** — except `/actuator/health` and `/actuator/info`. Endpoints like `/env`, `/beans`, `/configprops` leak sensitive internals

## Testing Secured Endpoints

See `references/testing.md` for patterns on testing with MockMvc, `@WithMockUser`, JWT test tokens, and `SecurityMockMvcRequestPostProcessors`.

## References

- [Spring Security Reference Documentation](https://docs.spring.io/spring-security/reference/)
- [OAuth2 Resource Server — JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [CORS](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)
- [Testing](https://docs.spring.io/spring-security/reference/servlet/test/index.html)
