# AI Task Claim

claim_id: FIX-DEV-010-S01
task_id: FIX-DEV-010
task_lock_path: ai-dev-input/10_task_claims/active_locks/FIX-DEV-010.lock
slice: develop-compile-health
title: develop 主干编译修复与协作哨兵收敛
owner: Codex-GPT5
role: senior backend integration engineer
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: de19acc
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T18:40+08:00
last_heartbeat: 2026-05-22T18:40+08:00
expected_finish: 2026-05-22T20:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_compile_fix
local_db_verified: pending
oracle_verification_required: false
review_required: true
review_id: RV-FIX-DEV-010-S01-R01
review_status: NOT_REQUESTED
reviewer: Codex-GPT5
open_findings: pending
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id: N/A
write_scope:
  - ai-dev-input/00_DEVELOP_HEALTH.md
  - ai-dev-input/10_task_claims/active/FIX-DEV-010-S01.md
  - ai-dev-input/10_task_claims/active_locks/FIX-DEV-010.lock
  - ai-dev-input/10_task_claims/active/*.md
  - ai-dev-input/10_task_claims/active_locks/*.lock
  - medkernel-mvp/src/main/java/com/medkernel/security/IdentityProviderRepository.java
  - medkernel-mvp/src/main/java/com/medkernel/security/SecurityUserRowMapper.java
read_scope:
  - docs/AI_CHARTER.md
  - docs/engineering/00_总入口与AI接手导航.md
  - docs/engineering/06_后端开发规范.md
  - medkernel-mvp/src/main/java/com/medkernel/security/**
forbidden_scope:
  - frontend/src/**
  - ai-dev-input/04_database/**
  - deploy/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/FIX-DEV-010.lock
```

## Dependencies

```text
develop @ de19acc is RED: mvn -q compile fails after IdentityBinding was renamed to SsoIdentityBinding while PR-FINAL-18 split repository code still references the old class name.
```

## Acceptance

```text
mvn -q compile passes from medkernel-mvp.
check-develop-health.ps1 no longer reports GREEN/RED mismatch.
check-ai-collaboration.ps1 has no malformed claim/lock failures introduced by latest merge.
verify-pr.ps1 passes or only reports documented non-blocking warnings.
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: PENDING
task_ledger_in_progress: N/A_HEALTH_SENTINEL_TASK
git_status_checked_before_edit: DONE
last_heartbeat_pushed: PENDING
review_status_synced: PENDING
task_ledger_done_synced: N/A_HEALTH_SENTINEL_TASK
commit_hash_recorded: PENDING
post_push_git_status_clean: PENDING
task_lock_removed_on_archive: PENDING
```

## Verification

```text
pending
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: not_required_compile_integration_fix
samples_or_api_examples_updated: not_required
docs_updated: pending
db_only_checked: not_required
oracle_dm_h2_schema_synced: not_required
production_development_schema_synced: not_required
table_and_column_comments_complete: not_required
required_code_comments_complete: pending
feature_acceptance_created: not_required
claim_status_synced: pending
security_privacy_checked: pending
```

## Quality Review

```text
review_id: RV-FIX-DEV-010-S01-R01
review_file: pending
review_status: NOT_REQUESTED
highest_severity: pending
open_findings: pending
changes_requested: pending
approved_by: pending
approved_at: pending
submit_allowed: false
```

## Progress

```text
2026-05-22T18:40+08:00 ACTIVE - Created after develop fast-forward to de19acc exposed compile failure and malformed active claim/lock metadata.
```

## Handoff

```text
If interrupted, first run mvn -q compile in medkernel-mvp and check-ai-collaboration.ps1 at repo root, then continue from the smallest failing item.
```

## Completion

```text
commit: pending
push: pending
tests: pending
review: pending
risks: pending
```
