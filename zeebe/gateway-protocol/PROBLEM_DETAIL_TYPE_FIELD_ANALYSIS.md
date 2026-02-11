# Problem Detail Type Field Analysis

## Background

The `type` field in RFC 9457 Problem Detail responses is currently set to `about:blank` in all error responses from the Camunda REST API. This document analyzes the purpose of this field and proposes how to populate it with meaningful values.

## Purpose of the Type Field (RFC 9457)

According to [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457), the `type` field serves several critical purposes:

### 1. Machine-Readable Error Classification

The `type` field is a **URI reference** that uniquely identifies the category of problem. This allows clients to:
- Programmatically recognize specific error scenarios
- Implement error-specific handling logic
- Build robust error recovery mechanisms

### 2. Human-Readable Documentation Link

Ideally, the URI should be **dereferenceable** (resolvable to a web page) that provides:
- Detailed explanation of the error
- Common causes and troubleshooting steps
- Examples and recovery suggestions
- API version-specific guidance

### 3. Promotes API Consistency

By standardizing error types across the API:
- Clients can reuse error-handling logic
- Developers have clearer expectations
- Documentation becomes more discoverable
- Cross-team integration is easier

## RFC 9457 Best Practices

### Use Stable URIs

```json
{
  "type": "https://docs.camunda.io/errors/resource-not-found",
  "title": "Process Definition Not Found",
  "status": 404,
  "detail": "Process definition with key '12345' does not exist",
  "instance": "/v2/process-definitions/12345"
}
```

### When to Use `about:blank`

`about:blank` should **only** be used when:
- No specific error type is applicable
- The error is truly generic (e.g., network failures, timeouts)
- You're returning a standard HTTP status without additional context

For application-specific errors (validation failures, business logic errors, etc.), use specific type URIs.

## Current Implementation Analysis

### Where ProblemDetail is Created

`GatewayErrorMapper.createProblemDetail()` creates the ProblemDetail:

```java
public static ProblemDetail createProblemDetail(
    final HttpStatusCode status, final String detail, final String title) {
  final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
  problemDetail.setTitle(title);
  return problemDetail;
}
```

**Issue**: The `type` field is never set, so it defaults to `about:blank` for ALL errors.

### Error Categories in Camunda

Based on `ServiceException.Status` enum, we have these error categories:

1. **INVALID_ARGUMENT** → 400 Bad Request
2. **NOT_FOUND** → 404 Not Found
3. **ALREADY_EXISTS** → 409 Conflict
4. **INVALID_STATE** → 409 Conflict
5. **FORBIDDEN** → 403 Forbidden
6. **UNAUTHORIZED** → 401 Unauthorized
7. **RESOURCE_EXHAUSTED** → 503 Service Unavailable
8. **UNAVAILABLE** → 503 Service Unavailable
9. **DEADLINE_EXCEEDED** → 504 Gateway Timeout
10. **ABORTED** → 502 Bad Gateway
11. **INTERNAL** → 500 Internal Server Error
12. **UNKNOWN** → 500 Internal Server Error

## Proposed Solution

### Option 1: Map ServiceException Status to Type URIs (Recommended)

Update `GatewayErrorMapper` to set specific type URIs:

