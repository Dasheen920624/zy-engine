# Feature Acceptance: FA-PR-FINAL-17-S01

acceptance_id: FA-PR-FINAL-17-S01
feature_id: engine-persistence-repository-split
task_id: PR-FINAL-17
claim_id: PR-FINAL-17-CODEX-GPT5-20260522
review_id: RV-PR-FINAL-17-CODEX-GPT5-R01
title: 拆 EnginePersistenceService：Repository 边界收口
owner: Codex-GPT5
status: PENDING_PRODUCT_ACCEPTANCE
quality_level: GOLD
created_at: 2026-05-22T13:22+08:00
updated_at: 2026-05-22T13:22+08:00
commit: ea8f5f9
push: origin/develop pending final status-sync push

## Scope

```text
验收范围：
- EnginePersistenceService 保留 public facade，调用方无需迁移。
- 新增 PathwayInstanceRepository，承接推荐卡、患者路径实例、节点状态、任务状态、变异记录持久化。
- 新增 RuleExecLogRepository，承接 re_rule_exec_log 写入。
- 新增 SourceDocumentRepository，承接 src_document、src_citation、src_asset_binding 持久化与重建查询。
- 新增 AuditLogRepository，承接 engine_audit_log 写入、内存查询和汇总。
- 新增 IdAllocatorRepository 和 PersistenceRepositorySupport，统一 ID 分配、DataSource connection、JSON、日期与通用转换。

不验收范围：
- 不改数据库 schema / DDL / migration。
- 不改前端页面、路由、菜单。
- 不抽 Dify template、pathway draft/version、rule definition、unmapped queue；这些仍留给后续架构拆分。
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
frontend_states_complete: N/A_NO_FRONTEND_CHANGE
tests_and_smoke_complete: yes
security_privacy_checked: yes
docs_and_examples_updated: yes
optimization_task_registered_if_needed: N/A
```

## Evidence

```text
mvn_compile:
  PASS — mvn -q -DskipTests compile

focused_test:
  PASS — mvn -q -Dtest=EngineApiContractTests test

run-tests:
  PASS — medkernel-mvp/scripts/run-tests.ps1
  PASS — surefire reports=14 tests=260 failures=0 errors=0 skipped=0

build:
  PASS — medkernel-mvp/scripts/build.ps1

git diff --check:
  PASS

verify-pr:
  PASS — .\scripts\verify-pr.ps1 -TaskId PR-FINAL-17; 16 PASS / 0 FAIL / 2 WARN

local_h2:
  PASS — SpringBootTest active profile uses LOCAL_H2_FILE

production_db_smoke:
  N/A — no schema or migration change

claim_review_status:
  RV-PR-FINAL-17-CODEX-GPT5-R01 APPROVED, open_findings=0
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
remaining_risk: Remaining non-requested EnginePersistenceService domains should be extracted in a future architecture task, but this acceptance closes the six repository boundaries requested by PR-FINAL-17.
final_decision: Ready for develop after final verify-pr, commit and push.
```
