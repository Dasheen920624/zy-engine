# AI Task Claim

claim_id: REFIT-003-S01
task_id: REFIT-003
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-003.lock
slice: S01
title: 来源/审计/traceId/发布门禁统一改造
owner: CodeBuddy
role: 高级
status: ACTIVE
branch: ai/REFIT-003/provenance-audit-trace
target_base_branch: develop
git_base_commit: b37ffd9
git_status_at_claim: clean
created_at: 2026-05-20T09:38:00+08:00
last_heartbeat: 2026-05-20T09:38:00+08:00
expected_finish: 2026-05-20T18:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required:
feature_acceptance_id:
write_scope: src/main/java/com/medkernel/config/**, src/main/java/com/medkernel/rule/**, src/main/java/com/medkernel/pathway/**, src/main/java/com/medkernel/graph/**, src/main/java/com/medkernel/dify/**, src/main/java/com/medkernel/audit/**
read_scope: docs/**, ai-dev-input/**, src/main/java/com/medkernel/provenance/**
forbidden_scope: 其他任务独占文件

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/REFIT-003.lock
```

## Write Scope

```text
src/main/java/com/medkernel/config/**（配置模块）
src/main/java/com/medkernel/rule/**（规则引擎）
src/main/java/com/medkernel/pathway/**（路径引擎）
src/main/java/com/medkernel/graph/**（图谱引擎）
src/main/java/com/medkernel/dify/**（Dify适配）
src/main/java/com/medkernel/audit/**（审计模块）
```

## Read Scope

```text
docs/engineering/**（开发规范）
ai-dev-input/**（任务卡、API契约）
src/main/java/com/medkernel/provenance/**（来源模块，已实现）
src/main/java/com/medkernel/security/**（安全模块，依赖）
```

## Forbidden Scope

```text
其他任务独占文件（frontend/**、deploy/**等）
```

## Dependencies

REFIT-001 (DONE), PROV-003 (DONE), AUDIT-001 (DONE)

## Acceptance

1. 医学/医保/质控资产发布前统一来源检查（来源完整性、有效性）
2. 运行结果和高风险操作可查证据、审计和 traceId
3. 发布门禁：缺来源/过期/未审核时阻断
4. traceId 全链路透传（规则、路径、图谱、Dify）
5. 审计日志包含来源摘要和 traceId

## Verification

```powershell
cd medkernel-mvp && mvn test '-Dspring.profiles.active=memory'
```

## Status Sync Checkpoints

```text
claim_pushed_before_code:
task_ledger_in_progress:
git_status_checked_before_edit: true
last_heartbeat_pushed:
review_status_synced:
task_ledger_done_synced:
commit_hash_recorded:
post_push_git_status_clean:
task_lock_removed_on_archive:
```

## Self Check

```text
task_card_satisfied:
write_scope_matches_diff:
tests_updated:
samples_or_api_examples_updated:
docs_updated:
db_only_checked:
oracle_dm_h2_schema_synced:
production_development_schema_synced:
table_and_column_comments_complete:
required_code_comments_complete:
feature_acceptance_created:
claim_status_synced:
security_privacy_checked:
```

## Quality Review

```text
review_id:
review_file:
review_status:
highest_severity:
open_findings:
changes_requested:
approved_by:
approved_at:
submit_allowed:
```

## Progress

```text
2026-05-20 09:38 认领REFIT-003任务，创建claim和lock
```

## Handoff

```text

```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
