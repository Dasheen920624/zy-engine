# Feature Acceptance

acceptance_id:
feature_id:
task_id:
claim_id:
review_id:
title:
owner:
status: PENDING
quality_level:
created_at:
updated_at:
commit:
push:

## Scope

```text
功能范围：
不验收范围：
关联接口：
关联页面：
关联表：
```

## Role Reviewers

```text
product_reviewer:
architecture_reviewer:
backend_reviewer:
frontend_reviewer:
database_reviewer:
test_reviewer:
medical_or_insurance_reviewer:
security_or_ops_reviewer:
```

## Acceptance Checklist

```text
business_story_complete:
target_role_can_complete_task:
api_contract_stable:
trace_id_and_audit_complete:
source_traceability_complete:
organization_scope_complete:
production_db_schema_synced:
development_db_local_h2_verified:
table_and_column_comments_complete:
required_code_comments_complete:
frontend_states_complete:
tests_and_smoke_complete:
security_privacy_checked:
docs_and_examples_updated:
optimization_task_registered_if_needed:
```

## Evidence

```text
run-tests:
build:
git diff --check:
local_h2:
production_db_smoke:
frontend_validation:
screenshots_or_reports:
claim_review_status:
git_status_after_push:
```

## Findings

```text
finding_id:
severity: P0/P1/P2/P3
owner:
status: OPEN
problem:
required_fix:
target_task:
optimization_owner:
```

## Verdict

```text
quality_level: GOLD / SILVER / BRONZE / REJECTED
approved_for_customer_demo: false
approved_for_integration: false
needs_optimization_task:
remaining_risk:
final_decision:
```
