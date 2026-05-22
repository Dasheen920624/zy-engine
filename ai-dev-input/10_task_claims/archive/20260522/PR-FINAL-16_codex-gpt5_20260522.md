# AI Task Claim

claim_id: PR-FINAL-16-CODEX-GPT5-20260522
task_id: PR-FINAL-16
task_lock_path: ai-dev-input/10_task_claims/archive/20260522/PR-FINAL-16.lock.removed
slice: backend-api-contract
title: Jackson SNAKE_CASE 全局 + API 契约收口
owner: Codex-GPT5
role: 架构师 AI
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: b1506cbebe0b9f5e8dca6d5b256ac3fb9ab7d34d
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T12:26+08:00
last_heartbeat: 2026-05-22T13:00+08:00
expected_finish: 2026-05-22T20:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: yes
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-16-CODEX-GPT5-R01
review_status: APPROVED
reviewer: Codex-GPT5
open_findings: 0
quality_gate: PASSED
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-16-S01
write_scope:
  - medkernel-mvp/src/main/resources/application.yml
  - medkernel-mvp/src/main/java/com/medkernel/**
  - medkernel-mvp/src/test/java/com/medkernel/**
  - frontend/src/api/mpi.ts
  - frontend/src/pages/Mpi/**
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
ai-dev-input/10_task_claims/active_locks/PR-FINAL-16.lock removed on archive
```

## Write Scope

```text
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/**
medkernel-mvp/src/test/java/com/medkernel/**
frontend/src/api/mpi.ts
frontend/src/pages/Mpi/**
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
MPI frontend API/page contract aligned with snake_case backend JSON fields.
No new raw Map controller request bodies.
No schema or DDL changes.
Backend build and tests pass.
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: LOCAL_CREATED
task_ledger_in_progress: DONE
git_status_checked_before_edit: DONE
last_heartbeat_pushed: PENDING_FINAL_PUSH
review_status_synced: DONE
task_ledger_done_synced: DONE
commit_hash_recorded: PENDING
post_push_git_status_clean: PENDING
task_lock_removed_on_archive: DONE
```

## Verification

```text
verify-task-prereq: initial FAIL because PR-FINAL-16 was missing from task ledger; ledger row added before business code edits.
run-tests: PASS — medkernel-mvp/scripts/run-tests.ps1; surefire reports=14 tests=260 failures=0 errors=0 skipped=0
build: PASS — medkernel-mvp/scripts/build.ps1
frontend: PASS — npm run typecheck; npm test 39 files / 176 tests; npm run build
verify-pr: PASS — .\scripts\verify-pr.ps1 -TaskId PR-FINAL-16; 18 PASS / 0 FAIL / 2 WARN
git diff --check: PASS
encoding: PASS
```

## Self Check

```text
task_card_satisfied: yes
write_scope_matches_diff: yes
tests_updated: yes
samples_or_api_examples_updated: N/A
docs_updated: yes
db_only_checked: yes
oracle_dm_h2_schema_synced: N/A_NO_SCHEMA_CHANGE
production_development_schema_synced: N/A_NO_SCHEMA_CHANGE
table_and_column_comments_complete: N/A_NO_SCHEMA_CHANGE
required_code_comments_complete: yes
feature_acceptance_created: yes
claim_status_synced: DONE
security_privacy_checked: yes
```

## Quality Review

```text
review_id: RV-PR-FINAL-16-CODEX-GPT5-R01
review_file: ai-dev-input/11_ai_reviews/approved/RV-PR-FINAL-16-CODEX-GPT5-R01.md
review_status: APPROVED
highest_severity: none
open_findings: 0
changes_requested: no
approved_by: Codex-GPT5
approved_at: 2026-05-22T12:55+08:00
submit_allowed: true
```

## Progress

```text
2026-05-22T12:26+08:00 ACTIVE - Created claim and task lock after prereq identified missing ledger registration.
2026-05-22T12:40+08:00 ACTIVE - Expanded write scope to MPI frontend API/page contract after SNAKE_CASE impact review found POJO responses now serialize as snake_case.
2026-05-22T13:00+08:00 DONE - Jackson SNAKE_CASE enabled; API contract tests, MPI frontend contract and verify-pr passed.
```

## Handoff

```text
N/A
```

## Completion

```text
commit: PENDING_FINAL_COMMIT
push: PENDING_FINAL_PUSH
tests: backend 260 PASS; frontend 176 PASS; verify-pr 18 PASS / 0 FAIL / 2 WARN
review: RV-PR-FINAL-16-CODEX-GPT5-R01 APPROVED, open_findings=0
risks: Non-MPI legacy raw Map endpoints still need future DTO cleanup; no schema changes in this task.
```
