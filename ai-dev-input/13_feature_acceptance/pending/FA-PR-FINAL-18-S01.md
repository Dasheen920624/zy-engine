# Feature Acceptance: FA-PR-FINAL-18-S01

acceptance_id: FA-PR-FINAL-18-S01
feature_id: service-boundary-extraction-five-domains
task_id: PR-FINAL-18
claim_id: PR-FINAL-18-CODEX-GPT5-20260522
review_id: RV-PR-FINAL-18-CODEX-GPT5-R01
title: 拆 RuleService / PathwayService / SecurityPersistence 等 5 个超长
owner: Codex-GPT5
status: PENDING_PRODUCT_ACCEPTANCE
quality_level: GOLD
created_at: 2026-05-22T14:12+08:00
updated_at: 2026-05-22T14:12+08:00
commit: a40207b
push: origin/develop pending status-sync push

## Scope

```text
验收范围：
- SecurityPersistenceService 变为兼容门面，SEC 用户、角色、身份源/绑定、审计访问拆入专门 Repository。
- RuleService 的执行日志内存环、查询、聚合移入 RuleExecutionLogService。
- KnowledgePackageService 的 knowledge_package raw JDBC 移入 KnowledgePackageRepository。
- GraphService 的 Neo4j 查询/降级召回移入 GraphQueryService，版本导入/激活/回滚移入 GraphVersionService。
- PathwayService 的路径模板版本 diff 移入 PathwayTemplateService。
- 所有 Controller/Service public API 保持兼容；无 schema、DDL、前端变更。

不验收范围：
- 不做数据库迁移。
- 不改前端页面、菜单、路由。
- 不进入 AI-GOV-002 的 quality/llm/cdss 任务范围。
- 不承诺一次性把所有存量超长编排类降到 500 行以下；本次验收第一批清晰边界拆分，并保证新增文件全部小于 500 行。
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
  PASS — mvn -DskipTests compile

run-tests:
  PASS — medkernel-mvp/scripts/run-tests.ps1
  PASS — surefire reports=14 tests=260 failures=0 errors=0 skipped=0

build:
  PASS — medkernel-mvp/scripts/build.ps1
  PASS — target/medkernel-mvp-0.1.0-SNAPSHOT.jar

git diff --check:
  PASS

collaboration_check:
  PASS — medkernel-mvp/scripts/check-ai-collaboration.ps1; no write-scope overlap with AI-GOV-002

new_file_line_guard:
  PASS — largest new Java file GraphQueryService.java is 466 lines

verify-pr:
  PASS — .\scripts\verify-pr.ps1 -TaskId PR-FINAL-18; 16 PASS / 0 FAIL / 2 WARN

local_h2:
  PASS — SpringBootTest active profile uses LOCAL_H2_FILE

production_db_smoke:
  N/A — no schema or migration change

claim_review_status:
  RV-PR-FINAL-18-CODEX-GPT5-R01 APPROVED, open_findings=0
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
remaining_risk: The remaining large RuleService/PathwayService/KnowledgePackageService orchestration methods are deliberately left compatible for future incremental slices; this task closes the first five-domain boundary extraction requested by PR-FINAL-18.
final_decision: Ready for develop after status-sync commit and push.
```
