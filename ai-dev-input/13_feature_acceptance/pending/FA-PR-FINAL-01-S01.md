# Feature Acceptance: FA-PR-FINAL-01-S01

acceptance_id: FA-PR-FINAL-01-S01
feature_id: llm-gateway-package-cleanup
task_id: PR-FINAL-01
claim_id: PR-FINAL-01-S01
review_id: RV-PR-FINAL-01-S01-R01
title: LLM Gateway 迁包 dify/ → llm/
owner: Codex-GPT5
status: PENDING_PRODUCT_ACCEPTANCE
quality_level: GOLD
created_at: 2026-05-21T22:12:34+08:00
updated_at: 2026-05-21T22:12:34+08:00
commit: pending
push: pending

## Scope

```text
功能范围：
- 保持 LLM Gateway 主路径在 com.medkernel.llm。
- 将 Dify 可选 WORKFLOW Provider 收敛到 com.medkernel.dify.workflow。
- 将 AI Governance 从 Dify 根包移入 com.medkernel.quality。
- 修正 EnginePersistenceService / QualityService / HealthController 导入。
- 同步封板领单卡共享文件清单与架构命名记录。

不验收范围：
- 不改 REST URL：/api/dify/workflows、/api/model-gateway/*、/api/ai-governance/* 保持兼容。
- 不改 DDL、不改数据库表名、不改前端页面。

关联接口：
- /api/model-gateway/*
- /api/dify/workflows*
- /api/ai-governance/*

关联页面：
- N/A

关联表：
- N/A，本次无 schema 变更。
```

## Role Reviewers

```text
product_reviewer: N/A
architecture_reviewer: Codex-GPT5
backend_reviewer: Codex-GPT5
frontend_reviewer: N/A
database_reviewer: N/A
test_reviewer: Codex-GPT5
medical_or_insurance_reviewer: N/A
security_or_ops_reviewer: N/A
```

## Acceptance Checklist

```text
business_story_complete: yes
target_role_can_complete_task: yes
api_contract_stable: yes
trace_id_and_audit_complete: unchanged
source_traceability_complete: unchanged
organization_scope_complete: unchanged
production_db_schema_synced: N/A
development_db_local_h2_verified: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: yes
frontend_states_complete: N/A
tests_and_smoke_complete: yes
security_privacy_checked: yes
docs_and_examples_updated: yes
optimization_task_registered_if_needed: N/A
```

## Evidence

```text
mvn_compile:
  PASS — mvn -q -f medkernel-mvp/pom.xml compile

mvn_test:
  PASS — mvn -q -f medkernel-mvp/pom.xml test
  PASS — surefire reports 255 tests, 0 failures, 0 errors

package_guard:
  PASS — rg --pcre2 "com\\.medkernel\\.dify(?!\\.workflow)|package com\\.medkernel\\.dify;" found no source references

git diff --check:
  PASS — no whitespace errors

api_contract:
  PASS — EngineApiContractTests includes /api/dify/workflows contract and passed in full mvn test

claim_review_status:
  RV-PR-FINAL-01-S01-R01 APPROVED, open_findings=0

git_status_after_push:
  pending
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
remaining_risk: none for package boundary; URL and schema unchanged.
final_decision: Ready for PR review to develop.
```
