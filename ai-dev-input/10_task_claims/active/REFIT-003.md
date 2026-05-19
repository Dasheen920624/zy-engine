# AI Task Claim

claim_id: REFIT-003-20260520
task_id: REFIT-003
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-003.lock
slice: 横切
title: 来源/审计/traceId/发布门禁统一改造
owner: CodeBuddy
role: 全栈开发
status: ACTIVE
branch: ai/REFIT-003/source-audit-trace
target_base_branch: develop
git_base_commit: b37ffd904dc1c56ff84aab9e9430f5b239498fd7
git_status_at_claim: clean
created_at: 2026-05-20T07:35:00+08:00
last_heartbeat: 2026-05-20T07:35:00+08:00
expected_finish: 2026-05-21T19:35:00+08:00
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
feature_acceptance_required: true
feature_acceptance_id: 
write_scope: 
read_scope: 
forbidden_scope: 

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/REFIT-003.lock
```

锁文件内容模板：

```text
task_id: REFIT-003
claim_id: REFIT-003-20260520
owner: CodeBuddy
branch: ai/REFIT-003/source-audit-trace
git_base_commit: b37ffd904dc1c56ff84aab9e9430f5b239498fd7
created_at: 2026-05-20T07:35:00+08:00
last_heartbeat: 2026-05-20T07:35:00+08:00
```

## Write Scope

```text
src/main/java/com/medkernel/config/**
src/main/java/com/medkernel/rule/**
src/main/java/com/medkernel/pathway/**
src/main/java/com/medkernel/graph/**
src/main/java/com/medkernel/dify/**
src/main/java/com/medkernel/audit/**
src/test/java/com/medkernel/config/**
src/test/java/com/medkernel/rule/**
src/test/java/com/medkernel/pathway/**
src/test/java/com/medkernel/graph/**
src/test/java/com/medkernel/dify/**
src/test/java/com/medkernel/audit/**
```

## Read Scope

```text
src/main/java/com/medkernel/provenance/**
src/main/java/com/medkernel/common/**
docs/engineering/06_后端开发规范.md
docs/engineering/00_总入口与AI接手导航.md
```

## Forbidden Scope

```text
frontend/**
ai-dev-input/04_database/**
deploy/**
```

## Dependencies

```text
REFIT-001 已实现能力全量盘点与一致性基线（已完成）
PROV-003 SRC_ASSET_BINDING 资产绑定（已完成）
AUDIT-001 统一审计事件（已完成）
```

## Acceptance

```text
1. 医学/医保/质控资产发布前统一来源检查
2. 运行结果和高风险操作可查证据、审计和 traceId
3. 缺来源、来源过期、未审核不得发布
4. 所有发布/回滚/同步操作必须写入 ENGINE_AUDIT_LOG
5. 所有关键操作必须携带 traceId
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: false
task_ledger_in_progress: false
git_status_checked_before_edit: true
last_heartbeat_pushed: false
review_status_synced: false
task_ledger_done_synced: false
commit_hash_recorded: false
post_push_git_status_clean: false
task_lock_removed_on_archive: false
```

## Verification

```text

```

## Self Check

```text
task_card_satisfied: false
write_scope_matches_diff: false
tests_updated: false
samples_or_api_examples_updated: false
docs_updated: false
db_only_checked: true
oracle_dm_h2_schema_synced: true
production_development_schema_synced: true
table_and_column_comments_complete: true
required_code_comments_complete: false
feature_acceptance_created: false
claim_status_synced: false
security_privacy_checked: true
```

## Quality Review

```text
review_id: 
review_file: 
review_status: NOT_REQUESTED
highest_severity: 
open_findings: 
changes_requested: 
approved_by: 
approved_at: 
submit_allowed: false
```

## Progress

```text
2026-05-20 07:35:00 - 开始任务认领
2026-05-20 07:35:00 - 选择REFIT-003任务
2026-05-20 07:35:00 - 创建REFIT-003认领文件
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