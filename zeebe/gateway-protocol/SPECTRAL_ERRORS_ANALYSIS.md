# Spectral Validation Errors - Analysis and Resolution

## Summary

**Initial State**: 172 Spectral validation errors  
**Current State**: 1 error (unrelated to ProblemDetail)  
**Resolution**: Fixed ProblemDetail schema examples

## What Were the 172 Errors?

All 172 errors were identical in nature:

```
oas3-valid-schema-example: "example" property must match format "uri"
```

### Root Cause

The errors originated from a single source: the `ProblemDetail` schema in `problem-detail.yaml`:

```yaml
# BEFORE (caused 172 errors)
properties:
  type:
    type: string
    format: uri
    description: A URI identifying the problem type.
    default: about:blank
    example: about:blank  # ❌ This was the problem
```

### Why Did This Cause 172 Errors?

1. **Cascading Effect**: The `ProblemDetail` schema is referenced in error responses across the entire API
2. **Response Distribution**: 172 different error responses (401, 403, 404, 500, 503) reference `ProblemDetail`
3. **Validation Propagation**: When Spectral validates the complete `rest-api.yaml`, it validates all references

### Why Was `about:blank` Flagged?

According to RFC 3986, `about:blank` IS a valid URI. However:

- **Spectral's Strict Validation**: Uses JSON Schema's URI format validator which is very strict
- **URI Scheme Recognition**: The `about:` scheme is less common and not always recognized
- **Format Validator Behavior**: Some validators don't accept all valid URI schemes

This is technically a **false positive** - `about:blank` is valid per RFC 9457 and RFC 3986.

## The Fix

Updated `problem-detail.yaml` with examples that pass validation:

```yaml
# AFTER (passes validation)
properties:
  type:
    type: string
    format: uri
    description: |
      A URI identifying the problem type. Ideally, this URI should resolve to 
      human-readable documentation for the problem type.
      
      The default value 'about:blank' indicates a generic error without a specific type.
      Application-specific errors should use a meaningful URI.
    default: about:blank
    example: https://docs.camunda.io/errors/invalid-argument  # ✅ Valid HTTPS URI
```

### Why This Works

- **HTTPS Scheme**: Universally recognized as valid
- **Demonstrates Intent**: Shows how the field SHOULD be used in practice
- **RFC 9457 Best Practice**: Encourages meaningful, dereferenceable type URIs
- **Passes Validation**: Spectral's strict validator accepts it

## Error Distribution by File

The 172 errors were spread across these files (examples):

- `audit-logs.yaml`: ~8 errors (401, 403, 404, 500 responses)
- `authorizations.yaml`: ~15 errors
- `batch-operations.yaml`: ~12 errors
- `cluster-variables.yaml`: ~10 errors
- `decision-definitions.yaml`: ~8 errors
- `deployments.yaml`: ~8 errors
- `documents.yaml`: ~6 errors
- `element-instances.yaml`: ~8 errors
- ...and ~20 more files

Each file had errors in multiple response definitions that reference `ProblemDetail`.

## Remaining Error

There is 1 unrelated error in `expression.yaml`:

```
63:20  error  oas3-valid-schema-example  "example" property type must be object
components.schemas.ExpressionEvaluationResult.properties.result.example
```

This is a different issue where:
- The property is defined as `type: object`
- But the example is a scalar value: `example: 30`
- Should be: `example: { "value": 30 }` or remove the example

This error is unrelated to ProblemDetail and should be fixed separately.

## Lessons Learned

### 1. Schema Examples Matter

Even though examples are for documentation, validators check them against schema definitions. A single bad example in a widely-used schema can cause many errors.

### 2. Use Realistic Examples

Instead of technically-correct-but-problematic values like `about:blank`, use examples that:
- Demonstrate the intended usage pattern
- Pass common validators
- Follow best practices

### 3. Validator Strictness Varies

Different OpenAPI validators have different strictness levels:
- **vacuum**: Lenient, didn't catch the schema structure error
- **openapi-generator**: Lenient, accepted `about:blank`
- **Spectral**: Strict, flagged `about:blank` as invalid
- **Pydantic-based tools**: Very strict, caught the schema structure error

Having multiple validators (vacuum for conventions, Spectral for schema validation) provides better coverage.

## Recommendations

### Immediate

- ✅ Keep the updated ProblemDetail examples
- ✅ Use Spectral validation in CI
- ⚠️ Fix the remaining expression.yaml error

### Future

1. **Implement Meaningful Type URIs**: Update `GatewayErrorMapper` to set specific type URIs based on error category
2. **Create Error Documentation**: Make type URIs dereferenceable with actual documentation
3. **Add Examples to Common Responses**: Show specific error type examples in response definitions
4. **Review All Examples**: Audit other examples for validator compatibility

## References

- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 3986: URI Generic Syntax](https://www.rfc-editor.org/rfc/rfc3986)
- [Spectral Documentation](https://stoplight.io/open-source/spectral)
- [OpenAPI 3.0.3 Specification](https://spec.openapis.org/oas/v3.0.3)

