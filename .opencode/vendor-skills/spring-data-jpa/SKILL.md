---
name: spring-data-jpa
description: Guide Spring Data JPA usage including entity design, repositories, queries, projections, and performance optimization. Use when designing data models, writing repository interfaces, or troubleshooting JPA query performance.
version: 0.1.0
tags: [database, jpa, repositories, queries, entities]
triggers:
  - creating JPA entities or repositories
  - writing custom queries or projections
  - configuring database access with Spring Data
  - optimizing JPA query performance
  - mapping entity relationships
spring_versions:
  boot: ">=3.2"
  project: "Spring Data JPA >=3.2"
depends_on: [spring-boot]
---

# Spring Data JPA

Conventions for JPA entities, Spring Data repositories, queries, and projections in Spring Boot 3.2+.

**Do NOT Load** for non-relational data access (MongoDB, Redis, Elasticsearch) — use the appropriate Spring Data skill.

## Entity Design

### Basic Entity

```java
@Entity
@Table(name = "orders")
class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)        // Never ORDINAL — reordering breaks data
    @Column(nullable = false)
    private OrderStatus status;

    protected Order() {}                // JPA requires no-arg constructor

    Order(String customerName, BigDecimal totalAmount) {
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.CREATED;
    }
}
```

### Identity: UUID vs Long

| Strategy | Pros | Cons | Use When |
|----------|------|------|----------|
| `Long` + `IDENTITY` | Simple, compact, readable | DB-dependent, merge issues, predictable | Internal entities, high insert volume |
| `UUID` | Globally unique, assignable pre-persist, safe for distributed systems | 16 bytes, index fragmentation | Public-facing IDs, distributed systems, event-driven |

For UUID primary keys:

```java
@Id
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

### equals and hashCode

Use business key or ID-based equality — **never use `@Data`/`@EqualsAndHashCode` with all fields**:

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Order other)) return false;
    return id != null && id.equals(other.id);
}

@Override
public int hashCode() {
    return getClass().hashCode();     // Constant — safe for Sets before persist
}
```

This pattern works correctly with Hibernate proxies and across entity lifecycle states (transient, managed, detached).

### Field Access vs Property Access

Use **field access** (default when `@Id` is on a field). Never mix field and property annotations on the same entity — Hibernate picks one strategy per entity.

## Relationships

### Prefer Unidirectional

```java
// Good — unidirectional @ManyToOne
@Entity
class OrderItem {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
```

Add the `@OneToMany` inverse side **only when you need to navigate from parent to children in queries or business logic**:

```java
// Only when needed — bidirectional
@Entity
class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    void addItem(OrderItem item) {      // Always use helper methods to sync both sides
        items.add(item);
        item.setOrder(this);
    }
}
```

### Fetch Types

| Annotation | JPA Default | Recommended | Reason |
|------------|-------------|-------------|--------|
| `@ManyToOne` | `EAGER` | **`LAZY`** | Prevents loading the parent for every child query |
| `@OneToMany` | `LAZY` | `LAZY` | Already correct |
| `@OneToOne` | `EAGER` | **`LAZY`** | Same as ManyToOne — override the default |
| `@ManyToMany` | `LAZY` | `LAZY` | Already correct; consider a join entity instead |

**Always explicitly set `fetch = FetchType.LAZY`** on `@ManyToOne` and `@OneToOne` — the eager default is the #1 cause of N+1 problems.

### Cascade Rules

- `CascadeType.ALL` + `orphanRemoval = true` — only on parent-child aggregates where children don't exist without the parent
- **Never cascade from child to parent** (`@ManyToOne` should never have `cascade`)
- **Never cascade `REMOVE` on `@ManyToMany`** — it deletes the other side's entities, not just the association

## Repositories

### Interface Choice

| Interface | Methods | Use When |
|-----------|---------|----------|
| `ListCrudRepository<T, ID>` | CRUD returning `List` instead of `Iterable` | **Default choice** for Spring Boot 3.2+ |
| `JpaRepository<T, ID>` | CRUD + flush + batch + JPA-specific | Need `saveAndFlush`, `deleteInBatch`, paging |
| `CrudRepository<T, ID>` | CRUD returning `Iterable` | Prefer `ListCrudRepository` instead |

```java
interface OrderRepository extends ListCrudRepository<Order, Long> {

    // Derived query — Spring generates the JPQL
    List<Order> findByStatus(OrderStatus status);

    // Derived query with multiple conditions
    List<Order> findByCustomerNameAndStatusOrderByCreatedAtDesc(
        String customerName, OrderStatus status);

    // @Query when derived names become unwieldy
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :minAmount AND o.status = :status")
    List<Order> findExpensiveByStatus(@Param("minAmount") BigDecimal minAmount,
                                      @Param("status") OrderStatus status);
}
```

### Query Strategy

| Approach | Use When |
|----------|----------|
| Derived query | Simple conditions (1-2 fields), readable method name |
| `@Query` (JPQL) | Complex conditions, joins, aggregations |
| `@Query` (native) | Database-specific features, complex joins, performance-critical |
| `Specification` | Dynamic filtering with user-supplied criteria |

