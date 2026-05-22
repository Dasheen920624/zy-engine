# AI Task Claim

claim_id: GA-GOV-02-S01
task_id: GA-GOV-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-GOV-02.lock
slice: S01
title: v1.0 GA 文档入口收口
owner: TraeAI-GLM5
role: architect
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 5d5eed56aa77cf41d39b5265251a1590333e5507
git_status_at_claim: clean
created_at: 2026-05-23T20:30:00+08:00
last_heartbeat: 2026-05-23T20:30:00+08:00
expected_finish: 2026-05-24T08:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-GA-GOV-02-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope: docs/**, ai-dev-input/README.md, ai-dev-input/10_task_claims/**
read_scope: medkernel-mvp/**, frontend/**
forbidden_scope: medkernel-mvp/src/main/java/**, frontend/src/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-GOV-02.lock
```

## Write Scope

```text
docs/engineering/00_总入口与AI接手导航.md
docs/DEPLOYMENT_DUAL_MODE.md
docs/engineering/2026-05-21-v0.2-demo-release-PR-draft.md
docs/engineering/2026-05-21-v0.2-demo-演示话术.md
docs/engineering/2026-05-21-功能矩阵-V3.md
docs/engineering/02_任务台账.md
docs/README.md
ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-GOV-02.lock
```

## Read Scope

```text
docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md
docs/PRODUCT_SIMPLIFICATION_V1_GA.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
```

## Dependencies

```text
GA-GOV-01 (DONE)
```

## Acceptance

```text
1. docs/engineering/00_总入口与AI接手导航.md 增加旧 v0.3 文档归档声明
2. docs/DEPLOYMENT_DUAL_MODE.md v0.3 列标注为历史
3. v0.2 时期文件增加归档标注
4. docs/README.md 旧文档列表补全
5. 任务台账 v0.3 历史区块视觉分隔
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: true
git_status_checked_before_edit: true
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: true
tests_updated: N/A
samples_or_api_examples_updated: N/A
docs_updated: true
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: true
feature_acceptance_created: N/A
claim_status_synced: true
security_privacy_checked: true
```

## Quality Review

```text
review_id: RV-GA-GOV-02-S01-R01
review_file: ai-dev-input/11_ai_reviews/pending/RV-GA-GOV-02-S01-R01.md
review_status: NOT_REQUESTED
highest_severity:
open_findings: 0
changes_requested:
approved_by:
approved_at:
submit_allowed: false
```

## Progress

```text
- 补充文档入口归档声明
- 标注旧 v0.3/v0.2 文档为历史
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
