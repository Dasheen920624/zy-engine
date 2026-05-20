# AI Quality Review

review_id:
claim_id:
task_id:
title:
review_type: INDEPENDENT_REVIEW
builder:
reviewer:
domain_reviewer:
product_reviewer:
architecture_reviewer:
database_reviewer:
frontend_reviewer:
test_reviewer:
status: REVIEW_REQUESTED
created_at:
updated_at:
branch:
database_mode:
oracle_available:
local_db_verified:
oracle_smoke_status:
feature_acceptance_id:

## Scope

```text
Reviewed files:
Out of scope:
```

## Builder Self Check

```text
task_card_satisfied:
write_scope_matches_diff:
tests_updated:
samples_or_api_examples_updated:
docs_updated:
organization_context_checked:
source_traceability_checked:
audit_checked:
trace_id_checked:
db_only_checked:
oracle_dm_h2_schema_synced:
production_development_schema_synced:
table_and_column_comments_complete:
required_code_comments_complete:
feature_acceptance_created:
develop_health_status_before_pickup:        # GREEN / YELLOW / RED（来自 ai-dev-input/00_DEVELOP_HEALTH.md）
develop_health_status_after_commit:         # GREEN / YELLOW / RED
mvn_compile_local_passed:                   # YES / NO（必须 YES 才能进 review）
mvn_test_local_passed:                      # YES / NO / SKIPPED_REASON
```

## Verification Submitted By Builder

```text
run-tests:
build:
git diff --check:
local h2 smoke:
oracle ddl:
oracle smoke:
mvn_compile_evidence:        # 粘贴 mvn -q compile 末尾 5 行（含 BUILD SUCCESS）
mvn_test_evidence:           # 粘贴 mvn test 末尾 5 行（含 Tests run: X, Failures: 0, Errors: 0）
other:
```

## Review Checklist

```text
requirements:
architecture:
medical_safety_and_source:
database_consistency:
database_comments:
code_quality:
code_comments:
tests_and_verification:
security_and_privacy:
frontend_ux:
operations:
feature_quality:
```

## Findings

```text
finding_id:
severity: P0/P1/P2/P3
status: OPEN
file:
line:
title:
problem:
impact:
required_fix:
verification_required:
owner:
fixed_in:
reviewer_verdict:
```

## Open Findings Summary

```text
p0:
p1:
p2:
p3:
open_findings:
highest_severity:
```

## Final Verdict

```text
review_status:
approved_by:
approved_at:
submit_allowed: false
commit:
push:
risks:
feature_acceptance_status:
optimization_required:
follow_up_claims:
```
