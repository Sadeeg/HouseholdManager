---
name: spring-web
description: Guide Spring Web MVC for REST controllers, request validation, error handling, and API conventions. Use when creating REST endpoints, handling validation, or designing consistent API responses.
version: 0.1.0
tags: [rest, controllers, validation, error-handling]
triggers:
  - creating REST controllers or endpoints
  - implementing request/response validation
  - setting up global error handling
  - configuring CORS
  - designing REST API conventions
spring_versions:
  boot: ">=3.2"
depends_on: [spring-boot]
---

# Spring Web

Conventions for building REST APIs with Spring Web MVC in Spring Boot 3.2+.

**Do NOT Load** for reactive web applications (WebFlux) or GraphQL APIs — use the dedicated skill.

## Controller Design

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @GetMapping
    List<OrderSummary> findAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    OrderDetail findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderDetail create(@RequestBody @Valid CreateOrderRequest request) {
        return orderService.create(request);
    }

    @PutMapping("/{id}")
    OrderDetail update(@PathVariable Long id, @RequestBody @Valid UpdateOrderRequest request) {
        return orderService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
```

### Controller Rules

- **One controller per resource** — `OrderController` handles `/api/orders/**`
- **Controllers are thin** — delegate to services immediately. No business logic, no persistence calls, no conditional branching
- **Return DTOs, never entities** — see spring-data-jpa skill
- **Use `@ResponseStatus`** for non-200 success codes (`CREATED`, `NO_CONTENT`)
- **Use `ResponseEntity` only when** the status code varies at runtime (e.g., `CREATED` vs `OK` for upserts)

## Request and Response DTOs

Use records for immutable, concise DTOs:

```java
record CreateOrderRequest(
    @NotBlank String customerName,
    @NotNull @Positive BigDecimal totalAmount,
    @NotEmpty List<@Valid OrderItemRequest> items
) {}

record OrderItemRequest(
    @NotNull Long productId,
    @NotNull @Min(1) Integer quantity
) {}

record OrderDetail(
    Long id,
    String customerName,
    BigDecimal totalAmount,
    OrderStatus status,
    List<OrderItemDetail> items,
    Instant createdAt
) {}
```

- **Request DTOs** carry validation annotations
- **Response DTOs** are plain records — no validation needed
- **Never reuse the same DTO for request and response** — they evolve independently

## Validation

### Bean Validation with @Valid

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
OrderDetail create(@RequestBody @Valid CreateOrderRequest request) {
    return orderService.create(request);
}
```

Spring auto-validates the request body and throws `MethodArgumentNotValidException` on failure. Handle it in your `@ControllerAdvice` (see Error Handling).

### Path Variables and Request Parameters

```java
@GetMapping("/{id}")
OrderDetail findById(@PathVariable @Positive Long id) { /* ... */ }

@GetMapping
List<OrderSummary> search(
    @RequestParam(required = false) OrderStatus status,
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "20") @Max(100) int size
) { /* ... */ }
```

Add `@Validated` on the controller class to enable validation on `@PathVariable` and `@RequestParam`:

```java
@RestController
@RequestMapping("/api/orders")
@Validated                          // Enables method parameter validation
@RequiredArgsConstructor
class OrderController { /* ... */ }
```

### Custom Validation

For cross-field validation, create a custom constraint:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@interface ValidDateRange {
    String message() default "End date must be after start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRangeRequest> {
    @Override
    public boolean isValid(DateRangeRequest request, ConstraintValidatorContext context) {
        return request.startDate().isBefore(request.endDate());
    }
}
```

## Error Handling

### ProblemDetail (RFC 9457)

Spring Boot 3.x natively supports ProblemDetail — the standard error response format. Enable it:

```yaml
spring.mvc.problemdetails.enabled: true
```

This auto-converts standard Spring exceptions (validation errors, 404s, 405s) to RFC 9457 JSON:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed for argument [0]",
  "instance": "/api/orders"
}
```

### @ControllerAdvice for Custom Exceptions

```java
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Order Not Found");
        problem.setProperty("orderId", ex.getOrderId());     // Custom extension field
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");

        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (a, b) -> a + "; " + b       // Merge multiple errors per field
            ));
        problem.setProperty("errors", errors);
        return problem;
    }
}
```

### Exception Design

```java
// Base class with HTTP status
@Getter
abstract class BusinessException extends RuntimeException {
    private final HttpStatus status;

