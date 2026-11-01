---
name: spring-framework
description: Guide Spring Framework core patterns — dependency injection, bean lifecycle, AOP, proxy mechanics, @Transactional, @Async, and @Configuration. Use when working with DI wiring, transaction management, aspect-oriented programming, proxy self-invocation issues, bean lifecycle hooks, or Spring Framework 7 features.
version: 0.1.0
tags: [core, dependency-injection, aop, transactions, proxies, lifecycle, async]
triggers:
  - wiring beans or resolving dependency injection issues
  - working with @Transactional propagation, isolation, or rollback
  - debugging proxy self-invocation or AOP not being applied
  - configuring @Async, scheduling, or task executors
  - understanding bean lifecycle hooks or initialization order
  - using @Configuration classes or programmatic bean registration
  - migrating to Spring Framework 7 or JSpecify
spring_versions:
  framework: ">=6.1"
  latest_targeted: "7.0"
depends_on: [spring-boot]
---

# Spring Framework

**Pattern:** Process

Core Spring patterns for dependency injection, AOP, transactions, and bean lifecycle. Targets **Framework 7** (Jakarta EE 11, Java 17+). Most guidance applies to Framework 6.1+ — version-specific features are annotated.

**Mental model:** Spring's AOP is a decorator pattern applied at runtime via proxies. Every annotation that adds behavior (`@Transactional`, `@Async`, `@Cacheable`) is a decorator — decorators only work when called through the decorated object (the proxy), not via `this`.

**Do NOT Load** for Boot-specific topics (auto-configuration, starters, profiles, actuator) — use `spring-boot`. For web, security, data, or testing — use the dedicated skill.

## When to Use

- Wiring beans, resolving injection ambiguities, or fixing circular dependencies
- Debugging why `@Transactional`, `@Async`, or `@Cacheable` is silently ignored
- Understanding proxy mechanics and the self-invocation trap
- Choosing between lifecycle hooks (`@PostConstruct` vs `SmartLifecycle`)
- Writing `@Configuration` classes or registering beans programmatically
- Using Spring Framework 7 features (`BeanRegistrar`, `@Proxyable`, `RetryTemplate`)

## Dependency Injection

Use **constructor injection exclusively** for mandatory dependencies. Spring auto-detects a single constructor without `@Autowired`.

- **`@Qualifier`** disambiguates multiple beans of the same type; **`@Primary`** designates a default
- **`ObjectProvider<T>`** for optional or multi-valued dependencies — provides `getIfAvailable()`, `getIfUnique()`, and stream support
- **`@Lazy`** on a parameter creates a lazy-resolution proxy — use to break circular dependencies as a last resort
- **More than 5 constructor parameters** signals too many responsibilities — refactor the class, do not switch to field injection

## Proxy Mechanics and Self-Invocation

This is the single most misunderstood aspect of Spring. Every `@Transactional`, `@Async`, `@Cacheable`, and `@Retryable` method works through a proxy wrapper. When a method calls another method on `this`, it bypasses the proxy.

```java
@Service
class OrderService {
    @Transactional
    public void createOrder(OrderRequest req) {
        // ...
        this.sendConfirmation(req);   // BYPASSES proxy — @Async is ignored
    }

    @Async
    public void sendConfirmation(OrderRequest req) { /* ... */ }
}
```

**Solutions (ranked by preference):**
1. **Extract to a separate bean** — cleaner design, eliminates the problem
2. **Inject self via `ObjectProvider<OrderService>`** — acceptable for simple cases
3. **`AopContext.currentProxy()`** — fragile, requires `exposeProxy = true`

Spring uses CGLIB proxies by default (subclasses the target). Implications:
- **`final` methods cannot be proxied** — AOP silently does not apply
- **`private` methods are invisible** to the proxy — `@Transactional` on private methods is silently ignored
- **Framework 7:** `@Proxyable` annotation allows per-bean proxy strategy override

For CGLIB vs JDK proxy trade-offs, all self-invocation solutions with code, and proxy gotchas (`final`, `equals`, casting), load `references/proxy-mechanics.md`.

### Debugging: AOP Annotation Silently Ignored

Check in this order:
1. **Self-invocation?** — `this.method()` bypasses proxy
2. **Method visibility?** — private = invisible to proxy
3. **Method modifier?** — `final` = CGLIB can't override
4. **Proxy created?** — verify with `AopUtils.isAopProxy(bean)`
5. **Bean class `final` or a record?** — CGLIB cannot subclass final classes or records