Prefer derived queries for simple cases. Switch to `@Query` the moment the method name becomes hard to read — `findByStatusAndCustomerNameContainingAndCreatedAtAfter` is too long.

## Projections

**Never return entities to the API layer.** Use projections for read operations:

### Interface Projection (Preferred for Simple Cases)

```java
interface OrderSummary {
    String getCustomerName();
    BigDecimal getTotalAmount();
    OrderStatus getStatus();
}

interface OrderRepository extends ListCrudRepository<Order, Long> {
    List<OrderSummary> findByStatus(OrderStatus status);
}
```

### Record Projection (Preferred for Service Layer DTOs)

```java
record OrderDto(String customerName, BigDecimal totalAmount, OrderStatus status) {}

interface OrderRepository extends ListCrudRepository<Order, Long> {
    @Query("SELECT new com.example.app.order.OrderDto(o.customerName, o.totalAmount, o.status) " +
           "FROM Order o WHERE o.status = :status")
    List<OrderDto> findDtosByStatus(@Param("status") OrderStatus status);
}
```

## Auditing

```java
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
```

```java
@Configuration
@EnableJpaAuditing
class JpaConfig {}
```

Use `Instant` for timestamps — not `LocalDateTime`. `Instant` is unambiguous and timezone-safe.

## Transactions

- Place `@Transactional` on **service methods**, not on repositories or controllers
- Use `@Transactional(readOnly = true)` on read-only service methods — Hibernate skips dirty checking, some databases use read replicas
- Never let transactions span user-facing I/O (HTTP calls, message sending)

```java
@Service
@RequiredArgsConstructor
class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) { /* ... */ }

    @Transactional(readOnly = true)
    public List<OrderSummary> findByStatus(OrderStatus status) { /* ... */ }
}
```

## N+1 Query Prevention

The most common JPA performance problem. Detect it by enabling SQL logging in development:

```yaml
# application-local.yml only — never in production
spring.jpa.show-sql: true
spring.jpa.properties.hibernate.format_sql: true
logging.level.org.hibernate.SQL: DEBUG
logging.level.org.hibernate.orm.jdbc.bind: TRACE
```

### Fix with Fetch Join

```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.status = :status")
List<Order> findByStatusWithItems(@Param("status") OrderStatus status);
```

### Fix with @EntityGraph

```java
@EntityGraph(attributePaths = {"items", "items.product"})
List<Order> findByStatus(OrderStatus status);
```

### Fix with Batch Fetching (Global)

```yaml
spring.jpa.properties.hibernate.default_batch_fetch_size: 16
```

Use fetch joins for targeted queries, batch fetching as a global safety net.

## Key Decisions

### Open Session in View (OSIV)

Spring Boot enables OSIV by default (`spring.jpa.open-in-view=true`). **Disable it:**

```yaml
spring.jpa.open-in-view: false
```

OSIV keeps the Hibernate session open through the HTTP response rendering. This hides lazy-loading issues during development that become N+1 bombs in production. Disabling forces you to fetch everything needed within the service transaction — which is correct.

### DDL Generation vs Flyway

| Approach | Use For |
|----------|---------|
| `spring.jpa.hibernate.ddl-auto=validate` | **Production** — Flyway manages schema, Hibernate validates |
| `spring.jpa.hibernate.ddl-auto=create-drop` | Throwaway prototypes only |

Never use `update` or `create` in production. Use Flyway for schema migrations (see spring-flyway skill).

## NEVER

- **Never use `FetchType.EAGER`** — override JPA defaults on `@ManyToOne` and `@OneToOne` to `LAZY`. Eager fetching is the root cause of most JPA performance issues
- **Never use `@EqualsAndHashCode` (Lombok) on entities** — it includes all fields by default, breaking equality across entity states and causing Hibernate proxy issues
- **Never return entities from controllers** — use projections or DTOs. Entities expose internal state, trigger lazy loading, and create serialization cycles
- **Never use `EnumType.ORDINAL`** — reordering enum constants silently corrupts data. Always use `EnumType.STRING`
- **Never cascade from child to parent** — `@ManyToOne(cascade = CascadeType.ALL)` deletes the parent when you delete a child
- **Never use `spring.jpa.hibernate.ddl-auto=update` in production** — it cannot drop columns, rename safely, or handle data migrations. Use Flyway
- **Never rely on Open Session in View** — it masks lazy loading bugs that surface as N+1 queries under load. Disable it and fetch eagerly within transactions
- **Never put `@Transactional` on repository interfaces** — Spring Data already wraps repository calls in transactions. Adding it again creates nested transaction confusion

## Reference Files

- **`references/queries.md`** — Advanced query patterns: Specifications, QueryDSL, custom repository implementations, pagination, sorting

## References

- [Spring Data JPA Reference Documentation](https://docs.spring.io/spring-data/jpa/reference/)
- [Hibernate ORM User Guide](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html)
- [Vlad Mihalcea's Blog — JPA Best Practices](https://vladmihalcea.com/)
