# AI Task Claim

claim_id: GA-GOV-01-S01
task_id: GA-GOV-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-GOV-01.lock
slice: S01
title: 并发机制硬门禁
owner: TraeAI-Main
role: senior
status: ACTIVE
branch: ai/GA-GOV-01/collaboration-guard
target_base_branch: develop
git_base_commit: 7a744abea894438856699a94f8a35bf7c0dce396
git_status_at_claim: clean
created_at: 2026-05-23T19:00:00+08:00
last_heartbeat: 2026-05-23T19:00:00+08:00
expected_finish: 2026-05-23T22:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope: scripts/**, medkernel-mvp/scripts/check-ai-collaboration.ps1, ai-dev-input/10_task_claims/**, .github/workflows/ci.yml, docs/engineering/02_任务台账.md
read_scope: docs/**, ai-dev-input/**
forbidden_scope: medkernel-mvp/src/main/java/**, frontend/src/**

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/GA-GOV-01.lock
```

锁文件内容模板：

```text
task_id: GA-GOV-01
claim_id: GA-GOV-01-S01
owner: TraeAI-Main
branch: ai/GA-GOV-01/collaboration-guard
git_base_commit: 7a744abea894438856699a94f8a35bf7c0dce396
created_at: 2026-05-23T19:00:00+08:00
last_heartbeat: 2026-05-23T19:00:00+08:00
```

## Write Scope

```text
scripts/verify-task-prereq.ps1
scripts/verify-pr.ps1
medkernel-mvp/scripts/check-ai-collaboration.ps1
ai-dev-input/10_task_claims/active/GA-GOV-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-GOV-01.lock
.github/workflows/ci.yml
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
ai-dev-input/**
medkernel-mvp/scripts/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
```

## Dependencies

```text
无
```

## Acceptance

```text
1. CI 新增 ai-collaboration-guard job，push/PR 自动跑 check-ai-collaboration.ps1 -Strict
2. orphan lock 自动阻断（CI FAIL）
3. 重复 task_id 自动阻断（CI FAIL）
4. write_scope 重叠自动阻断（CI FAIL）
5. verify-task-prereq.ps1 支持 GA-* 任务编号模式
6. check-ai-collaboration.ps1 支持 GA-* 任务编号的依赖检查
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: pending
git_status_checked_before_edit: true
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
.\scripts\verify-task-prereq.ps1 -TaskId GA-GOV-01 -Level senior
.\medkernel-mvp\scripts\check-ai-collaboration.ps1 -Strict
.\scripts\verify-pr.ps1 -TaskId GA-GOV-01 -SkipFrontend -SkipBackend
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: N/A
samples_or_api_examples_updated: N/A
docs_updated: pending
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: pending
feature_acceptance_created: N/A
claim_status_synced: pending
security_privacy_checked: N/A
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
- 创建 claim 和 lock 文件
- 增强 CI 工作流
- 更新 verify-task-prereq.ps1 支持 GA-* 编号
- 增强 check-ai-collaboration.ps1
- 更新任务台账
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
