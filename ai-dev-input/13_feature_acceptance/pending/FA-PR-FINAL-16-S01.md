# Feature Acceptance: FA-PR-FINAL-16-S01

acceptance_id: FA-PR-FINAL-16-S01
feature_id: jackson-snake-case-contract
task_id: PR-FINAL-16
claim_id: PR-FINAL-16-CODEX-GPT5-20260522
review_id: RV-PR-FINAL-16-CODEX-GPT5-R01
title: Jackson SNAKE_CASE 全局 + API 契约收口
owner: Codex-GPT5
status: PENDING_PRODUCT_ACCEPTANCE
quality_level: GOLD
created_at: 2026-05-22T12:55+08:00
updated_at: 2026-05-22T13:00+08:00
commit: PENDING_FINAL_COMMIT
push: PENDING_FINAL_PUSH

## Scope

```text
功能范围：
- 启用 spring.jackson.property-naming-strategy: SNAKE_CASE。
- 后端 MockMvc 契约测试断言统一到 snake_case。
- UserSync / SSO / MPI 的 raw Map 请求体热点统一读取 snake_case。
- MPI 前端 API 类型与页面消费字段统一为 snake_case。
- 新增 MPI MockMvc 契约测试，锁住 snake_case 请求、查询参数和响应字段。

不验收范围：
- 不改数据库 schema / DDL / 多方言脚本。
- 不改非 MPI 的历史 raw Map Controller 全量 DTO 化。
- 不改前端路由、菜单、全局样式。

关联接口：
- /api/v1/mpi/*
- /api/user-sync/*
- /api/sso/*
- EngineApiContractTests 覆盖的核心后端接口

关联页面：
- /mpi/patients

关联表：
- N/A，本次无 schema 变更。
```

## Role Reviewers

```text
product_reviewer: N/A
architecture_reviewer: Codex-GPT5
backend_reviewer: Codex-GPT5
frontend_reviewer: Codex-GPT5
database_reviewer: N/A_NO_SCHEMA_CHANGE
test_reviewer: Codex-GPT5
medical_or_insurance_reviewer: N/A_NO_MEDICAL_LOGIC_CHANGE
security_or_ops_reviewer: Codex-GPT5
```

## Acceptance Checklist

```text
business_story_complete: yes
target_role_can_complete_task: yes
api_contract_stable: yes
trace_id_and_audit_complete: unchanged
source_traceability_complete: unchanged
organization_scope_complete: unchanged
production_db_schema_synced: N/A_NO_SCHEMA_CHANGE
development_db_local_h2_verified: yes
table_and_column_comments_complete: N/A_NO_SCHEMA_CHANGE
required_code_comments_complete: yes
frontend_states_complete: unchanged
tests_and_smoke_complete: yes
security_privacy_checked: yes
docs_and_examples_updated: yes
optimization_task_registered_if_needed: N/A
```

## Evidence

```text
run-tests:
  PASS — medkernel-mvp/scripts/run-tests.ps1
  PASS — surefire reports=14 tests=260 failures=0 errors=0 skipped=0

build:
  PASS — medkernel-mvp/scripts/build.ps1
  PASS — npm run build

frontend:
  PASS — npm run typecheck
  PASS — npm test; 39 files / 176 tests
  PASS — npm test -- src/pages/Mpi; 4 files / 8 tests

git diff --check:
  PASS

verify-pr:
  PASS — .\scripts\verify-pr.ps1 -TaskId PR-FINAL-16; 18 PASS / 0 FAIL / 2 WARN

local_h2:
  PASS — SpringBootTest active profile test uses LOCAL_H2_FILE

production_db_smoke:
  N/A — no schema or migration change

frontend_validation:
  PASS — MPI API/page TypeScript contract compiles with snake_case fields

screenshots_or_reports:
  N/A — contract/backend task; no visual layout change

claim_review_status:
  RV-PR-FINAL-16-CODEX-GPT5-R01 APPROVED, open_findings=0

git_status_after_push:
  PENDING_FINAL_PUSH
```

## Findings

```text
finding_id: none
severity: none
owner: none
status: CLOSED
problem: none
required_fix: none
target_task: none
optimization_owner: none
```

## Verdict

```text
quality_level: GOLD
approved_for_customer_demo: true
approved_for_integration: true
needs_optimization_task: false
remaining_risk: Non-MPI legacy raw Map endpoints still need future DTO cleanup, but this PR closes the configured Jackson/API contract surface and covered pages.
final_decision: Ready for develop after verify-pr, commit and push.
```
