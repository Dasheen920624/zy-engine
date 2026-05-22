# AI Task Claim

claim_id: GA-UX-01-S01
task_id: GA-UX-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
slice: S01
title: 客户可见路由完整度
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: aeeab95
git_status_at_claim: clean
created_at: 2026-05-23T21:30+08:00
last_heartbeat: 2026-05-23T21:30+08:00
expected_finish: 2026-05-24T09:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
```

## Write Scope

```text
frontend/src/router/routes.tsx
frontend/src/pages/**
ai-dev-input/10_task_claims/active/GA-UX-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
frontend/src/router/**
frontend/src/pages/**
docs/PRODUCT_SIMPLIFICATION_V1_GA.md
docs/04_页面规格书.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/api/types.ts
frontend/src/router/menuConfig.tsx
frontend/src/styles/tokens.css
frontend/src/App.tsx
frontend/package.json
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. 客户可见路由无 PlaceholderPage
2. 客户可见路由无 404
3. 客户可见路由无空白页
4. 所有路由指向真实组件
5. 前端 typecheck + lint + build 通过
```

## Verification

```text
cd frontend && npm run typecheck && npm run lint && npm run build
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
- [ ] 创建 claim + lock 并 push
- [ ] 扫描所有路由找出 PlaceholderPage
- [ ] 为每个 PlaceholderPage 创建最小可用页面
- [ ] 前端验证
- [ ] 更新台账
- [ ] commit + push
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
