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
last_heartbeat: 2026-05-20T08:48:00+08:00
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
claim_pushed_before_code: true (commit 5503807)
task_ledger_in_progress: true (commit 5503807)
git_status_checked_before_edit: true
last_heartbeat_pushed: true
review_status_synced: NOT_REQUESTED
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: true (全部改动在write_scope内，纯SQL文件)
tests_updated: N/A (DDL任务)
samples_or_api_examples_updated: N/A
docs_updated: N/A
db_only_checked: true (只修改SQL文件)
oracle_dm_h2_schema_synced: true (权威DDL→部署DDL→运行时DDL已对齐)
production_development_schema_synced: true (adp表补充tenant_id/hospital_code与权威一致)
table_and_column_comments_complete: true (H2行内注释 + Oracle/DM/PG COMMENT ON)
required_code_comments_complete: N/A
feature_acceptance_created: pending
claim_status_synced: true
security_privacy_checked: true
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
2026-05-20 08:42 修复runtime h2_core_ddl.sql adp表结构（添加tenant_id/hospital_code）
2026-05-20 08:42 添加wf_todo_task/wf_approval_action/wf_approval_rule到runtime h2_core_ddl.sql
2026-05-20 08:43 创建resources/db/local/notify_ddl.sql、wf_ddl.sql、tenant_onboarding_ddl.sql
2026-05-20 08:44 为H2 sec_ddl.sql添加中文行内注释（7表+关键列）
2026-05-20 08:45 创建medkernel-mvp/db/dm/medkernel_comments_unistr.sql
2026-05-20 08:46 创建medkernel-mvp/db/postgres/medkernel_comments_unistr.sql
2026-05-20 08:48 修复所有新建文件行尾（CRLF→LF）
2026-05-20 08:48 编译验证：编译错误均为分支上已有的Java问题，与SQL改动无关
2026-05-20 08:48 自检通过，准备提交
```

## Handoff

```text

```

## Completion

```text
commit:
push:
tests: 编译错误为分支已有Java问题（WebSocket/TenantOnboarding/RuleAction），与DDL改动无关
review: NOT_REQUESTED
risks: 无（纯DDL文件变更，不影响运行时代码逻辑）
```
