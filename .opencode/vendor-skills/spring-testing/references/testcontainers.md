# Testcontainers — Reference

## @ServiceConnection (Spring Boot 3.1+)

The recommended approach. Spring Boot auto-detects the container type and configures properties:

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17");
    }
}
```

No `@DynamicPropertySource` needed — `@ServiceConnection` maps the container to the correct Spring Boot properties automatically.

### Supported Containers

| Container | Auto-configures |
|-----------|----------------|
| `PostgreSQLContainer` | `spring.datasource.*` |
| `MySQLContainer` | `spring.datasource.*` |
| `MongoDBContainer` | `spring.data.mongodb.*` |
| `KafkaContainer` | `spring.kafka.bootstrap-servers` |
| `RedisContainer` | `spring.data.redis.*` |
| `RabbitMQContainer` | `spring.rabbitmq.*` |
| `ElasticsearchContainer` | `spring.elasticsearch.*` |

## Reusable Singleton Pattern

Start one container per test suite, not per test class. Use a static field + static initializer:

```java
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();     // Started once, reused across all test classes
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }
}
```

### Using with Test Classes

```java
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OrderIntegrationTest { /* ... */ }

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrderRepositoryTest { /* ... */ }
```

## @DynamicPropertySource (Pre-3.1 or Custom Properties)

For containers not supported by `@ServiceConnection`, or when you need custom property mapping:

```java
@Testcontainers
@SpringBootTest
class ExternalApiTest {

    @Container
    static GenericContainer<?> wiremock = new GenericContainer<>("wiremock/wiremock:3.5.4")
        .withExposedPorts(8080);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("app.external-api.url", () ->
            "http://%s:%d".formatted(wiremock.getHost(), wiremock.getFirstMappedPort()));
    }
}
```

## Multiple Containers

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

    @Bean
    @ServiceConnection
    GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);
    }
}
```

## TestApplication for Local Development

Combine Testcontainers with `spring-boot-docker-compose` alternative for `./gradlew bootTestRun`:

```java
// src/test/java
class TestApplication {
    public static void main(String[] args) {
        SpringApplication.from(Application::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
    }
}
```

Run with `./gradlew bootTestRun` — starts the app with Testcontainers-managed infrastructure. Useful when Docker Compose is not available or desired.

## Container Lifecycle

| Approach | Lifecycle | Speed | Isolation |
|----------|-----------|-------|-----------|
| `@Container` + `@Testcontainers` | Per test class | Slow | High |
| Static singleton (recommended) | Per JVM | Fast | Shared — clean up data between tests |
| `@ServiceConnection` bean | Per Spring context | Medium | Context-scoped |

**Prefer the static singleton pattern** for CI speed. Use `@Transactional` test rollback or explicit cleanup between tests.

## Docker Requirements

- Testcontainers requires a Docker daemon (Docker Desktop, Colima, Podman, or remote Docker)
- CI: Most CI systems provide Docker. Use `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` for local development
- If Docker is unavailable, tests using Testcontainers will be skipped (not fail) when using `@Testcontainers` with `disabledWithoutDocker = true`
