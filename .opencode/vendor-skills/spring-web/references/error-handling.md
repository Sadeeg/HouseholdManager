# Spring Web Error Handling — Reference

## Exception Hierarchy Pattern

Organize domain exceptions under a common base:

```java
@Getter
abstract class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}

// 404 family
class ResourceNotFoundException extends BusinessException {
    ResourceNotFoundException(String resourceType, Object id) {
        super("%s not found: %s".formatted(resourceType, id),
              HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}

// 409 family
class ConflictException extends BusinessException {
    ConflictException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}

class DuplicateOrderException extends ConflictException {
    DuplicateOrderException(String orderRef) {
        super("Order already exists: " + orderRef, "DUPLICATE_ORDER");
    }
}

class InsufficientStockException extends ConflictException {
    InsufficientStockException(String productId, int requested, int available) {
        super("Insufficient stock for %s: requested %d, available %d"
              .formatted(productId, requested, available),
              "INSUFFICIENT_STOCK");
    }
}
```

## Unified ControllerAdvice

Handle the base exception type and let polymorphism do the work:

```java
@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusinessException(BusinessException ex) {
        var problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getStatus().getReasonPhrase());
        problem.setProperty("errorCode", ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("errors", extractFieldErrors(ex));
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Constraint violation");
        problem.setTitle("Validation Error");
        problem.setProperty("errorCode", "CONSTRAINT_VIOLATION");
        problem.setProperty("errors", extractViolations(ex));
        return problem;
    }

    private Map<String, String> extractFieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (a, b) -> a + "; " + b
            ));
    }

    private Map<String, String> extractViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                v -> extractFieldName(v.getPropertyPath()),
                ConstraintViolation::getMessage,
                (a, b) -> a + "; " + b
            ));
    }

    private String extractFieldName(Path propertyPath) {
        var fieldName = "";
        for (var node : propertyPath) {
            fieldName = node.getName();
        }
        return fieldName;
    }
}
```

## Error Codes

Use structured error codes for machine-readable error identification:

```
VALIDATION_FAILED          — Bean validation failed
CONSTRAINT_VIOLATION       — Path/query parameter constraint violated
RESOURCE_NOT_FOUND         — Requested resource does not exist
DUPLICATE_ORDER            — Order with same reference already exists
INSUFFICIENT_STOCK         — Not enough inventory
PAYMENT_DECLINED           — Payment provider rejected the charge
```

Error codes are stable identifiers that clients can switch on. HTTP status codes alone are too broad — multiple error types share 409 or 400.

## ProblemDetail Response Example

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Insufficient stock for PROD-123: requested 10, available 3",
  "instance": "/api/orders",
  "errorCode": "INSUFFICIENT_STOCK"
}
```

Validation error with field details:

```json
{
  "type": "about:blank",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/orders",
  "errorCode": "VALIDATION_FAILED",
  "errors": {
    "customerName": "must not be blank",
    "totalAmount": "must be greater than 0"
  }
}
```

## Request Logging for Debugging

Log failed requests with correlation IDs for traceability:

```java
@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error [{}] {} {} — {}",
            ex.getErrorCode(), request.getMethod(), request.getRequestURI(), ex.getMessage());
        // ... build ProblemDetail
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error {} {} — {}",
            request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
```

The `handleUnexpected` handler is a **safety net only** — it logs the full stack trace and returns a generic message. All expected errors should have specific handlers above it.

## Key Guidelines

- **One `@ControllerAdvice` per application** — multiple advice classes create ordering headaches. Use method-level `@ExceptionHandler` for specificity
- **Error codes are contracts** — once published, don't rename or remove them without versioning
- **Never expose stack traces or internal details** in production error responses — use `detail` for human-readable messages, `errorCode` for machine-readable identification
- **Log at the right level**: `WARN` for business/client errors (4xx), `ERROR` for unexpected failures (5xx)
