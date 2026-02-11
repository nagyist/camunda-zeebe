# Vacuum vs Spectral: Multi-Part OpenAPI Spec Validation Testing

## Problem Statement

Why didn't Vacuum catch the malformed `required: false` error (at property level instead of object level) before the PR was merged?

## Hypothesis

Vacuum can detect schema validation errors in standalone OpenAPI specs, but fails to detect them in multi-part specifications where schemas are split across multiple files with `$ref` references. Spectral, however, can detect errors in both cases.

## Test Methodology

### Test Setup

1. **Malformed Field**: Added `required: false` at property level in `deleteHistory` field

   ```yaml
   deleteHistory:
     type: boolean
     required: false  # ❌ INVALID: should be array at object level
     default: false
   ```
2. **Test Scenarios**:
   - **Standalone Spec**: Single YAML file with inline schema
   - **Multi-Part Spec**: Full repository structure (41 files) with `rest-api.yaml` referencing `deployments.yaml` via `$ref`
3. **Tools Tested**:
   - Vacuum v0.23.4 with `vacuum-ruleset.yaml` (extends `spectral:oas`)
   - Spectral CLI v6.14.3 with `.spectral.yaml` ruleset

## Test Results

### Test 1: Standalone Spec

**File**: `/tmp/test_standalone.yaml` (single file with inline schema)

#### Vacuum Result: ✅ DETECTED

```
Rule: oas3-schema
Violations: 1 error
Message: "schema invalid: `/paths//test/post/requestBody/content/application/json/schema/properties/deleteHistory/required` expected type `[array]`, got `boolean`"
Quality Score: 10/100
```

#### Spectral Result: ✅ DETECTED

```
error oas3-valid-schema-example: schema is invalid: data/required must be array
```

### Test 2: Multi-Part Spec

**Files**: `/tmp/test_multipart_spec/` (41 YAML files)
- Main: `rest-api.yaml`
- Malformed: `deployments.yaml` with `required: false` at line 371

#### Vacuum Result: ❌ NOT DETECTED

```
Errors: 0
Warnings: 383
Infos: 142
Quality Score: 25/100

NO oas3-schema error reported despite malformed field
```

#### Spectral Result: ✅ DETECTED

```
/tmp/test_multipart_spec/deployments.yaml
 151:20  error  oas3-valid-schema-example  schema is invalid: data/required must be array
         paths./resources/{resourceKey}/deletion.post.requestBody.content.application/json.schema

✖ 2 problems (2 errors, 0 warnings, 0 infos, 0 hints)
```

## Findings

### Vacuum Limitation Identified

Vacuum's `oas3-schema` validation rule has a **critical limitation**:
- ✅ Works correctly for standalone/single-file specifications
- ❌ **Fails to validate schemas referenced via external file `$ref`** in multi-part specifications
- The rule does not properly traverse or resolve external schema references during validation

### Spectral Behavior

Spectral's `oas3-valid-schema-example` rule:
- ✅ Works correctly for standalone specifications  
- ✅ **Works correctly for multi-part specifications with external `$ref`**
- Properly resolves and validates all referenced schemas regardless of file structure

## Root Cause Analysis

### Why the Error Wasn't Caught Before Merge

1. **Repository Structure**: The Camunda OpenAPI spec is a multi-part specification split across 41 YAML files
2. **Vacuum Limitation**: The CI pipeline used only Vacuum, which cannot validate external schema references
3. **Schema Location**: The malformed `required: false` was in `deployments.yaml`, referenced by `rest-api.yaml`
4. **Vacuum Behavior**: Vacuum validated `rest-api.yaml` but didn't traverse into `deployments.yaml` schemas

### Why SDK Generators Failed

SDK generators (using Pydantic, OpenAPI Generator, etc.) perform complete schema validation including:
- Resolving all `$ref` references
- Validating complete resolved schemas against JSON Schema specification
- Enforcing strict OpenAPI 3.0 compliance

This caught the error that Vacuum missed.

## Recommendations

### 1. Keep Both Tools (Current Approach) ✅

**Vacuum**: Fast linting for conventions, style, and basic validation
- Operation IDs, descriptions, examples
- Custom business rules
- Naming conventions
- Performance: Very fast

**Spectral**: Deep schema validation with external reference support
- Complete schema structure validation
- Works with multi-part specifications
- Catches JSON Schema compliance issues
- Required for complex specs

### 2. Alternative: Wait for Vacuum Fix

Monitor Vacuum project for fixes to external reference validation. However:
- No guarantee of timeline
- Current limitation is significant
- Spectral is proven and reliable

### 3. Tool Configuration

Current optimal configuration:

```yaml
# CI Pipeline
- Run Vacuum first (fast feedback on conventions)
- Run Spectral second (thorough schema validation)
- Both must pass for merge
```

## Testing Commands

### Reproduce the Test

```bash
# Create malformed multi-part spec
mkdir -p /tmp/test_multipart_spec
cp zeebe/gateway-protocol/src/main/proto/v2/*.yaml /tmp/test_multipart_spec/

# Add malformed field to deployments.yaml
sed -i '371 a\          required: false' /tmp/test_multipart_spec/deployments.yaml

# Test with Vacuum (will NOT detect)
vacuum lint /tmp/test_multipart_spec/rest-api.yaml \
  --ruleset zeebe/gateway-protocol/vacuum-ruleset.yaml \
  --functions zeebe/gateway-protocol/vacuum-rules \
  --errors

# Test with Spectral (will DETECT)
spectral lint /tmp/test_multipart_spec/rest-api.yaml \
  --ruleset zeebe/gateway-protocol/.spectral.yaml \
  --fail-severity error
```

## Conclusion

**Hypothesis: CONFIRMED** ✅

Vacuum cannot detect schema validation errors in multi-part OpenAPI specifications with external `$ref`, explaining why the malformed spec was not caught before merge. Spectral is required for comprehensive schema validation in complex, multi-file API specifications.

The dual-tool approach provides the best coverage:
- Speed and convention checking (Vacuum)
- Complete schema validation (Spectral)

## Impact

- **PR Review**: Justified keeping Spectral in CI pipeline
- **Future PRs**: All schema changes must be validated with both tools
- **Documentation**: Updated validation approach in `OPENAPI_VALIDATION.md`
- **CI Configuration**: Both tools configured in `.github/workflows/ci.yml`

