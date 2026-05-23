# AI Task Claim

claim_id: PR-FINAL-17-CODEX-GPT5-20260522
task_id: PR-FINAL-17
task_lock_path: ai-dev-input/10_task_claims/archive/20260522/PR-FINAL-17.lock.removed
slice: repository-boundary-extraction
title: 拆 EnginePersistenceService：Repository 边界收口
owner: Codex-GPT5
role: 架构师 AI
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: 151f1b0
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T13:03+08:00
last_heartbeat: 2026-05-22T13:22+08:00
expected_finish: 2026-05-22T20:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: yes
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-17-CODEX-GPT5-R01
review_status: APPROVED
reviewer: Codex-GPT5
open_findings: 0
quality_gate: PASSED
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
ai-dev-input/10_task_claims/active_locks/PR-FINAL-17.lock removed on archive
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
claim_pushed_before_code: DONE
task_ledger_in_progress: DONE
git_status_checked_before_edit: DONE
last_heartbeat_pushed: PENDING_FINAL_PUSH
review_status_synced: DONE
task_ledger_done_synced: DONE
commit_hash_recorded: ea8f5f9
post_push_git_status_clean: PENDING_FINAL_PUSH
task_lock_removed_on_archive: DONE
```

## Verification

```text
verify-task-prereq: initial FAIL because PR-FINAL-17 was missing from task ledger; ledger row added before business code edits.
mvn_compile: PASS — mvn -q -DskipTests compile
focused_test: PASS — mvn -q -Dtest=EngineApiContractTests test
run-tests: PASS — medkernel-mvp/scripts/run-tests.ps1; surefire reports=14 tests=260 failures=0 errors=0 skipped=0
build: PASS — medkernel-mvp/scripts/build.ps1; target/medkernel-mvp-0.1.0-SNAPSHOT.jar
git diff --check: PASS
verify-pr: PASS — .\scripts\verify-pr.ps1 -TaskId PR-FINAL-17; 16 PASS / 0 FAIL / 2 WARN
```

## Self Check

```text
task_card_satisfied: yes
write_scope_matches_diff: yes
tests_updated: N/A_REFACTOR_ONLY
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
review_id: RV-PR-FINAL-17-CODEX-GPT5-R01
review_file: ai-dev-input/11_ai_reviews/approved/RV-PR-FINAL-17-CODEX-GPT5-R01.md
review_status: APPROVED
highest_severity: none
open_findings: 0
changes_requested: no
approved_by: Codex-GPT5
approved_at: 2026-05-22T13:22+08:00
submit_allowed: true
```

## Progress

```text
2026-05-22T13:03+08:00 ACTIVE - Created PR-FINAL-17 claim after prereq showed missing ledger registration and no active lock conflict.
2026-05-22T13:17+08:00 ACTIVE - Extracted PathwayInstanceRepository, RuleExecLogRepository, SourceDocumentRepository, AuditLogRepository, IdAllocatorRepository and PersistenceRepositorySupport; backend compile green.
2026-05-22T13:22+08:00 DONE - Backend 260 tests and build passed; review approved; claim archived and task lock removed.
```

## Completion

```text
commit: ea8f5f9
push: origin/develop pending final status-sync push
tests: backend 260 PASS; build PASS; verify-pr 16 PASS / 0 FAIL / 2 WARN
review: RV-PR-FINAL-17-CODEX-GPT5-R01 APPROVED, open_findings=0
risks: Remaining EnginePersistenceService still owns pathway draft/version, rule definition, Dify template and unmapped queue persistence; those are separate domains outside the six repository boundaries requested here.
```
