# AI Task Claim

claim_id: GA-REL-01-S01
task_id: GA-REL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
slice: S01
title: 发布与分支保护证据
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-REL-01/release-protection
target_base_branch: develop
git_base_commit: dd4a94fb5cb49cccd6597da742669b55b585888a
git_status_at_claim: clean
created_at: 2026-05-23T22:00:00+08:00
last_heartbeat: 2026-05-23T22:00:00+08:00
expected_finish: 2026-05-24T04:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
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
write_scope: .github/workflows/**, scripts/**, VERSIONING.md
read_scope: docs/**, medkernel-mvp/**
forbidden_scope: frontend/**, medkernel-mvp/src/main/java/**, medkernel-mvp/src/test/java/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
```

## Write Scope

```text
.github/workflows/ci.yml
.github/workflows/release.yml
scripts/verify-pr.ps1
scripts/verify-task-prereq.ps1
VERSIONING.md
```

## Read Scope

```text
docs/**
medkernel-mvp/**
```

## Forbidden Scope

```text
frontend/**
medkernel-mvp/src/main/java/**
medkernel-mvp/src/test/java/**
medkernel-mvp/pom.xml
```

## Dependencies

```text
GA-GOV-01 (DONE)
```

## Acceptance

```text
1. main/develop 分支保护规则文档化
2. release evidence 工作流（tag + changelog 自动生成）
3. tag 流程可自动校验
4. VERSIONING.md 版本策略文档
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
pwsh scripts/verify-pr.ps1 -TaskId GA-REL-01
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
认领完成，开始开发
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
