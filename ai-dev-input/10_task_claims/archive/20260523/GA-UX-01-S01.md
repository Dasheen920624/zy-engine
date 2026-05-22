# AI Task Claim

claim_id: GA-UX-01-S01
task_id: GA-UX-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
slice: S01
title: 客户可见路由完整度
owner: TraeAI-5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: b121607
git_status_at_claim: clean
created_at: 2026-05-23T19:10+08:00
last_heartbeat: 2026-05-23T19:10+08:00
expected_finish: 2026-05-23T22:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
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
frontend/src/router/menuConfig.tsx
frontend/src/components/PlaceholderPage.tsx
frontend/src/components/placeholderPage.module.css
frontend/src/components/index.ts
frontend/src/pages/ProvenancePlaceholder.tsx
frontend/src/pages/NotFound.tsx
frontend/eslint-rules/no-placeholder-page.js
frontend/eslint.config.js
frontend/src/pages/Dashboard.tsx
frontend/src/pages/Dashboard.module.css
ai-dev-input/10_task_claims/active/GA-UX-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
frontend/src/pages/**
frontend/src/App.tsx
frontend/src/layouts/**
frontend/package.json
docs/04_页面规格书.md
docs/PRODUCT_SIMPLIFICATION_V1_GA.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/api/**
ai-dev-input/04_database/**
```

## Dependencies

```text
无显式依赖（Batch 1 可并行）
```

## Acceptance

```text
1. 删除未使用的 PlaceholderPage 组件和样式文件
2. 删除未使用的 ProvenancePlaceholder 组件
3. 新增 ESLint 规则 no-placeholder-page 禁止引入 PlaceholderPage
4. 验证所有 menuConfig 菜单项均有对应真实路由和组件
5. Dashboard 卡片链接全部可达真实页面
6. NotFound 页面优化：提供返回导航而非死胡同
7. 前端 lint/typecheck/build 通过
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: pending
git_status_checked_before_edit: pending
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
cd frontend && npx tsc --noEmit && npx eslint src/ --max-warnings 0 && npm run build
```

## Self Check

```text
task_card_satisfied:
write_scope_matches_diff:
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
- [ ] 删除 PlaceholderPage 组件和样式
- [ ] 删除 ProvenancePlaceholder 组件
- [ ] 新增 no-placeholder-page ESLint 规则
- [ ] 验证菜单-路由-组件完整对应
- [ ] Dashboard 卡片链接验证
- [ ] NotFound 页面优化
- [ ] 前端验证通过
- [ ] 更新台账
- [ ] verify-pr + commit + push
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
