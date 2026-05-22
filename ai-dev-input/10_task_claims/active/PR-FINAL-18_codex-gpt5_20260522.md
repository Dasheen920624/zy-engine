# AI Task Claim

claim_id: PR-FINAL-18-CODEX-GPT5-20260522
task_id: PR-FINAL-18
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-18.lock
slice: service-boundary-extraction
title: 拆 RuleService / PathwayService / SecurityPersistence 等 5 个超长
owner: Codex-GPT5
role: 架构师 AI
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 6af2ddf6b6fbc680219a6b5319b35c44a3ac2931
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T13:37+08:00
last_heartbeat: 2026-05-22T13:37+08:00
expected_finish: 2026-05-22T21:30+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: pending
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-18-CODEX-GPT5-R01
review_status: PENDING
reviewer: Codex-GPT5
open_findings: pending
quality_gate: PENDING
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-18-S01
write_scope:
  - medkernel-mvp/src/main/java/com/medkernel/pathway/**
  - medkernel-mvp/src/main/java/com/medkernel/rule/**
  - medkernel-mvp/src/main/java/com/medkernel/knowledge/**
  - medkernel-mvp/src/main/java/com/medkernel/security/SecurityPersistenceService.java
  - medkernel-mvp/src/main/java/com/medkernel/security/*Repository.java
  - medkernel-mvp/src/main/java/com/medkernel/graph/**
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
  - medkernel-mvp/src/main/java/com/medkernel/quality/**
  - medkernel-mvp/src/main/java/com/medkernel/llm/**
  - medkernel-mvp/src/main/java/com/medkernel/cdss/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/PR-FINAL-18.lock
```

## Dependencies

```text
PR-FINAL-15a: DONE
PR-FINAL-15b: DONE
PR-FINAL-17: DONE
```

## Acceptance

```text
PathwayService, RuleService, KnowledgePackageService, SecurityPersistenceService and GraphService lose high-coupling private blocks behind dedicated bounded-context collaborators.
Existing Controller/Service public APIs remain compatible.
New files stay below 500 lines.
No DDL/schema changes.
No frontend changes.
Backend compile and full tests pass.
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
2026-05-22T13:37+08:00 ACTIVE - Created PR-FINAL-18 claim after prereq identified missing ledger registration and no same-task lock conflict.
```
