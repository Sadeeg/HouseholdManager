# Proxy Mechanics Deep Dive

How Spring proxies work, why self-invocation fails, and all solutions with trade-offs.

## How Spring Proxies Work

When Spring detects that a bean needs AOP (e.g., `@Transactional`, `@Async`, `@Cacheable`), it wraps the bean in a proxy object. The proxy intercepts method calls and applies the cross-cutting logic before/after delegating to the actual target.

```
Caller → Proxy → Interceptor Chain → Target Object
                   ↑
         @Transactional, @Async, @Cacheable, @Retryable
```

### CGLIB Proxy (Default in Spring Boot)

- Subclasses the target class at runtime
- All non-final, non-private methods are proxied
- Does NOT require interfaces
- Cannot proxy `final` classes or `final` methods
- Adds ~1KB per proxied class to metaspace

### JDK Dynamic Proxy

- Implements the target's interfaces
- Only interface-declared methods are proxied
- Cannot be cast to the implementation class — only to the interface
- Slightly faster proxy creation, slightly slower invocation

### Choosing a Strategy

Spring Boot defaults to CGLIB (`spring.aop.proxy-target-class=true`). This is correct for most applications.

**Framework 7 `@Proxyable`:**
```java
@Service
@Proxyable(ProxyStyle.INTERFACES)    // Force JDK dynamic proxy for this bean
class OrderServiceImpl implements OrderService { }
```

Global override via `ProxyConfig` bean:
```java
@Bean
ProxyConfig proxyConfig() {
    return ProxyConfig.ofTargetClass();    // or ofInterfaces()
}
```

## The Self-Invocation Problem

When a method calls another method on `this`, it calls the target object directly, bypassing the proxy:

```java
@Service
class ReportService {

    @Transactional(readOnly = true)
    public Report generate(Long id) {
        var data = loadData(id);
        this.archive(data);           // ⚠ Calls target directly — @Transactional is ignored
        return data;
    }

    @Transactional                    // This annotation has NO effect when called from generate()
    public void archive(ReportData data) {
        repository.archive(data);     // Runs in the OUTER read-only transaction
    }
}
```

### Solution 1: Extract to Separate Bean (Recommended)

```java
@Service
class ReportService {
    private final ReportArchiver archiver;

    ReportService(ReportArchiver archiver) {
        this.archiver = archiver;
    }

    @Transactional(readOnly = true)
    public Report generate(Long id) {
        var data = loadData(id);
        archiver.archive(data);       // ✅ Goes through archiver's proxy
        return data;
    }
}

@Service
class ReportArchiver {
    @Transactional
    public void archive(ReportData data) {
        repository.archive(data);     // ✅ Own transaction
    }
}
```

### Solution 2: Self-Injection via ObjectProvider

```java
@Service
class ReportService {
    private final ObjectProvider<ReportService> self;

    ReportService(ObjectProvider<ReportService> self) {
        this.self = self;
    }

    @Transactional(readOnly = true)
    public Report generate(Long id) {
        var data = loadData(id);
        self.getObject().archive(data);    // ✅ Goes through proxy
        return data;
    }

    @Transactional
    public void archive(ReportData data) { /* ... */ }
}
```

**Trade-off:** Keeps everything in one class but creates a self-referencing dependency. Acceptable for simple cases.

### Solution 3: AopContext.currentProxy() (Not Recommended)

```java
@EnableAspectJAutoProxy(exposeProxy = true)
@Configuration
class AopConfig {}

@Service
class ReportService {
    @Transactional(readOnly = true)
    public Report generate(Long id) {
        var data = loadData(id);
        ((ReportService) AopContext.currentProxy()).archive(data);    // ✅ But fragile
        return data;
    }
}
```

**Trade-offs:** Requires `exposeProxy = true`, uses `ThreadLocal` (does not work with virtual threads out of the box), couples code to Spring AOP internals.

### Solution 4: AspectJ Compile/Load-Time Weaving (Rarely Needed)

Weaves aspects directly into the bytecode — no proxy at all. `this` calls are intercepted.

```java
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableAsync(mode = AdviceMode.ASPECTJ)
```

Requires `aspectj-maven-plugin` or `spring-instrument` Java agent. Adds build complexity. Use only when proxy-based AOP is fundamentally insufficient (e.g., `private` method interception).

## Proxy Gotchas

### final Methods

```java
@Service
class OrderService {
    @Transactional
    public final Order create(OrderRequest req) {    // ⚠ CGLIB cannot override
        // @Transactional silently ignored
    }
}
```

Remove `final` from proxied methods. If the class itself is `final`, the bean cannot be proxied with CGLIB at all.

### equals/hashCode

Proxy delegates `equals()` and `hashCode()` to the target, but identity checks (`==`) fail:

```java
OrderService a = context.getBean(OrderService.class);
OrderService b = context.getBean(OrderService.class);
a == b;           // true (same proxy)
a.equals(b);      // true (delegates to target)

// But if comparing proxy to raw target:
OrderService proxy = context.getBean(OrderService.class);
OrderService raw = /* somehow get the raw target */;
proxy == raw;     // false
```

### Type Casting with JDK Proxies

```java
// With JDK proxy (interface-based):
OrderService proxy = context.getBean(OrderService.class);    // Returns the interface
OrderServiceImpl impl = (OrderServiceImpl) proxy;            // ⚠ ClassCastException!
```

Always depend on interfaces when using JDK proxies. CGLIB does not have this issue.

## Affected Annotations (All Subject to Self-Invocation)

| Annotation | Module |
|------------|--------|
| `@Transactional` | spring-tx |
| `@Async` | spring-context |
| `@Cacheable` / `@CacheEvict` | spring-context |
| `@Retryable` | spring-retry (annotation), spring-core (RetryTemplate) |
| Custom `@Around` aspects | spring-aop |