    protected BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

class OrderNotFoundException extends BusinessException {
    @Getter private final String orderId;

    OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId, HttpStatus.NOT_FOUND);
        this.orderId = orderId;
    }
}

class InsufficientStockException extends BusinessException {
    InsufficientStockException(String productId) {
        super("Insufficient stock for product: " + productId, HttpStatus.CONFLICT);
    }
}
```

See `references/error-handling.md` for advanced patterns.

## HTTP Status Codes

| Operation | Success Status | When |
|-----------|---------------|------|
| GET | `200 OK` | Resource found |
| GET | `404 Not Found` | Resource not found |
| POST (create) | `201 Created` | Resource created |
| PUT/PATCH | `200 OK` | Resource updated, return body |
| DELETE | `204 No Content` | Resource deleted, no body |
| POST (action) | `200 OK` or `202 Accepted` | Action performed / queued |
| Validation failure | `400 Bad Request` | Invalid input |
| Auth failure | `401 Unauthorized` | Missing/invalid credentials |
| Authz failure | `403 Forbidden` | Valid credentials, insufficient permissions |
| Business rule violation | `409 Conflict` | State conflict (duplicate, insufficient stock) |

## CORS (Without Spring Security)

If your application does **not** use Spring Security, configure CORS via `WebMvcConfigurer`:

```java
@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://app.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("Authorization", "Content-Type")
            .maxAge(3600);
    }
}
```

If you **do** use Spring Security, configure CORS in the `SecurityFilterChain` instead — see the spring-security skill.

## Key Decisions

### ResponseEntity vs Direct Return

| Approach | Use When |
|----------|----------|
| Direct return (`OrderDetail`) | Status code is always the same (200 or via `@ResponseStatus`) |
| `ResponseEntity<OrderDetail>` | Status code varies at runtime, or you need custom headers |

Prefer direct returns — they're simpler. Use `ResponseEntity` for upserts or when returning `Location` headers:

```java
@PostMapping
ResponseEntity<OrderDetail> create(@RequestBody @Valid CreateOrderRequest request,
                                    UriComponentsBuilder ucb) {
    var order = orderService.create(request);
    var location = ucb.path("/api/orders/{id}").buildAndExpand(order.id()).toUri();
    return ResponseEntity.created(location).body(order);
}
```

### Wrapping Responses

Do **not** wrap every response in a generic `ApiResponse<T>` or `ResponseWrapper<T>`. Use ProblemDetail for errors and plain DTOs for success. Wrappers add noise, complicate client code, and duplicate what HTTP already provides (status codes, headers).

## NEVER

- **Never put business logic in controllers** — controllers map HTTP to service calls. If you see `if/else`, loops, or repository calls in a controller, move it to a service
- **Never return JPA entities from controllers** — entities expose internal state, trigger lazy loading outside transactions, and create Jackson serialization cycles with bidirectional relationships
- **Never catch `Exception.class` in `@ControllerAdvice` and return 500** — handle specific exceptions. Unhandled exceptions already produce 500 via Spring's default. A catch-all swallows stack traces and hides bugs
- **Never use `Map<String, Object>` as request or response type** — it's unvalidatable, undocumented, and invisible to OpenAPI generators. Use typed records
- **Never mix `@Controller` and `@RestController` on the same class** — pick one. `@RestController` implies `@ResponseBody` on every method
- **Never validate in the controller method body** — use Bean Validation annotations on DTOs. Manual `if (name == null) throw ...` duplicates what `@NotNull` + `@Valid` does automatically
- **Never return `200 OK` for everything** — use appropriate HTTP status codes. Returning 200 with `{"success": false, "error": "..."}` breaks HTTP semantics and confuses clients

## Reference Files

- **`references/error-handling.md`** — Advanced error handling: exception hierarchy patterns, i18n error messages, error codes, request logging

## References

- [Spring Web MVC Reference](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
- [Bean Validation (Hibernate Validator)](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/)
- [RFC 9457 — Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
