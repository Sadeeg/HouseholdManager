---
name: spring-testing
description: Guide Spring Boot testing strategies including test slices, MockMvc, WebTestClient, Testcontainers, Spock, and reactive testing. Use when writing or improving tests for Spring applications.
version: 0.1.0
tags: [testing, mockmvc, testcontainers, spock, junit, webtestclient, reactive]
triggers:
  - writing unit or integration tests for Spring applications
  - using MockMvc or WebTestClient for controller testing
  - setting up Testcontainers for integration tests
  - configuring test slices
  - writing Spock specifications for Spring components
  - testing reactive WebFlux endpoints
spring_versions:
  boot: ">=3.2"
depends_on: [spring-boot]
---

# Spring Testing

Conventions for testing Spring Boot 3.2+ applications. Covers test slices, MockMvc, WebTestClient, Testcontainers, Spock, reactive testing, and test configuration.

**Do NOT Load** for general unit testing without Spring context — standard JUnit/Spock practices apply.

## Test Pyramid

| Level | Annotation | Context | Speed | Use For |
|-------|-----------|---------|-------|---------|
| Unit | None | No Spring | Fast | Services, mappers, domain logic |
| Slice | `@WebMvcTest`, `@DataJpaTest`, etc. | Partial | Medium | One layer in isolation |
| Integration | `@SpringBootTest` | Full | Slow | Cross-cutting flows, smoke tests |

**Default to the narrowest context that covers your test case.** Most tests should be unit or slice tests.

## Test Slices

