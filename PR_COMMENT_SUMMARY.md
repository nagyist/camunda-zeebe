## 🔬 Discovery: Why Vacuum Didn't Catch the Malformed Spec

@tmetzke - I've completed testing to understand why Vacuum didn't catch the `required: false` error before merge. Here are the findings:

### 🎯 Key Discovery

**Vacuum v0.23.4 has a critical limitation**: Its `oas3-schema` validation rule **does not properly traverse or validate schemas referenced via external file `$ref`** in multi-part OpenAPI specifications.

### 📊 Test Results

I created comprehensive tests with both standalone and multi-part specs containing the same malformed field:

|               Test Scenario                |     Vacuum v0.23.4      |   Spectral v6.14.3   |
|--------------------------------------------|-------------------------|----------------------|
| **Standalone spec** (single file)          | ✅ Detected error        | ✅ Detected error     |
| **Multi-part spec** (41 files with `$ref`) | ❌ **0 errors reported** | ✅ **Detected error** |

### 🔍 What This Means

**Why it wasn't caught:**
1. The Camunda OpenAPI spec is split across 41 YAML files
2. The malformed `required: false` was in `deployments.yaml` (line 371)
3. Vacuum validated `rest-api.yaml` but **never traversed into the referenced `deployments.yaml` schemas**
4. Result: 0 errors reported despite the malformed field being present

**Why SDK generators failed:**
- SDK generators (Pydantic, OpenAPI Generator, etc.) perform complete schema validation
- They resolve ALL `$ref` references across files
- They enforce strict JSON Schema compliance
- This caught what Vacuum missed

### ✅ Recommendation: Keep Both Tools

The dual-tool approach is **necessary and justified**:

- **Vacuum**: Fast linting for conventions, style, custom business rules
  - Excellent for immediate feedback
  - Performance advantage
  - Good for custom validations
- **Spectral**: Essential for deep schema validation
  - **Required for multi-part specifications**
  - Properly resolves and validates external `$ref`
  - Catches JSON Schema compliance issues
  - Proven to work with our spec structure

### 📄 Documentation

Complete test methodology and reproduction steps are in:
- `zeebe/gateway-protocol/VACUUM_VS_SPECTRAL_TESTING.md`

### 🧪 Reproduction

```bash
# Create test with malformed field
cp -r zeebe/gateway-protocol/src/main/proto/v2 /tmp/test_spec
sed -i '371 a\          required: false' /tmp/test_spec/deployments.yaml

# Vacuum: Reports 0 errors (misses it)
vacuum lint /tmp/test_spec/rest-api.yaml --ruleset vacuum-ruleset.yaml --errors

# Spectral: Detects the error
spectral lint /tmp/test_spec/rest-api.yaml --ruleset .spectral.yaml --fail-severity error
# Result: deployments.yaml:151:20 error - schema is invalid: data/required must be array
```

### 💡 Conclusion

This isn't about Vacuum being "worse" - it's about **tool limitations for specific use cases**. Multi-part OpenAPI specs with external references require a validator that can traverse those references. Spectral handles this; Vacuum (currently) doesn't.

Both tools serve important purposes in our validation strategy.

---

cc: @jwulf for awareness of the testing results
