# AI Task Claim

claim_id: PR-FINAL-26_S01
task_id: PR-FINAL-26
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-26.lock
slice: S01
title: Admin Layout 布局重构（左菜单/顶部导航 fixed + 折叠 + 独立滚动 + 响应式）
owner: claude-opus-4.7-pr-final-26-session
role: senior
status: ACTIVE
branch: claude/pr-final-26-admin-layout
target_base_branch: develop
git_base_commit: de19acc93674e49194ce0bd65b4ebab17ced4cf2
git_status_at_claim: clean
created_at: 2026-05-22T18:20+08:00
last_heartbeat: 2026-05-22T23:07+08:00
expected_finish: 2026-05-23T18:00+08:00
heartbeat_interval_minutes: 60
database_mode: N/A (frontend-only)
oracle_available: N/A
local_db_verified: N/A
oracle_verification_required: false
review_required: true
review_id: pending
review_status: NOT_REQUESTED
reviewer: pending
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id: N/A
write_scope:
  - frontend/src/layouts/AppLayout.tsx
  - frontend/src/layouts/SideMenu.tsx
  - frontend/src/layouts/TopNav.tsx
  - frontend/src/layouts/AppLayout.module.css
  - frontend/src/layouts/SideMenu.module.css
  - frontend/src/layouts/TopNav.module.css
  - frontend/src/layouts/useSidebarCollapsed.ts
  - frontend/src/layouts/__tests__/AppLayout.test.tsx
  - frontend/src/styles/global.css
  - scripts/check-inline-style-count.ps1
  - ai-dev-input/10_task_claims/active/PR-FINAL-26_S01.md
  - ai-dev-input/10_task_claims/active_locks/PR-FINAL-26.lock
read_scope:
  - frontend/src/styles/tokens.css
  - frontend/src/styles/tokens.ts
  - frontend/src/App.tsx
  - frontend/src/router/menuConfig.tsx
  - frontend/src/router/routes.tsx
  - frontend/src/theme/ThemeSelector.tsx
  - frontend/src/hooks/useOrgContext.ts
  - frontend/src/store/auth.ts
forbidden_scope:
  - frontend/src/api/types.ts
  - frontend/src/router/menuConfig.tsx (改菜单内容)
  - frontend/src/router/routes.tsx (改路由结构)
  - frontend/src/styles/tokens.css (改 token)
  - frontend/src/theme/**
  - frontend/src/App.tsx (改根容器结构)
  - frontend/eslint-rules/**
  - frontend/eslint.config.js
  - docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md (PR #41 已添加 PR-FINAL-26 行，避免冲突)

## Task Lock

`ai-dev-input/10_task_claims/active_locks/PR-FINAL-26.lock`

## Write Scope

```text
frontend/src/layouts/AppLayout.tsx                          重写：CSS Module + 折叠 + 响应式容器
frontend/src/layouts/SideMenu.tsx                           重写：去 inline style + CSS Module
frontend/src/layouts/TopNav.tsx                             重写：去 inline style + 加 collapse trigger
frontend/src/layouts/AppLayout.module.css                   新建：grid + fixed positioning + 响应式
frontend/src/layouts/SideMenu.module.css                    新建：菜单分组样式 + :global 桥接 antd
frontend/src/layouts/TopNav.module.css                      新建：顶部导航样式
frontend/src/layouts/useSidebarCollapsed.ts                 新建：localStorage 持久化 hook
frontend/src/layouts/__tests__/AppLayout.test.tsx           新建：render + collapse 交互测试
frontend/src/styles/global.css                              删除：.mk-app-* / .mk-side-menu* / .mk-top-nav* (迁移至 module)
scripts/check-inline-style-count.ps1                        校准 baseline (493 → 实际新值)
```

## Read Scope

```text
frontend/src/styles/tokens.css                              设计 token 来源（--mk-*）
frontend/src/App.tsx                                        根挂载点
frontend/src/router/menuConfig.tsx                          菜单数据契约（不改）
frontend/src/router/routes.tsx                              路由数据契约（不改）
frontend/src/theme/ThemeSelector.tsx                        TopNav 集成（不改）
frontend/src/hooks/useOrgContext.ts                         TopNav 用（不改）
frontend/src/store/auth.ts                                  TopNav 用（不改）
```

## Forbidden Scope

```text
任何 §3 共享文件清单：tokens.css/ts、theme-tokens.ts、theme/**、router/menuConfig.tsx、router/routes.tsx、api/types.ts、App.tsx、main.tsx、eslint-rules/**、eslint.config.js
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md：PR #41 已加 PR-FINAL-26 行，本 PR 不动避免冲突；status 改 ✅ DONE 走 followup commit
```

## Dependencies

```text
前置：无（任务文档 commit 0743883 在 PR #41 上待合 develop，但本 PR 实施不依赖其合并）
后置：PR #41 合 develop 后，followup commit 把 backlog 表格 PR-FINAL-26 行的 🟡 TODO 改 ✅ DONE
影响范围：所有路由页面（layout 是全局根），需全站手测 6 个核心页面
```

## Acceptance

```text
1. 长内容页面滚动时，sidebar 与 header 保持位置不动（fixed）
2. 点击 sidebar 折叠按钮 → 220px → 64px（图标态），刷新页面状态保留
3. 浏览器宽度 < 768px 时 sidebar 自动收起，header 出现汉堡按钮
4. inline style 计数减少 ≥ 5 处（AppLayout 1 + SideMenu 2 + TopNav 3）
5. typecheck / lint / unit test / build 全 PASS
6. ./scripts/check-inline-style-count.ps1 PASS（baseline 校准后）
7. 全站 6 个核心页面（Login / Dashboard / Pathway / Rule / MPI / Admin）目视无回归
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
task_ledger_in_progress: N/A (本任务无 02_任务台账 行)
git_status_checked_before_edit: 2026-05-22T18:15 clean
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: N/A
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
cd frontend && npm run typecheck
cd frontend && npm run lint
cd frontend && npm test -- --run src/layouts
cd frontend && npm run build
./scripts/check-inline-style-count.ps1
全站目视：localhost:5173 → Login / Dashboard / Pathway / Rule / MPI / Admin
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: pending
samples_or_api_examples_updated: N/A (无 API 改动)
docs_updated: N/A (backlog 表格不动)
db_only_checked: N/A (frontend-only)
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: pending
feature_acceptance_created: false (高级 AI 自验)
claim_status_synced: ACTIVE
security_privacy_checked: pass (无 PII / 无 token 改动)
```

## Quality Review

```text
review_id: pending
review_file: pending
review_status: NOT_REQUESTED
highest_severity: pending
open_findings: 0
changes_requested: pending
approved_by: pending
approved_at: pending
submit_allowed: false
```

## Progress

```text
2026-05-22T18:20 claim 创建，准备实施
```

## Handoff

```text
无（单 AI 单 slice）
```

## Completion

```text
commit: pending
push: pending
tests: pending
review: pending
risks:
  - 全站布局根改动 → 需手动验证 6 核心页面
  - 响应式 < 768px drawer → 需小屏目视
  - global.css 删除 .mk-app-* → 全 src grep 确认 0 引用再删
```

