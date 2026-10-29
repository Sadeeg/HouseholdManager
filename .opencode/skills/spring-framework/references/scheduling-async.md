# Scheduling and Async Deep Dive

Cron syntax, virtual thread integration, TaskDecorator patterns, and error handling.

## @Scheduled Configuration

### Cron Syntax (6 Fields)

```
┌──── second (0-59)
│ ┌──── minute (0-59)
│ │ ┌──── hour (0-23)
│ │ │ ┌──── day of month (1-31)
│ │ │ │ ┌──── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌──── day of week (0-7 or MON-SUN, 0 and 7 = Sunday)
│ │ │ │ │ │
* * * * * *
```

**Examples:**
```java
@Scheduled(cron = "0 0 2 * * *")       // Every day at 2:00 AM
@Scheduled(cron = "0 */15 * * * *")    // Every 15 minutes
@Scheduled(cron = "0 0 9-17 * * MON-FRI")  // Hourly, business hours, weekdays
@Scheduled(cron = "${app.cleanup.cron}")    // Externalized via config
```

**Macros (Spring 5.3+):**
```java
@Scheduled(cron = "@daily")     // 0 0 0 * * *
@Scheduled(cron = "@hourly")    // 0 0 * * * *
@Scheduled(cron = "@midnight")  // 0 0 0 * * *
```

### fixedRate vs fixedDelay

```
fixedRate = 5000:
  |--task 3s--|     |--task 3s--|     |--task 3s--|
  0s          3s    5s          8s    10s

fixedDelay = 5000:
  |--task 3s--|          |--task 3s--|          |--task 3s--|
  0s          3s         8s          11s        16s
```

- `fixedRate`: next execution scheduled from the START of the previous. Tasks overlap if they run longer than the rate
- `fixedDelay`: next execution scheduled from the END of the previous. Guarantees gap between executions

### Initial Delay

```java
@Scheduled(fixedRate = 60000, initialDelay = 5000)    // Wait 5s before first execution
@Scheduled(initialDelay = 5000)                        // One-time execution after 5s (Spring 6.1+)
```

## Scheduler Configuration

**The default scheduler is single-threaded.** One slow task blocks all others.

```java
@Configuration(proxyBeanMethods = false)
class SchedulerConfig {

    @Bean
    TaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);                      // Concurrent scheduled tasks
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setErrorHandler(t -> log.error("Scheduled task failed", t));
        return scheduler;
    }
}
```

### With Virtual Threads (Java 21+)

```java
@Bean
TaskScheduler taskScheduler() {
    var scheduler = new SimpleAsyncTaskScheduler();
    scheduler.setVirtualThreads(true);
    scheduler.setThreadNamePrefix("scheduled-");
    return scheduler;
}
```

## Error Handling

**Unhandled exceptions stop future executions** of a `@Scheduled` method. Always handle errors:

```java
@Scheduled(fixedDelay = 30000)
void cleanupExpiredSessions() {
    try {
        sessionService.cleanupExpired();
    } catch (Exception e) {
        log.error("Session cleanup failed", e);
        // Task continues to be scheduled
    }
}
```

Or configure a global error handler on the `TaskScheduler`:

```java
scheduler.setErrorHandler(t -> {
    log.error("Scheduled task failed: {}", t.getMessage(), t);
    meterRegistry.counter("scheduled.task.errors").increment();
});
```

## @Async Configuration

### Custom Executor

```java
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig {

    @Bean
    TaskExecutor applicationTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        return executor;
    }
}
```

### Virtual Thread Executor (Java 21+)

```java
@Bean
TaskExecutor applicationTaskExecutor() {
    var executor = new SimpleAsyncTaskExecutor();
    executor.setVirtualThreads(true);
    executor.setThreadNamePrefix("async-");
    executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
    return executor;
}
```

No pool sizing needed — virtual threads are lightweight. But `TaskDecorator` is still essential for context propagation.

### Named Executors

```java
@Bean("emailExecutor")
TaskExecutor emailExecutor() { /* ... */ }

@Bean("reportExecutor")
TaskExecutor reportExecutor() { /* ... */ }

@Async("emailExecutor")
CompletableFuture<Void> sendEmail(Email email) { /* ... */ }

@Async("reportExecutor")
CompletableFuture<Report> generateReport(Long id) { /* ... */ }
```

### Exception Handling

```java
@Configuration(proxyBeanMethods = false)
@EnableAsync
class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Async method {} failed", method.getName(), throwable);
            meterRegistry.counter("async.errors", "method", method.getName()).increment();
        };
    }
}
```

**Only applies to `void` return methods.** Methods returning `CompletableFuture` propagate exceptions to the caller via `CompletableFuture.exceptionally()` or `handle()`.

## TaskDecorator for Context Propagation

Thread-local data (MDC, security context, trace context) does not propagate to async threads by default.

### Composite TaskDecorator (Boot 4.0)

Spring Boot 4.0 auto-composes multiple `TaskDecorator` beans via `CompositeTaskDecorator`:

```java
@Bean
@Order(1)
TaskDecorator mdcDecorator() {
    return runnable -> {
        var contextMap = MDC.getCopyOfContextMap();
        return () -> {
            MDC.setContextMap(contextMap != null ? contextMap : Map.of());
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    };
}

@Bean
@Order(2)
TaskDecorator securityDecorator() {
    return runnable -> {
        var context = SecurityContextHolder.getContext();
        return () -> {
            SecurityContextHolder.setContext(context);
            try {
                runnable.run();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    };
}
```

### ContextPropagatingTaskDecorator (Micrometer)

If using Micrometer observability, use the built-in decorator that propagates all registered `ThreadLocalAccessor` instances:

```java
@Bean
TaskDecorator taskDecorator() {
    return new ContextPropagatingTaskDecorator();    // Propagates trace context, MDC, etc.
}
```

## Observability

### Scheduled Task Observations (Boot 3.2+)

Auto-instrumented when `management.observations.annotations.enabled=true`:

```yaml
management:
  observations:
    annotations:
      enabled: true
```

Produces `spring.scheduling` observations for every `@Scheduled` invocation.

### Async Task Observations

Not auto-instrumented. Wrap with `@Observed` if needed:

```java
@Async
@Observed(name = "report.generation")
CompletableFuture<Report> generateReport(Long id) { /* ... */ }
```