### @WebMvcTest — Controller Layer

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrderService orderService;

    @Test
    void shouldReturnOrder() throws Exception {
        var order = new OrderDetail(1L, "Alice", BigDecimal.TEN, OrderStatus.CREATED);
        when(orderService.findById(1L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerName").value("Alice"))
            .andExpect(jsonPath("$.totalAmount").value(10));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(orderService.findById(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/api/orders/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateRequestBody() throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerName": "", "totalAmount": -1}
                    """))
            .andExpect(status().isBadRequest());
    }
}
```

`@WebMvcTest` loads only the web layer: controllers, `@ControllerAdvice`, filters, converters. Mock everything else with `@MockitoBean`.

### @WebFluxTest — Reactive Controller Layer

```java
@WebFluxTest(OrderController.class)
class OrderControllerTest {

    @Autowired WebTestClient webTestClient;
    @MockitoBean OrderService orderService;

    @Test
    void shouldReturnOrder() {
        var order = new OrderDetail(1L, "Alice", BigDecimal.TEN);
        when(orderService.findById(1L)).thenReturn(Mono.just(order));

        webTestClient.get().uri("/api/orders/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.customerName").isEqualTo("Alice");
    }

    @Test
    void shouldStreamOrders() {
        var orders = Flux.just(
            new OrderSummary(1L, "Alice"),
            new OrderSummary(2L, "Bob"));
        when(orderService.streamAll()).thenReturn(orders);

        webTestClient.get().uri("/api/orders/stream")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(OrderSummary.class).hasSize(2);
    }
}
```

### @DataJpaTest — Repository Layer

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)   // Use real DB via Testcontainers
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;
    @Autowired TestEntityManager em;

    @Test
    void shouldFindByStatus() {
        em.persist(new Order("Alice", BigDecimal.TEN, OrderStatus.COMPLETED));
        em.persist(new Order("Bob", BigDecimal.ONE, OrderStatus.CREATED));
        em.flush();

        var completed = orderRepository.findByStatus(OrderStatus.COMPLETED);

        assertThat(completed).hasSize(1);
        assertThat(completed.getFirst().getCustomerName()).isEqualTo("Alice");
    }
}
```

`@DataJpaTest` auto-configures an embedded database by default. Override with `Replace.NONE` to use Testcontainers.

### @JsonTest — Serialization

```java
@JsonTest
class OrderDetailJsonTest {

    @Autowired JacksonTester<OrderDetail> json;

    @Test
    void shouldSerialize() throws Exception {
        var order = new OrderDetail(1L, "Alice", new BigDecimal("99.99"), OrderStatus.CREATED);

        assertThat(json.write(order))
            .extractingJsonPathStringValue("$.customerName").isEqualTo("Alice")
            .extractingJsonPathNumberValue("$.totalAmount").isEqualTo(99.99);
    }

    @Test
    void shouldDeserialize() throws Exception {
        var content = """
            {"id": 1, "customerName": "Alice", "totalAmount": 99.99, "status": "CREATED"}
            """;

        assertThat(json.parse(content))
            .usingRecursiveComparison()
            .isEqualTo(new OrderDetail(1L, "Alice", new BigDecimal("99.99"), OrderStatus.CREATED));
    }
}
```

### Other Slices

| Slice | What it loads | Use For |
|-------|--------------|---------|
| `@WebMvcTest` | Controllers, filters, advice | REST endpoint testing (servlet) |
| `@WebFluxTest` | Reactive controllers, WebFlux config | REST endpoint testing (reactive) |
| `@DataJpaTest` | JPA repos, entities, Flyway | Repository queries, entity mapping |
| `@JsonTest` | Jackson/Gson config, testers | JSON serialization/deserialization |
| `@RestClientTest` | RestClient, MockRestServiceServer | Testing outbound REST calls |
| `@JdbcTest` | JdbcTemplate, Flyway | Direct JDBC queries |
| `@JooqTest` | jOOQ, Flyway | jOOQ queries |

## @SpringBootTest — Full Context

Use only when test slices are insufficient — typically for cross-cutting integration tests:

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void shouldCreateAndRetrieveOrder() throws Exception {
        var result = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerName": "Alice", "totalAmount": 99.99}
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        var location = result.getResponse().getHeader("Location");

        mockMvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerName").value("Alice"));
    }
}
```

For reactive full-context tests, use `@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)` with `WebTestClient`:

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @Autowired WebTestClient webTestClient;

    @Test
    void shouldCreateAndRetrieveOrder() {
        webTestClient.post().uri("/api/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {"customerName": "Alice", "totalAmount": 99.99}
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().exists("Location");
    }
}
```

## Reactive Testing

### StepVerifier for Service/Repository Layer

```java
@Test
void shouldFindOrdersByStatus() {
    StepVerifier.create(orderService.findByStatus(OrderStatus.COMPLETED))
        .expectNextMatches(order -> order.getCustomerName().equals("Alice"))
        .expectNextCount(2)
        .verifyComplete();
}

@Test
void shouldHandleEmpty() {
    StepVerifier.create(orderService.findByStatus(OrderStatus.CANCELLED))
        .verifyComplete();    // Completes without emitting
}

@Test
void shouldPropagateError() {
    StepVerifier.create(orderService.findById(99L))
        .verifyError(OrderNotFoundException.class);
}
```

### Virtual Time for Delayed Publishers

```java
@Test
void shouldRetryWithDelay() {
    StepVerifier.withVirtualTime(() -> orderService.findWithRetry(1L))
        .expectSubscription()
        .thenAwait(Duration.ofSeconds(5))    // Advance virtual clock
        .expectNextCount(1)
        .verifyComplete();
}
```

### Testing Reactive Streams with Testcontainers

Reactive repositories (R2DBC) work with Testcontainers too:

```java
@DataR2dbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class OrderRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            "r2dbc:postgresql://%s:%d/%s".formatted(
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired OrderRepository orderRepository;

    @Test
    void shouldFindByStatus() {
        StepVerifier.create(orderRepository.findByStatus(OrderStatus.COMPLETED))
            .expectNextCount(1)
            .verifyComplete();
    }
}
```

## Testcontainers

Replace H2 with real databases. See `references/testcontainers.md` for setup patterns.

### Quick Setup with @ServiceConnection (Spring Boot 3.1+)

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17");
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    }
}
```

`@ServiceConnection` auto-configures `spring.datasource.*`, `spring.kafka.bootstrap-servers`, etc. No manual property wiring needed.

## Mocking

### @MockitoBean vs @SpyBean

| Annotation | Behavior | Use When |
|------------|----------|----------|
| `@MockitoBean` | Replaces bean with a mock (all methods return defaults) | The real bean is not needed for this test |
| `@SpyBean` | Wraps real bean, allows selective stubbing | Need real behavior + override one method |

**Prefer `@MockitoBean`** — spies are fragile and indicate the test scope is too broad.

### Unit Tests: No Spring Context

```java
class OrderServiceTest {

    OrderRepository orderRepository = mock(OrderRepository.class);
    OrderService orderService = new OrderService(orderRepository);

    @Test
    void shouldThrowWhenNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(1L))
            .isInstanceOf(OrderNotFoundException.class);
    }
}
```

For unit tests, construct the object under test directly — no `@ExtendWith(MockitoExtension.class)` needed for simple cases.

## Test Naming and Structure

### JUnit 5

```java
class OrderServiceTest {
    // Method naming: should<Expected>When<Condition>
    @Test
    void shouldCreateOrderWhenInputIsValid() {}

    @Test
    void shouldThrowWhenOrderNotFound() {}

    // Structure: given/when/then as comments
    @Test
    void shouldCalculateTotalWithDiscount() {
        // given
        var order = OrderFixtures.completedOrder();

        // when
        var total = order.calculateTotal(DISCOUNT_10_PERCENT);

        // then
        assertThat(total).isEqualByComparingTo("90.00");
    }
}
```

### Spock

```groovy
class OrderServiceSpec extends Specification {

    def orderRepository = Mock(OrderRepository)
    def orderService = new OrderService(orderRepository)

    def "should throw when order not found"() {
        given:
        orderRepository.findById(1L) >> Optional.empty()

        when:
        orderService.findById(1L)

        then:
        thrown(OrderNotFoundException)
    }

    def "should calculate total with discount"() {
        expect:
        order.calculateTotal(discount) == expectedTotal

        where:
        order                          | discount | expectedTotal
        OrderFixtures.completedOrder() | 0.1      | 90.00
        OrderFixtures.completedOrder() | 0.0      | 100.00
    }
}
```

See `references/spock.md` for Spring integration patterns with Spock.

## Key Decisions

### H2 vs Testcontainers

| Aspect | H2 | Testcontainers |
|--------|----|----|
| Speed | Faster startup | Slower (container boot) |
| Fidelity | Emulation mode, subtle differences | Identical to production |
| Setup | Zero config | Docker required |
| **Recommendation** | **Never for integration tests** | **Always for integration tests** |

H2 hides SQL dialect differences, missing constraints, and database-specific behavior. Tests pass on H2 but fail in production.

### Where to Use @MockitoBean

- `@WebMvcTest` — mock service layer beans
- `@ApplicationModuleTest` (Modulith) — mock cross-module dependencies
- `@SpringBootTest` — only when a specific bean must be isolated (e.g., external API client)
- **Never in `@DataJpaTest`** — test the real repository

## NEVER

- **Never default to `@SpringBootTest`** — it loads the entire context. Use test slices first; resort to full context only for cross-cutting integration tests
- **Never test against H2 when production uses PostgreSQL** — SQL dialects differ in subtle ways. Use Testcontainers with the same database version
- **Never use `Thread.sleep()` in tests** — use Awaitility for async assertions, `CountDownLatch` for thread synchronization, or `StepVerifier.withVirtualTime()` for reactive
- **Never share mutable state between tests** — each test must be independently runnable. Use `@BeforeEach` for setup, `@DirtiesContext` only as a last resort (it's slow)
- **Never mock the class under test** — if you're mocking the thing you're testing, the test proves nothing. Mock dependencies, not the subject
- **Never write tests without assertions** — a test that calls code without `assertThat`, `andExpect`, or `then:` is just an expensive compilation check
- **Never use `@DirtiesContext` as a default** — it destroys and recreates the entire Spring context. Fix the shared state issue instead
- **Never suppress test failures with `@Disabled` permanently** — fix or delete them. `@Disabled` with a linked issue is acceptable temporarily

## Reference Files

- **`references/testcontainers.md`** — Testcontainers setup, @ServiceConnection, reusable containers, per-test vs shared lifecycle
- **`references/spock.md`** — Spock with Spring Boot: @SpringBean, data-driven tests, interaction testing, Testcontainers integration

## References

- [Spring Boot Testing Reference](https://docs.spring.io/spring-boot/reference/testing/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html)
- [Testcontainers for Java](https://java.testcontainers.org/)
- [Spock Framework](https://spockframework.org/spock/docs/2.3/index.html)
- [Project Reactor Testing](https://projectreactor.io/docs/core/release/reference/testing.html)