```java
public static ProblemDetail createProblemDetail(
    final HttpStatusCode status, final String detail, final String title) {
  final var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
  problemDetail.setTitle(title);
  
  // Set meaningful type URI based on error category
  problemDetail.setType(determineTypeUri(title));
  
  return problemDetail;
}

private static URI determineTypeUri(String title) {
  // Map ServiceException.Status names to type URIs
  return switch (title) {
    case "INVALID_ARGUMENT" -> URI.create("https://docs.camunda.io/errors/invalid-argument");
    case "NOT_FOUND" -> URI.create("https://docs.camunda.io/errors/not-found");
    case "ALREADY_EXISTS" -> URI.create("https://docs.camunda.io/errors/already-exists");
    case "INVALID_STATE" -> URI.create("https://docs.camunda.io/errors/invalid-state");
    case "FORBIDDEN" -> URI.create("https://docs.camunda.io/errors/forbidden");
    case "UNAUTHORIZED" -> URI.create("https://docs.camunda.io/errors/unauthorized");
    case "RESOURCE_EXHAUSTED" -> URI.create("https://docs.camunda.io/errors/resource-exhausted");
    case "UNAVAILABLE" -> URI.create("https://docs.camunda.io/errors/unavailable");
    case "DEADLINE_EXCEEDED" -> URI.create("https://docs.camunda.io/errors/deadline-exceeded");
    case "ABORTED" -> URI.create("https://docs.camunda.io/errors/aborted");
    case "INTERNAL" -> URI.create("https://docs.camunda.io/errors/internal-error");
    case "UNKNOWN" -> URI.create("https://docs.camunda.io/errors/unknown-error");
    default -> URI.create("about:blank"); // Fallback for unexpected errors
  };
}
```

### Option 2: Use Relative URIs (Simpler, Less Maintenance)

Use relative URIs that don't require external documentation:

```java
private static URI determineTypeUri(String title) {
  // Use relative URIs under /errors/
  return switch (title) {
    case "INVALID_ARGUMENT" -> URI.create("/errors/invalid-argument");
    case "NOT_FOUND" -> URI.create("/errors/not-found");
    // ... etc
    default -> URI.create("about:blank");
  };
}
```

**Pros**:
- No dependency on external docs infrastructure
- URIs are stable and under our control
- Can still be documented in OpenAPI spec

**Cons**:
- Not dereferenceable (can't click the link for help)
- Less discoverable

### Option 3: Per-Operation Type URIs (Most Detailed)

For more granular error types, include operation context:

```
/errors/process-definition/not-found
/errors/process-instance/invalid-state
/errors/user-task/already-completed
```

**Pros**:
- Very specific error identification
- Enables fine-grained error handling
- Better for debugging

**Cons**:
- More complex implementation
- Requires refactoring how errors are created
- More type URIs to document

## Recommended Approach

**Start with Option 1** (ServiceException Status mapping) because:

1. **Minimal code changes** - only update `GatewayErrorMapper`
2. **Immediate value** - clients can distinguish error categories
3. **Backward compatible** - doesn't break existing clients
4. **Extensible** - can evolve to Option 3 later
5. **Follows RFC 9457** - uses stable, meaningful type URIs

### Implementation Steps

1. Update `GatewayErrorMapper.createProblemDetail()` to set type URIs
2. Update OpenAPI `problem-detail.yaml` with better examples:

   ```yaml
   type:
     type: string
     format: uri
     description: A URI identifying the problem type. See https://docs.camunda.io/errors for details.
     example: https://docs.camunda.io/errors/invalid-argument
   ```
3. Document error types in OpenAPI spec or external docs
4. Add tests to verify type URIs are set correctly
5. Update client libraries to expose type field

### Future Enhancements

1. **Create documentation pages** for each error type
2. **Add error codes** as extension properties (e.g., `CAM-1001`)
3. **Include recovery suggestions** in the detail field
4. **Track error metrics** by type URI
5. **Version the error types** (e.g., `/v2/errors/...`)

## Impact on Current Issue

The Spectral validation error about `about:blank` is actually revealing a **missed opportunity** rather than a bug:

- **`about:blank` is valid** per RFC 9457
- **But it's not ideal** - we should use specific type URIs
- **The false positive** happens because Spectral's URI format validator is overly strict

### Short-term Fix for Spectral

Update the example in `problem-detail.yaml`:

```yaml
type:
  type: string
  format: uri
  description: A URI identifying the problem type.
  default: about:blank
  example: https://docs.camunda.io/errors/invalid-argument  # Changed from about:blank
```

This:
- ✅ Passes Spectral validation
- ✅ Shows clients the intended usage pattern
- ✅ Documents that specific URIs should be used
- ✅ Doesn't require code changes immediately

## References

- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [Spring Framework ProblemDetail Support](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Best Practices for API Error Handling](https://zuplo.com/learning-center/best-practices-for-api-error-handling)

