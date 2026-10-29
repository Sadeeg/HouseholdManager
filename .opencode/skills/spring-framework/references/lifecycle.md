# Bean Lifecycle Deep Dive

Full bean lifecycle sequence, initialization hooks, and lifecycle management patterns.

## Complete Lifecycle Sequence

```
1. BeanDefinition registered
2. BeanFactoryPostProcessor.postProcessBeanFactory()     ← modify definitions
3. Bean instantiation (constructor)
4. Dependency injection (setter/@Autowired)
5. BeanPostProcessor.postProcessBeforeInitialization()   ← @PostConstruct here
6. @PostConstruct
7. InitializingBean.afterPropertiesSet()
8. Custom init-method
9. BeanPostProcessor.postProcessAfterInitialization()    ← proxy creation here
10. SmartInitializingSingleton.afterSingletonsInstantiated()  ← all singletons ready
11. SmartLifecycle.start()                                ← phase-ordered start
12. ApplicationReadyEvent                                 ← app fully started
    ─── application running ───
13. SmartLifecycle.stop()                                 ← phase-ordered stop
14. @PreDestroy
15. DisposableBean.destroy()
16. Custom destroy-method
```

## Choosing the Right Hook

| Need | Hook | Why |
|------|------|-----|
| Simple field validation | `@PostConstruct` | Runs early, before proxy creation |
| Access other beans after init | `SmartInitializingSingleton` | All singletons guaranteed ready |
| Ordered startup (e.g., consumers after producers) | `SmartLifecycle` | Phase-based ordering with start/stop |
| One-time post-startup task | `ApplicationRunner` / `CommandLineRunner` | Runs after full context initialization |
| Resource cleanup on shutdown | `@PreDestroy` | Called during graceful shutdown |
| Phased graceful shutdown | `SmartLifecycle.stop(Runnable)` | Ordered, async-capable shutdown |

## @PostConstruct Constraints

```java
@Service
class CacheWarmer {

    @PostConstruct
    void warm() {
        // ⚠ Runs inside singleton lock
        // ⚠ Other singletons may not be initialized yet
        // ⚠ Proxy is NOT yet created (step 6, proxy at step 9)
        // ✅ OK: validate fields, compute derived state
        // ❌ BAD: HTTP calls, database queries, publishing events
    }
}
```

**Framework 7:** `@PostConstruct` is from `jakarta.annotation` (not `javax.annotation`).

## SmartInitializingSingleton

Runs after ALL singletons are initialized — safe to access any bean:

```java
@Component
class CacheWarmer implements SmartInitializingSingleton {

    private final ProductRepository products;
    private final CacheManager cache;

    @Override
    public void afterSingletonsInstantiated() {
        // ✅ All beans ready, proxies created
        // ✅ Safe to call @Transactional methods
        var allProducts = products.findAll();
        cache.getCache("products").putAll(allProducts);
    }
}
```

## SmartLifecycle

Phase-ordered startup and shutdown. Lower phase starts first, stops last.

```java
@Component
class KafkaConsumerLifecycle implements SmartLifecycle {

    private volatile boolean running;

    @Override
    public int getPhase() {
        return 100;    // Higher phase = starts later, stops earlier
    }

    @Override
    public void start() {
        // Start consuming
        running = true;
    }

    @Override
    public void stop(Runnable callback) {
        // Graceful shutdown — MUST call callback when done
        stopConsuming();
        running = false;
        callback.run();    // Signal completion
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
```

**Critical:** Always call the `Runnable` callback in `stop(Runnable)`. Failing to do so hangs the shutdown process until timeout.

### Phase Ordering Example

```
Phase 0:   Database connection pools → start first, stop last
Phase 10:  Cache warmers
Phase 50:  HTTP server (Spring Boot default)
Phase 100: Message consumers → start last, stop first
```

## BeanPostProcessor

Intercepts every bean creation. Used by Spring internals — rarely needed in application code.

```java
@Component
class MetricsPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof MetricsAware ma) {
            ma.setRegistry(meterRegistry);
        }
        return bean;    // Must return the bean (or a wrapper)
    }
}
```

**Warning:** `BeanPostProcessor` beans are initialized early. Their dependencies are also initialized early — potentially before other `BeanPostProcessor`s run. Avoid complex dependency trees.

## BeanFactoryPostProcessor

Modifies bean definitions before any beans are created:

```java
@Component
class TimeoutDefaultsPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
        // Modify bean definitions — not instances
        // Runs before any bean instantiation
    }
}
```

Use cases: modifying property sources, registering additional bean definitions, adjusting scope or lazy-init defaults.

## Prototype Scope Caveats

```java
@Scope("prototype")
@Component
class RequestProcessor { /* new instance per injection */ }
```

- **No destruction callbacks** — Spring does not track prototype instances. `@PreDestroy` is never called
- **Injection into singletons** creates a single instance that is reused. Use `ObjectProvider<RequestProcessor>` to get a fresh instance each time
- **Memory leaks** — prototype beans that hold resources need manual cleanup

```java
@Service
class OrderService {
    private final ObjectProvider<RequestProcessor> processorProvider;

    void handle(Order order) {
        RequestProcessor processor = processorProvider.getObject();    // Fresh instance
        processor.process(order);
        processor.close();    // Manual cleanup required
    }
}
```

## ApplicationRunner vs CommandLineRunner

Both run after full context initialization. Prefer `ApplicationRunner` — it provides parsed `ApplicationArguments`.

```java
@Component
@Order(1)    // Lower = runs first
class DataMigrationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("migrate")) {
            // Run migration
        }
    }
}
```

Use for: one-time startup tasks, data migrations, cache warming, health check validation.
