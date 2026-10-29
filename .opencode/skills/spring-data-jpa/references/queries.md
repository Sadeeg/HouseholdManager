# Spring Data JPA Queries — Reference

## Specifications (Dynamic Queries)

Use when filter criteria are user-supplied and optional:

```java
interface OrderRepository extends ListCrudRepository<Order, Long>,
                                  JpaSpecificationExecutor<Order> {}
```

```java
class OrderSpecs {

    static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Order> customerNameContains(String name) {
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("customerName")),
            "%" + name.toLowerCase() + "%"
        );
    }

    static Specification<Order> totalAmountGreaterThan(BigDecimal amount) {
        return (root, query, cb) -> cb.greaterThan(root.get("totalAmount"), amount);
    }
}
```

```java
// Composing specifications dynamically
var spec = Specification.where(OrderSpecs.hasStatus(COMPLETED));
if (nameFilter != null) {
    spec = spec.and(OrderSpecs.customerNameContains(nameFilter));
}
if (minAmount != null) {
    spec = spec.and(OrderSpecs.totalAmountGreaterThan(minAmount));
}
var orders = orderRepository.findAll(spec);
```

## Pagination and Sorting

```java
interface OrderRepository extends ListCrudRepository<Order, Long>,
                                  ListPagingAndSortingRepository<Order, Long> {

    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);
}
```

```java
// In service
var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
var page = orderRepository.findByStatus(COMPLETED, pageable);

// Page metadata
page.getTotalElements();    // total matching records
page.getTotalPages();       // total pages
page.getContent();          // current page content
```

### Slice vs Page

| Type | Count Query | Use When |
|------|-------------|----------|
| `Page<T>` | Yes — runs `SELECT COUNT(*)` | Need total count (classic pagination with page numbers) |
| `Slice<T>` | No | Infinite scroll / "load more" — avoids expensive count query |

```java
Slice<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);
```

## Custom Repository Implementation

For queries that can't be expressed with derived methods, `@Query`, or Specifications:

```java
// 1. Define the custom interface
interface OrderRepositoryCustom {
    List<Order> findOrdersWithComplexCriteria(OrderSearchCriteria criteria);
}

// 2. Implement it (suffix must be "Impl" by convention)
@RequiredArgsConstructor
class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Order> findOrdersWithComplexCriteria(OrderSearchCriteria criteria) {
        var cb = em.getCriteriaBuilder();
        var query = cb.createQuery(Order.class);
        var root = query.from(Order.class);
        // ... complex criteria building
        return em.createQuery(query).getResultList();
    }
}

// 3. Extend in main repository
interface OrderRepository extends ListCrudRepository<Order, Long>,
                                  OrderRepositoryCustom {}
```

## Native Queries

Use for database-specific features or when JPQL is insufficient:

```java
@Query(value = """
    SELECT o.id, o.customer_name, o.total_amount
    FROM orders o
    WHERE o.status = :status
    AND o.created_at > NOW() - INTERVAL '30 days'
    """, nativeQuery = true)
List<Object[]> findRecentByStatusNative(@Param("status") String status);
```

For typed results, combine with interface projections:

```java
@Query(value = """
    SELECT o.customer_name AS customerName, SUM(o.total_amount) AS totalSpent
    FROM orders o
    GROUP BY o.customer_name
    HAVING SUM(o.total_amount) > :minSpent
    """, nativeQuery = true)
List<CustomerSpendingSummary> findHighSpenders(@Param("minSpent") BigDecimal minSpent);

interface CustomerSpendingSummary {
    String getCustomerName();
    BigDecimal getTotalSpent();
}
```

## Modifying Queries

```java
@Modifying
@Query("UPDATE Order o SET o.status = :status WHERE o.id IN :ids")
int updateStatusByIds(@Param("status") OrderStatus status, @Param("ids") List<Long> ids);

@Modifying
@Query("DELETE FROM Order o WHERE o.status = :status AND o.createdAt < :before")
int deleteOldByStatus(@Param("status") OrderStatus status, @Param("before") Instant before);
```

`@Modifying` queries bypass the persistence context. Call `entityManager.clear()` or `@Modifying(clearAutomatically = true)` if you read the same entities later in the transaction.

## Query Hints and Locking

```java
// Pessimistic locking
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM Order o WHERE o.id = :id")
Optional<Order> findByIdForUpdate(@Param("id") Long id);

// Query hints (e.g., read-only for performance)
@QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
List<Order> findByStatus(OrderStatus status);
```

## Key Guidelines

- **Start with derived queries**, move to `@Query` when names get long, use Specifications for dynamic filters
- **Always use projections for read-only queries** — selecting 3 columns is faster than loading a 20-field entity
- **Use `Slice` instead of `Page`** when you don't need total count — avoids an extra `COUNT(*)` query
- **Bulk operations (`@Modifying`) bypass the persistence context** — be aware of stale entities
- **Native queries lose portability** — use only when JPQL cannot express what you need
