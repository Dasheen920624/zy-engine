# AI Task Claim

claim_id: REFIT-004-S01
task_id: REFIT-004
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-004.lock
slice: S01
title: 多数据库持久化和中文注释统一补齐
owner: CodeBuddy
role: 高级
status: ACTIVE
branch: ai/REFIT-004/multi-db-ddl-unify
target_base_branch: develop
git_base_commit: b37ffd9
git_status_at_claim: clean
created_at: 2026-05-20T08:40:00+08:00
last_heartbeat: 2026-05-20T08:40:00+08:00
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
write_scope: ai-dev-input/04_database/**, medkernel-mvp/db/**, medkernel-mvp/src/main/resources/db/local/**
read_scope: docs/**, ai-dev-input/**
forbidden_scope: 其他任务独占文件

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/REFIT-004.lock
```

## Write Scope

```text
ai-dev-input/04_database/**（权威DDL源）
medkernel-mvp/db/**（部署DDL：oracle/dm/postgres）
medkernel-mvp/src/main/resources/db/local/**（开发库DDL）
```

## Read Scope

```text
docs/engineering/**（开发规范、数据库规范）
ai-dev-input/**（任务卡、DDL源）
medkernel-mvp/src/**（Java代码确认表使用）
```

## Forbidden Scope

```text
其他任务独占文件（src/main/java/com/medkernel/**中非DDL相关、frontend/**等）
```

## Dependencies

REFIT-001 (DONE), ARCH-004 (DONE)

## Acceptance

1. 已实现能力的 Oracle/DM/PG/LOCAL_H2_FILE 表结构一致
2. 所有表和列都有中文注释
3. 索引、约束一致
4. 开发库DDL可在H2上正常执行
5. smoke计划完整

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
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
2026-05-20 08:40 认领REFIT-004任务，创建claim和lock
2026-05-20 08:40 盘点三处DDL结构，识别GAP清单
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
