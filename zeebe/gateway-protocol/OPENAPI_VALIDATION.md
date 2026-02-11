# OpenAPI Validation

This directory contains the OpenAPI specifications for the Camunda REST API and related validation tools.

## Validation Tools

We use two complementary tools to validate the OpenAPI specifications:

### 1. Vacuum

- **Purpose**: Style, conventions, and custom business rules
- **Ruleset**: `vacuum-ruleset.yaml`
- **Custom Rules**: `vacuum-rules/` directory
- **What it catches**:
  - Custom naming conventions (e.g., no "flow node" terminology)
  - Property description requirements
  - Key property type validation
  - General OpenAPI structure issues

### 2. Spectral CLI

- **Purpose**: Strict JSON Schema validation
- **Ruleset**: `.spectral.yaml`
- **What it catches**:
  - Schema structure errors (e.g., `required` field misplacement)
  - Invalid schema property combinations
  - Type mismatches in schema definitions
  - Schema compliance with OpenAPI 3.0 JSON Schema spec

## Why Both Tools?

- **Vacuum** is great for custom business rules and conventions specific to our API
- **Spectral** provides stricter JSON Schema validation that catches structural errors Vacuum may miss

Example: Vacuum didn't catch `required: false` at property level in a schema (should be an array at object level), but Spectral's `oas3-valid-schema-example` rule caught it immediately.

## Running Validation Locally

### Run Vacuum

```bash
# From repository root
./vacuum lint zeebe/gateway-protocol/src/main/proto/v2/rest-api.yaml \
  --ruleset zeebe/gateway-protocol/vacuum-ruleset.yaml \
  --ignore-file zeebe/gateway-protocol/vacuum-ignores.yaml \
  --functions zeebe/gateway-protocol/vacuum-rules \
  --details --no-clip --errors
```

### Run Spectral

```bash
# Install Spectral CLI (if not already installed)
npm install -g @stoplight/spectral-cli

# Run validation
spectral lint zeebe/gateway-protocol/src/main/proto/v2/rest-api.yaml \
  --ruleset zeebe/gateway-protocol/.spectral.yaml \
  --fail-severity error
```

## Common Issues

### `required: false` at Property Level

**Wrong:**

```yaml
properties:
  myField:
    type: string
    required: false  # ❌ Invalid!
```

**Correct:**

```yaml
properties:
  myField:
    type: string
# If field is optional, simply don't include it in the required array at object level
# If field is required:
required:
  - myField  # ✓ Correct - array at object level
```

## CI Integration

Both validators run automatically in the CI pipeline when OpenAPI files are modified. See `.github/workflows/ci.yml` for the `openapi-lint` job.
