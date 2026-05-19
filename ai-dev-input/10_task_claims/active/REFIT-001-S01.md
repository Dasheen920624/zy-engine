# AI Task Claim

claim_id: REFIT-001-S01
task_id: REFIT-001
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-001.lock
slice: S01
title: 已实现能力全量盘点与一致性基线
owner: TraeAI-Main
role: 高级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: cb34a771f10b099e573051c0d0fba926712fceb7
git_status_at_claim: clean
created_at: 2026-05-19T20:00:00+08:00
last_heartbeat: 2026-05-19T20:00:00+08:00
expected_finish: 2026-05-20T08:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-REFIT-001-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope: docs/**, ai-dev-input/13_feature_acceptance/**, medkernel-mvp/scripts/**
read_scope: docs/engineering/**, medkernel-mvp/src/**, frontend/src/**, ai-dev-input/**
forbidden_scope: medkernel-mvp/src/main/java/**, frontend/src/**（只读不写业务代码）

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/REFIT-001.lock
```

锁文件内容模板：

```text
task_id: REFIT-001
claim_id: REFIT-001-S01
owner: TraeAI-Main
branch: develop
git_base_commit: cb34a771f10b099e573051c0d0fba926712fceb7
created_at: 2026-05-19T20:00:00+08:00
last_heartbeat: 2026-05-19T20:00:00+08:00
```

## Write Scope

```text
docs/**
ai-dev-input/13_feature_acceptance/**
medkernel-mvp/scripts/**
```

## Read Scope

```text
docs/engineering/**
medkernel-mvp/src/**
frontend/src/**
ai-dev-input/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**（业务代码只读不写）
frontend/src/**（前端代码只读不写）
```

## Dependencies

```text
DOC-012（已完成）
```

## Acceptance

```text
- 已实现能力矩阵（API/页面/表/测试清单）
- P0/P1/P2 改造 finding 列表
- 验收基线文档
- 功能验收记录（ai-dev-input/13_feature_acceptance/）
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
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
.\medkernel-mvp\scripts\check-ai-collaboration.ps1
.\medkernel-mvp\scripts\run-tests.ps1
.\medkernel-mvp\scripts\build.ps1
git diff --check
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: pending
samples_or_api_examples_updated: pending
docs_updated: pending
db_only_checked: pending
oracle_dm_h2_schema_synced: pending
production_development_schema_synced: pending
table_and_column_comments_complete: pending
required_code_comments_complete: pending
feature_acceptance_created: pending
claim_status_synced: pending
security_privacy_checked: pending
```

## Quality Review

```text
review_id: RV-REFIT-001-S01-R01
review_file: pending
review_status: NOT_REQUESTED
highest_severity:
open_findings: 0
changes_requested: 0
approved_by:
approved_at:
submit_allowed: false
```

## Progress

```text
认领任务，准备开始盘点
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
