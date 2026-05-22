# AI Task Claim

claim_id: PR-V3-PLACEHOLDER-CULL-CODEBUDDY-20260522
task_id: PR-V3-PLACEHOLDER-CULL
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-V3-PLACEHOLDER-CULL.lock
slice: frontend-placeholder-culling
title: 砍 / 实装所有 PlaceholderPage 入口 + 同步 menuConfig
owner: CodeBuddy
role: 前端工程师 AI
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: bf77832
git_status_at_claim: clean; develop...origin/develop
created_at: 2026-05-22T14:05+08:00
last_heartbeat: 2026-05-22T14:05+08:00
expected_finish: 2026-05-22T18:00+08:00
heartbeat_interval_minutes: 60
database_mode: no_schema_change
oracle_available: not_required_for_this_task
local_db_verified: not_required
oracle_verification_required: false
review_required: true
review_id: RV-PR-V3-PLACEHOLDER-CULL-R01
review_status: PENDING
reviewer: CodeBuddy
open_findings: pending
quality_gate: PENDING
feature_acceptance_required: true
feature_acceptance_id: FA-PR-V3-PLACEHOLDER-CULL-S01
write_scope:
  - frontend/src/router/menuConfig.tsx
  - frontend/src/router/routes.tsx
  - frontend/src/pages/**
  - frontend/src/components/**
  - docs/engineering/02_任务台账.md
  - ai-dev-input/10_task_claims/**
  - ai-dev-input/11_ai_reviews/**
  - ai-dev-input/13_feature_acceptance/**
read_scope:
  - docs/AI_CHARTER.md
  - docs/AI_TEAM_SOP.md
  - docs/PRODUCT_ARCHITECTURE_FINAL.md
  - docs/engineering/00_总入口与AI接手导航.md
  - docs/engineering/07_前端开发规范.md
  - frontend/src/api/**
  - frontend/src/styles/**
forbidden_scope:
  - medkernel-mvp/**
  - ai-dev-input/04_database/**
  - deploy/**
  - docs/engineering/06_后端开发规范.md

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/PR-V3-PLACEHOLDER-CULL.lock
```

## Dependencies

```text
PR-FINAL-04: DONE (CSS Modules 框架)
PR-FINAL-09: DONE (审计日志页面)
PR-FINAL-10: DONE (租户开通向导)
PR-FINAL-11: DONE (规则库页面)
PR-FINAL-12: DONE (适配器中心页面)
PR-FINAL-13: DONE (AI工作流引擎页面)
```

## Acceptance

```text
所有PlaceholderPage入口被实装或移除，演示零404。
已有实际组件的页面（如适配器中心、患者主索引、用户管理、审计日志）更新Dashboard状态为READY。
菜单配置与路由配置保持一致。
使用CSS Modules + var(--mk-*) token样式。
0新增inline style。
前端lint和typecheck通过。
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: LOCAL_CREATED
task_ledger_in_progress: PENDING
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
2026-05-22T14:05+08:00 ACTIVE - Created PR-V3-PLACEHOLDER-CULL claim after analyzing frontend placeholder pages and routes.
```