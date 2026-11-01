# Transaction Management Deep Dive

Detailed reference for `@Transactional` propagation, isolation, rollback rules, and multi-datasource patterns.

## Propagation Matrix

| Propagation | Outer TX Exists | Outer TX Absent | Use Case |
|-------------|----------------|-----------------|----------|
| `REQUIRED` (default) | Join outer | Create new | Standard business methods |
| `REQUIRES_NEW` | Suspend outer, create new | Create new | Audit logging, independent operations |
| `NESTED` | Create savepoint | Create new | Partial rollback within outer TX |
| `SUPPORTS` | Join outer | Run non-TX | Read methods that work with or without TX |
| `NOT_SUPPORTED` | Suspend outer | Run non-TX | Non-transactional operations (e.g., email) |
| `MANDATORY` | Join outer | Throw exception | Enforces that caller provides a TX |
| `NEVER` | Throw exception | Run non-TX | Enforces non-transactional execution |

### REQUIRES_NEW vs NESTED

```java
@Transactional
public void processOrder(Order order) {
    orderRepository.save(order);

    try {
        notificationService.send(order);       // REQUIRES_NEW: independent TX
    } catch (Exception e) {
        // Notification failure does NOT roll back order save
        log.warn("Notification failed", e);
    }
}

@Transactional
public void processWithPartialRollback(Order order) {
    orderRepository.save(order);

    try {
        enrichmentService.enrich(order);       // NESTED: savepoint within outer TX
    } catch (Exception e) {
        // Enrichment rolled back to savepoint, order save still committed
        log.warn("Enrichment failed, continuing without", e);
    }
}
```

**Key difference:** `REQUIRES_NEW` commits independently even if the outer TX rolls back. `NESTED` rolls back with the outer TX unless the savepoint was already committed.

## Isolation Levels

| Isolation | Dirty Read | Non-Repeatable Read | Phantom Read | Performance |
|-----------|-----------|-------------------|-------------|-------------|
| `READ_UNCOMMITTED` | Yes | Yes | Yes | Fastest |
| `READ_COMMITTED` (most DB defaults) | No | Yes | Yes | Good |
| `REPEATABLE_READ` (MySQL InnoDB default) | No | No | Yes* | Moderate |
| `SERIALIZABLE` | No | No | No | Slowest |

*MySQL InnoDB prevents phantom reads at REPEATABLE_READ via gap locking.

**Recommendation:** Use `DEFAULT` (database default) unless you have a specific consistency requirement. Override isolation per-method only when needed.

## Rollback Rules

### Default Behavior

- `RuntimeException` and `Error` → rollback
- Checked `Exception` → **NO rollback** (commit)

This surprises most developers. A `ParseException` or `IOException` will commit the transaction.

### Recommended: Rollback on All Exceptions

```java
// Global (Spring 6.2+)
@EnableTransactionManagement(rollbackOn = ALL_EXCEPTIONS)

// Per-method
@Transactional(rollbackFor = Exception.class)

// Excluding specific exceptions
@Transactional(rollbackFor = Exception.class, noRollbackFor = OptimisticLockingException.class)
```

### Transaction Status Inspection

```java
@Transactional
public void process() {
    // Check if current transaction is marked for rollback
    if (TransactionAspectSupport.currentTransactionStatus().isRollbackOnly()) {
        throw new IllegalStateException("Transaction already marked for rollback");
    }
}
```

## Read-Only Transactions

```java
@Transactional(readOnly = true)
public Report generateReport(Long id) {
    // Hibernate: flush mode set to MANUAL (no dirty checking)
    // Some databases: route to read replica
    // PostgreSQL: enables snapshot isolation
}
```

**Rules:**
- Use `readOnly = true` for all query-only methods — it's an optimization hint, not a constraint
- Some databases throw if you write within a read-only transaction; others silently ignore
- Read-only transactions still acquire connections — keep them short

## Multi-Datasource Transactions

### Qualifier-Based

```java
@Configuration(proxyBeanMethods = false)
class DataSourceConfig {
    @Bean
    @Qualifier("orders")
    DataSource ordersDataSource() { /* ... */ }

    @Bean
    @Qualifier("orders")
    PlatformTransactionManager ordersTransactionManager(@Qualifier("orders") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }
}

@Service
class OrderService {
    @Transactional("orders")    // Uses the orders transaction manager
    public void createOrder(Order order) { /* ... */ }
}
```

### Composed Annotation

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional("orders")
public @interface OrdersTransactional {}

@Service
class OrderService {
    @OrdersTransactional
    public void createOrder(Order order) { /* ... */ }
}
```

### Distributed Transactions

Avoid XA/JTA distributed transactions across databases. Instead:
- Use the **Outbox Pattern** (write to outbox table in same TX, publish asynchronously)
- Use **Saga Pattern** (compensating transactions for multi-service workflows)
- Use **`REQUIRES_NEW`** for independent operations that can tolerate partial failure

## Common Debugging

### "Transaction silently ignored"

Check in this order:
1. **Self-invocation** — calling `this.method()` bypasses proxy
2. **Private method** — `@Transactional` on private methods is silently ignored
3. **No `@EnableTransactionManagement`** — Spring Boot adds this automatically, but custom setups might miss it
4. **Wrong transaction manager** — multi-datasource setup using the wrong qualifier

### "Transaction already active"

```
org.springframework.transaction.IllegalTransactionStateException:
Pre-bound JDBC Connection found!
```

Usually caused by:
- Manual `DataSource.getConnection()` that wasn't closed
- Thread-local connection leak from a previous request
- Test method not cleaning up `@Transactional` test context

### "Read-only transaction committed changes"

Verify:
- The method is actually using `readOnly = true` (not just the class-level annotation)
- The database supports read-only mode (H2 in-memory ignores it)
- Hibernate's `FlushMode.MANUAL` is active (check `Session.getFlushMode()`)
