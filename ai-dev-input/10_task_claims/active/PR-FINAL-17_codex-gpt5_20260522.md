# AI Task Claim

claim_id: PR-FINAL-17-CODEX-GPT5-20260522
task_id: PR-FINAL-17
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-17.lock
slice: repository-boundary-extraction
title: 拆 EnginePersistenceService：Repository 边界收口
owner: Codex-GPT5
role: 架构师 AI
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 151f1b0
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T13:03+08:00
last_heartbeat: 2026-05-22T13:03+08:00
expected_finish: 2026-05-22T20:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: pending
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-17-CODEX-GPT5-R01
review_status: PENDING
reviewer: Codex-GPT5
open_findings: pending
quality_gate: PENDING
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-17-S01
write_scope:
  - medkernel-mvp/src/main/java/com/medkernel/persistence/EnginePersistenceService.java
  - medkernel-mvp/src/main/java/com/medkernel/persistence/PersistenceRepositorySupport.java
  - medkernel-mvp/src/main/java/com/medkernel/audit/**
  - medkernel-mvp/src/main/java/com/medkernel/common/**
  - medkernel-mvp/src/main/java/com/medkernel/pathway/**
  - medkernel-mvp/src/main/java/com/medkernel/provenance/**
  - medkernel-mvp/src/main/java/com/medkernel/rule/**
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
  - medkernel-mvp/src/main/resources/db/local/**
forbidden_scope:
  - frontend/**
  - medkernel-mvp/db/**
  - ai-dev-input/04_database/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/PR-FINAL-17.lock
```

## Dependencies

```text
PR-FINAL-15a: DONE
PR-FINAL-15b: DONE
PR-FINAL-16: DONE
```

## Acceptance

```text
EnginePersistenceService remains the public compatibility facade.
Pathway instance, rule execution log, source provenance, audit log and ID allocation persistence move behind dedicated Repository classes.
ConfigPackageRepository remains the existing config repository boundary.
No DDL/schema changes.
No frontend changes.
Backend compile and focused persistence/API tests pass.
verify-pr passes with no FAIL.
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: LOCAL_CREATED
task_ledger_in_progress: DONE
git_status_checked_before_edit: DONE
last_heartbeat_pushed: PENDING_CLAIM_PUSH
review_status_synced: PENDING
task_ledger_done_synced: PENDING
commit_hash_recorded: PENDING
post_push_git_status_clean: PENDING
task_lock_removed_on_archive: PENDING
```

## Progress

```text
2026-05-22T13:03+08:00 ACTIVE - Created PR-FINAL-17 claim after prereq showed missing ledger registration and no active lock conflict.
```
