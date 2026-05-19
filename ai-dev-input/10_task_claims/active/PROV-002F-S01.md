# AI Task Claim

claim_id: PROV-002F-S01
task_id: PROV-002F
task_lock_path: ai-dev-input/10_task_claims/active_locks/PROV-002F.lock
slice: S01
title: SRC_CITATION 持久化接通（REVIEW-FIX-002 follow-up）
owner: CodeBuddy
role: 中级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: a68cf8f4c8df6bdfb8469064a08156520450a831
git_status_at_claim: clean
created_at: 2026-05-19T21:30:00+08:00
last_heartbeat: 2026-05-19T21:30:00+08:00
expected_finish: 2026-05-20T08:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: N/A
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: false
review_id:
review_status:
reviewer:
open_findings: 0
quality_gate:
feature_acceptance_required: false
feature_acceptance_id:
write_scope: provenance/**, persistence/**, docs/**
read_scope: docs/engineering/02_任务台账.md, docs/03_设计系统.md
forbidden_scope: 除 write_scope 外的所有业务文件

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/PROV-002F.lock
```

## Write Scope

```text
provenance/**
persistence/**
docs/**
```

## Dependencies

```text
PROV-002 ✅ DONE
```

## Acceptance

```text
- 分析 PROV-002 当前内存态实现
- 检查 DDL 字段名错位问题
- 实现字段映射
- 实现加载/写入路径
- 实现 @PostConstruct 重建
- 更新文档
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
task_ledger_in_progress: pending
git_status_checked_before_edit: true
last_heartbeat_pushed: pending
review_status_synced: N/A
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Progress

```text
认领任务，准备开始开发
```