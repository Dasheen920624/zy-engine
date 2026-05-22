# AI Task Claim

claim_id: GA-QA-01-S01
task_id: GA-QA-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-01.lock
slice: S01
title: 后端覆盖率与 CI
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-QA-01/jacoco-coverage
target_base_branch: develop
git_base_commit: b121607767073f8bdd6f0be005f58a41d7d3920a
git_status_at_claim: clean
created_at: 2026-05-23T19:35:00+08:00
last_heartbeat: 2026-05-23T19:35:00+08:00
expected_finish: 2026-05-24T01:00:00+08:00
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
write_scope: medkernel-mvp/pom.xml, medkernel-mvp/src/test/**, .github/workflows/**
read_scope: docs/**, medkernel-mvp/src/main/**
forbidden_scope: frontend/**, ai-dev-input/10_task_claims/active/GA-GOV-01-S01.md

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/GA-QA-01.lock
```

## Write Scope

```text
medkernel-mvp/pom.xml
medkernel-mvp/src/test/**
.github/workflows/ci.yml
```

## Read Scope

```text
docs/**
medkernel-mvp/src/main/**
medkernel-mvp/src/test/**
```

## Forbidden Scope

```text
frontend/**
ai-dev-input/10_task_claims/active/GA-GOV-01-S01.md
```

## Dependencies

```text
无
```

## Acceptance

```text
1. Jacoco Maven 插件接入 pom.xml
2. mvn test 自动生成覆盖率报告
3. 后端覆盖率目标 70%（行覆盖率）
4. CI 工作流集成覆盖率检查
5. 覆盖率不足时构建失败
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
mvn -q -f medkernel-mvp/pom.xml test
mvn -f medkernel-mvp/pom.xml jacoco:report
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
