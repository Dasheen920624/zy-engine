# AI Task Claim

claim_id: PR-FINAL-16-CODEX-GPT5-20260522
task_id: PR-FINAL-16
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-16.lock
slice: backend-api-contract
title: Jackson SNAKE_CASE 全局 + API 契约收口
owner: Codex-GPT5
role: 架构师 AI
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: b1506cbebe0b9f5e8dca6d5b256ac3fb9ab7d34d
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T12:26+08:00
last_heartbeat: 2026-05-22T12:26+08:00
expected_finish: 2026-05-22T20:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: pending
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-16-CODEX-GPT5-R01
review_status: NOT_REQUESTED
reviewer: pending
open_findings: pending
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-16-S01
write_scope:
  - medkernel-mvp/src/main/resources/application.yml
  - medkernel-mvp/src/main/java/com/medkernel/**
  - medkernel-mvp/src/test/java/com/medkernel/**
  - docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md
  - docs/engineering/02_任务台账.md
  - ai-dev-input/10_task_claims/**
  - ai-dev-input/11_ai_reviews/**
  - ai-dev-input/13_feature_acceptance/**
read_scope:
  - docs/AI_CHARTER.md
  - docs/AI_TEAM_SOP.md
  - docs/PRODUCT_ARCHITECTURE_FINAL.md
  - docs/engineering/00_总入口与AI接手导航.md
  - docs/engineering/06_后端开发规范.md
  - frontend/src/api/**
forbidden_scope:
  - frontend/src/router/**
  - frontend/src/styles/**
  - medkernel-mvp/db/**
  - ai-dev-input/04_database/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/PR-FINAL-16.lock
```

## Write Scope

```text
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/**
medkernel-mvp/src/test/java/com/medkernel/**
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/**
ai-dev-input/11_ai_reviews/**
ai-dev-input/13_feature_acceptance/**
```

## Read Scope

```text
docs/AI_CHARTER.md
docs/AI_TEAM_SOP.md
docs/PRODUCT_ARCHITECTURE_FINAL.md
docs/engineering/00_总入口与AI接手导航.md
docs/engineering/06_后端开发规范.md
frontend/src/api/**
```

## Forbidden Scope

```text
frontend/src/router/**
frontend/src/styles/**
medkernel-mvp/db/**
ai-dev-input/04_database/**
```

## Dependencies

```text
PR-FINAL-15a: DONE
PR-FINAL-15b: DONE
```

## Acceptance

```text
spring.jackson.property-naming-strategy enabled as SNAKE_CASE.
MockMvc/API contract tests aligned with snake_case JSON fields.
No new raw Map controller request bodies.
No schema or DDL changes.
Backend build and tests pass.
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: LOCAL_CREATED
task_ledger_in_progress: DONE
git_status_checked_before_edit: DONE
last_heartbeat_pushed: PENDING
review_status_synced: PENDING
task_ledger_done_synced: PENDING
commit_hash_recorded: PENDING
post_push_git_status_clean: PENDING
task_lock_removed_on_archive: PENDING
```

## Verification

```text
verify-task-prereq: initial FAIL because PR-FINAL-16 was missing from task ledger; ledger row added before business code edits.
```

## Self Check

```text
task_card_satisfied: PENDING
write_scope_matches_diff: PENDING
tests_updated: PENDING
samples_or_api_examples_updated: PENDING
docs_updated: PENDING
db_only_checked: PENDING
oracle_dm_h2_schema_synced: N/A_NO_SCHEMA_CHANGE
production_development_schema_synced: N/A_NO_SCHEMA_CHANGE
table_and_column_comments_complete: N/A_NO_SCHEMA_CHANGE
required_code_comments_complete: PENDING
feature_acceptance_created: PENDING
claim_status_synced: ACTIVE
security_privacy_checked: PENDING
```

## Quality Review

```text
review_id: RV-PR-FINAL-16-CODEX-GPT5-R01
review_file: ai-dev-input/11_ai_reviews/pending/RV-PR-FINAL-16-CODEX-GPT5-R01.md
review_status: NOT_REQUESTED
highest_severity: PENDING
open_findings: PENDING
changes_requested: PENDING
approved_by: PENDING
approved_at: PENDING
submit_allowed: false
```

## Progress

```text
2026-05-22T12:26+08:00 ACTIVE - Created claim and task lock after prereq identified missing ledger registration.
```

## Handoff

```text
N/A
```

## Completion

```text
commit: PENDING
push: PENDING
tests: PENDING
review: PENDING
risks: PENDING
```
