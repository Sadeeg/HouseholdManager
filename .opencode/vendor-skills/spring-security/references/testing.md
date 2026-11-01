# Spring Security Testing — Reference

## MockMvc with Security

### Setup

```java
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)    // Include your security config in the slice
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrderService orderService;
}
```

### @WithMockUser — Simple Role Testing

```java
@Test
@WithMockUser(roles = "ADMIN")
void shouldAllowAdminAccess() throws Exception {
    mockMvc.perform(get("/api/admin/orders"))
        .andExpect(status().isOk());
}

@Test
void shouldRejectUnauthenticatedAccess() throws Exception {
    mockMvc.perform(get("/api/orders"))
        .andExpect(status().isUnauthorized());
}

@Test
@WithMockUser(roles = "USER")
void shouldForbidUserFromAdminEndpoint() throws Exception {
    mockMvc.perform(get("/api/admin/orders"))
        .andExpect(status().isForbidden());
}
```

### JWT MockMvc Request Post-Processor

For OAuth2 Resource Server apps, use `jwt()` post-processor instead of `@WithMockUser`:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@Test
void shouldExtractUserFromJwt() throws Exception {
    mockMvc.perform(get("/api/me")
            .with(jwt().jwt(jwt -> jwt
                .subject("user-123")
                .claim("preferred_username", "jdoe")
                .claim("realm_access", Map.of("roles", List.of("USER")))
            )))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("jdoe"));
}

@Test
void shouldCheckRolesFromJwtClaims() throws Exception {
    mockMvc.perform(delete("/api/admin/orders/1")
            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk());
}
```

### Custom @WithMockUser Annotation

For repeated test setups, create a custom annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(username = "admin", roles = "ADMIN")
@interface WithMockAdmin {}
```

```java
@Test
@WithMockAdmin
void shouldAllowAdmin() throws Exception { /* ... */ }
```

## Test SecurityFilterChain (Integration)

For full integration tests where the security filter chain must run end-to-end:

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isUnauthorized());
    }
}
```

## Test Configuration for JwtDecoder

In test profiles, replace the real `JwtDecoder` (which contacts the authorization server) with a mock:

```java
@TestConfiguration
class TestSecurityConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(testPublicKey()).build();
    }
}
```

Or simply use `spring.security.oauth2.resourceserver.jwt.public-key-location` in `application-test.yml` pointing to a test key.

The `jwt()` MockMvc post-processor bypasses the `JwtDecoder` entirely — it builds a pre-authenticated `JwtAuthenticationToken`. Use this for controller tests. Use a real `JwtDecoder` only for end-to-end security chain validation.

## Method Security Testing

```java
@SpringBootTest
class OrderServiceSecurityTest {

    @Autowired OrderService orderService;

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyDeleteForNonAdmin() {
        assertThatThrownBy(() -> orderService.deleteOrder("1"))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowDeleteForAdmin() {
        assertThatCode(() -> orderService.deleteOrder("1"))
            .doesNotThrowAnyException();
    }
}
```

## Key Testing Guidelines

- **Always test both allowed and denied scenarios** — a security test that only checks the happy path is worthless
- **Use `jwt()` post-processor for Resource Server apps**, `@WithMockUser` for session-based apps
- **Test the SecurityFilterChain configuration separately** from method security — they are different layers
- **Include `@Import(SecurityConfig.class)`** in `@WebMvcTest` — by default, test slices don't load your security config
- **Never skip security in tests** by permitting all — test the actual security rules
