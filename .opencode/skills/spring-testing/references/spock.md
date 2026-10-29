# Spock with Spring Boot — Reference

## Setup (Gradle)

```gradle
plugins {
    id 'groovy'
}

dependencies {
    testImplementation 'org.spockframework:spock-core:2.3-groovy-4.0'
    testImplementation 'org.spockframework:spock-spring:2.3-groovy-4.0'
}
```

## Unit Tests — No Spring Context

```groovy
class OrderServiceSpec extends Specification {

    def orderRepository = Mock(OrderRepository)
    def eventPublisher = Mock(ApplicationEventPublisher)
    def orderService = new OrderService(orderRepository, eventPublisher)

    def "should create order and publish event"() {
        given:
        def request = new CreateOrderRequest("Alice", 99.99)

        when:
        def result = orderService.create(request)

        then:
        1 * orderRepository.save(_ as Order) >> { Order order ->
            order.id = 1L
            order
        }
        1 * eventPublisher.publishEvent(_ as OrderCreated)

        and:
        result.id == 1L
        result.customerName == "Alice"
    }
}
```

## Spring Integration Tests

### @SpringBean — Replace Bean with Mock

```groovy
@SpringBootTest
class OrderIntegrationSpec extends Specification {

    @SpringBean
    PaymentClient paymentClient = Mock()

    @Autowired
    OrderService orderService

    def "should handle payment failure gracefully"() {
        given:
        paymentClient.charge(_ as PaymentRequest) >> { throw new PaymentDeclinedException() }

        when:
        orderService.processPayment("order-1")

        then:
        thrown(PaymentDeclinedException)
    }
}
```

`@SpringBean` is Spock's equivalent of `@MockitoBean`. It replaces the bean in the Spring context.

### @WebMvcTest with Spock

```groovy
@WebMvcTest(OrderController)
class OrderControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    OrderService orderService = Mock()

    def "should return order by id"() {
        given:
        orderService.findById(1L) >> new OrderDetail(1L, "Alice", 99.99, OrderStatus.CREATED)

        expect:
        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.customerName').value("Alice"))
    }

    def "should return 404 when not found"() {
        given:
        orderService.findById(99L) >> { throw new OrderNotFoundException(99L) }

        expect:
        mockMvc.perform(get("/api/orders/99"))
            .andExpect(status().isNotFound())
    }
}
```

### @DataJpaTest with Spock

```groovy
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestcontainersConfiguration)
class OrderRepositorySpec extends Specification {

    @Autowired
    OrderRepository orderRepository

    @Autowired
    TestEntityManager em

    def "should find orders by status"() {
        given:
        em.persist(new Order("Alice", 100.00, OrderStatus.COMPLETED))
        em.persist(new Order("Bob", 50.00, OrderStatus.CREATED))
        em.flush()

        when:
        def results = orderRepository.findByStatus(OrderStatus.COMPLETED)

        then:
        results.size() == 1
        results[0].customerName == "Alice"
    }
}
```

## Data-Driven Tests

Spock's `where:` block is powerful for parameterized testing:

```groovy
def "should validate order amount"() {
    expect:
    orderService.isValidAmount(amount) == expected

    where:
    amount | expected
    100.00 | true
    0.01   | true
    0.00   | false
    -1.00  | false
    null   | false
}
```

### Data Tables with Complex Objects

```groovy
def "should calculate shipping cost by region"() {
    expect:
    shippingService.calculate(region, weight) == expectedCost

    where:
    region     | weight | expectedCost
    "EU"       | 1.0    | 5.99
    "EU"       | 5.0    | 12.99
    "US"       | 1.0    | 9.99
    "US"       | 5.0    | 19.99
    "DOMESTIC" | 1.0    | 3.99
}
```

## Interaction Testing

Spock's interaction verification is more readable than Mockito's `verify()`:

```groovy
def "should notify customer on order completion"() {
    when:
    orderService.complete("order-1")

    then: "notification is sent exactly once"
    1 * notificationService.send({ it.orderId == "order-1" && it.type == "COMPLETED" })

    and: "no other notifications are sent"
    0 * notificationService.send(_)
}

def "should retry on transient failure"() {
    when:
    orderService.fetchWithRetry("order-1")

    then: "first call fails"
    1 * orderRepository.findById("order-1") >> { throw new TransientException() }

    then: "second call succeeds (ordered after first)"
    1 * orderRepository.findById("order-1") >> Optional.of(new Order("order-1"))
}
```

## Naming Convention

| Type | Class Name | Location |
|------|-----------|----------|
| Unit | `<Class>Spec.groovy` | `src/test/groovy/` |
| Integration | `<Feature>IntegrationSpec.groovy` | `src/test/groovy/` |
| Method | `"should <behavior> when <condition>"` | — |

## Key Guidelines

- **Use Spock for unit tests** — the Given-When-Then blocks and data tables are unmatched for readability
- **Use `@SpringBean` instead of `@MockitoBean`** in Spock tests — it integrates natively with Spock's mock lifecycle
- **Use data-driven tests** (`where:` blocks) to replace multiple similar test methods
- **Interaction assertions in `then:` block** are verified in order — use separate `then:` blocks for ordered interactions
- **Avoid `and:` in `when:` blocks** — `when:` should contain a single action. Use `given:` for setup