## @Transactional

Default: `propagation = REQUIRED`, rollback on `RuntimeException` and `Error` only.

```java
// Recommended: global rollback-on-all (Spring 6.2+)
@EnableTransactionManagement(rollbackOn = ALL_EXCEPTIONS)
```

Key rules:
- **Checked exceptions do not roll back by default** — use `rollbackOn = ALL_EXCEPTIONS` globally or `rollbackFor` per-method
- **`readOnly = true`** enables optimizations (Hibernate flush mode MANUAL, potential replica routing). Match to actual behavior
- **Keep transactions short** — never hold a transaction open across HTTP calls or message processing
- **`REQUIRES_NEW`** = independent commit/rollback (suspend outer); **`NESTED`** = savepoint within outer transaction

**Before adding `@Transactional`, ask:**
- What is the unit of work? Should this entire method be atomic?
- Can this method be called from another `@Transactional` method? Propagation matters
- Are there external calls inside? HTTP, messaging should be outside the transaction boundary

**Kotlin/Reactive caveats:**
- **`suspend` functions:** `@Transactional` silently does nothing on Kotlin `suspend` functions. Use `TransactionalOperator.executeAndAwait` instead
- **Mono/Flux:** Works, but `cancel` signals (from `take()`, `next()`, `timeout()`) cause rollback. Transaction context propagates via Reactor Context, not ThreadLocal

For the full propagation/isolation matrix, multi-datasource patterns, and transaction debugging, load `references/transactions.md`.

## @Configuration Classes

**Prefer lite mode:** `@Configuration(proxyBeanMethods = false)` — no CGLIB subclass, faster startup, native-image-friendly.

- **Full mode** (default): CGLIB-subclassed. Cross `@Bean` method calls return the container singleton. Use only when beans depend on each other within the same config class
- **`@Bean` in `@Component`** runs in lite mode always — cross-method calls create new instances. Common source of duplicate bean bugs
- **`@Import`** brings in additional config classes, `ImportSelector`, or `BeanRegistrar` (Framework 7)

## @Async and Task Execution

Requires `@EnableAsync`. Always return `CompletableFuture<T>` — `void` return swallows exceptions silently.

- **Always configure an executor** — default creates unbounded threads. Use `ThreadPoolTaskExecutor` or `SimpleAsyncTaskExecutor` with virtual threads
- **Virtual threads (Java 21+):** `SimpleAsyncTaskExecutor` with `setVirtualThreads(true)` — no pool sizing needed, but use `TaskDecorator` for MDC/security context propagation

For executor configuration, virtual thread integration, exception handling, and TaskDecorator patterns, load `references/scheduling-async.md`.

## Scheduling

`@EnableScheduling` + `@Scheduled`. Default scheduler is **single-threaded** — configure `ThreadPoolTaskScheduler` for concurrent tasks.

- **`fixedDelay`** — end-to-start interval (no overlap). Use for variable-duration tasks
- **`fixedRate`** — start-to-start interval (tasks overlap if slow). Use for periodic heartbeats
- **Unhandled exception stops future executions** of that task. Always wrap in try-catch

**Before writing a `@Scheduled` method, ask:** What happens if this fails? Silent retry? Alert? Graceful degradation? An unhandled exception silently kills the schedule.

For cron syntax reference and scheduler configuration, load `references/scheduling-async.md`.

## Bean Lifecycle

Initialization order: `@PostConstruct` -> `InitializingBean` -> custom init-method.

| Hook | Use When |
|------|----------|
| `@PostConstruct` | Simple initialization, no other bean access needed |
| `SmartInitializingSingleton` | Post-initialization that needs other beans to be ready |
| `SmartLifecycle` | Phase-ordered startup/shutdown (e.g., start consumers after producers) |

- **Never do blocking I/O in `@PostConstruct`** — runs inside the singleton lock, blocks startup, risks deadlock
- **Prototype beans have no destruction callbacks** — Spring does not track prototype instances

For the full 16-step lifecycle sequence, BeanPostProcessor, SmartLifecycle phases, and prototype caveats, load `references/lifecycle.md`.

## Annotation Interactions

The interactions between proxy-based features are where subtle bugs hide:

- **`@Async` + `@Transactional`** on same method — transaction runs in the async thread, independent of caller. This is intentional
- **`@Retryable` + `@Transactional`** — retry must be the OUTER advice so each retry gets a fresh transaction. Spring Retry defaults to `LOWEST_PRECEDENCE - 1` (outer) and `@EnableTransactionManagement` to `LOWEST_PRECEDENCE` (inner), so this is correct by default. If you override `order`, verify retry stays outer
- **`@Cacheable` + `@Transactional`** — both default to `LOWEST_PRECEDENCE`, so ordering is **undefined** unless explicit. Set `@EnableCaching(order = 1)` and `@EnableTransactionManagement(order = 2)` to ensure cache checks happen before opening a transaction

All of these are also subject to the self-invocation trap.

## Spring Framework 7

### BeanRegistrar (Programmatic Bean Registration)

```java
class MyBeanRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean("orderService", OrderService.class,
            spec -> spec.supplier(ctx -> new OrderService(ctx.bean(OrderRepository.class))));
    }
}
```

Import via `@Import(MyBeanRegistrar.class)`. Full AOT/native-image support. Use when conditional registration is awkward with `@Conditional` or when registering many similar beans in a loop.

### RetryTemplate in Spring Core

Spring Framework 7 adds programmatic retry support to `spring-core` via `RetryTemplate`:

```java
@Service
class PaymentService {
    private final RetryTemplate retryTemplate = RetryTemplate.builder()
        .maxAttempts(3)
        .exponentialBackoff(Duration.ofMillis(100), 2.0, Duration.ofSeconds(5))
        .build();

    Payment charge(PaymentRequest req) {
        return retryTemplate.execute(() -> doCharge(req));
    }
}
```

This is lower-level infrastructure. The declarative `@Retryable` annotation still requires the separate `spring-retry` dependency and `@EnableRetry`. The annotation-based approach is subject to proxy self-invocation rules.

### JSpecify Null-Safety

Spring migrated from `org.springframework.lang.@Nullable` to `org.jspecify.annotations.@Nullable`. Use `@NullMarked` at package level — all types are non-null by default.

## Anti-Patterns

- **The Field Injector** — `@Autowired` on fields. Untestable without reflection, hides dependencies, prevents immutability. Use constructor injection
- **The Self-Caller** — calling `this.method()` expecting proxy interception. Extract to a separate bean
- **The Silent Rollback** — assuming checked exceptions roll back `@Transactional`. They don't by default. Use `rollbackOn = ALL_EXCEPTIONS`
- **The Startup Blocker** — blocking I/O in `@PostConstruct`. Use `SmartInitializingSingleton` or `ApplicationRunner`
- **The Config Classpath Trap** — `@Bean` methods in `@Component` classes running in lite mode, creating duplicate instances on cross-references
- **The Unbounded Executor** — `@Async` without a configured executor. Creates a thread per invocation
- **The Long Transaction** — holding a database connection open across HTTP calls or message processing. Move external I/O outside the `@Transactional` boundary
- **The Advice Ordering Trap** — `@Cacheable` and `@Transactional` with undefined ordering (both default to `LOWEST_PRECEDENCE`). Set explicit `order` values on `@EnableCaching` and `@EnableTransactionManagement`
- **The Suspended Transaction** — `@Transactional` on Kotlin `suspend` functions silently does nothing. Use `TransactionalOperator` for coroutine-based code

## Resource Files

Load on demand for specific topics:

- **`references/proxy-mechanics.md`** — Deep dive on CGLIB vs JDK proxies, self-invocation solutions with code examples, @Proxyable
- **`references/transactions.md`** — Propagation/isolation matrix, rollback rules, multi-datasource patterns
- **`references/lifecycle.md`** — Full bean lifecycle sequence, BeanPostProcessor, SmartLifecycle phases
- **`references/scheduling-async.md`** — Cron syntax reference, virtual thread integration, TaskDecorator patterns

## References

- [Spring Framework Reference Documentation](https://docs.spring.io/spring-framework/reference/)
- [Spring Framework 7 Release Notes](https://github.com/spring-projects/spring-framework/wiki/What%27s-New-in-Spring-Framework-7.x)
- [Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [AOP Concepts](https://docs.spring.io/spring-framework/reference/core/aop.html)
