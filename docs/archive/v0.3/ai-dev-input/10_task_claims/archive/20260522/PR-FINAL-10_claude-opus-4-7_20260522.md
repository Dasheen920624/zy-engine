# AI Task Claim

claim_id: PR-FINAL-10-S02
task_id: PR-FINAL-10
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-10.lock
slice: S02
title: 租户开通向导（Codex 实装 cherry-pick 重整）
owner: claude-opus-4-7@pr-final-10-cherry-pick
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pr-final-10-cherry-pick
git_base_commit: 7b41fe6
git_status_at_claim: clean
created_at: 2026-05-22T07:00+08:00
last_heartbeat: 2026-05-22T07:00+08:00
expected_finish: 2026-05-22T08:00+08:00
heartbeat_interval_minutes: 60
database_mode: not_required
oracle_available: false
local_db_verified: not_required
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id:
write_scope: see "Write Scope" section
read_scope: see "Read Scope" section
forbidden_scope: see "Forbidden Scope" section

## Task Lock

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-10.lock`

## 任务背景

PR-FINAL-10 原由 Codex 实装在 `origin/codex/pr-final-10-tenant-onboarding` 分支（commit 24cb969），但该分支 base 已严重落后于 develop（基于 v0.3-beta 前的 main），与多次 release（PR-FINAL-00..06 + 11 + 15）形成实质冲突。

按用户拍板（2026-05-22）：保留 Codex 的业务实装成果，cherry-pick 到基于最新 develop 的新分支 `claude/pr-final-10-cherry-pick`，重整菜单/路由 + 重写认领卡。原 codex 分支会在本 PR 合并后删除。

**保留备份**：原 codex 分支已 `git bundle` 到 `D:/vibeCoding/claudeCode/medkernel-codex-archive/codex-pr-final-10-tenant-onboarding-20260522.bundle`（永久可恢复）+ 3 个 patch 文件。

## Write Scope

```text
frontend/src/api/tenantOnboarding.ts                            # checkout from codex 分支
frontend/src/pages/Tenant/Onboarding/**                         # checkout from codex 分支：5 文件 + steps/
frontend/src/router/menuConfig.tsx                              # 加 tenant-onboarding 到 M4 用户与身份（ShopOutlined）
frontend/src/router/routes.tsx                                  # /tenant/onboarding 改真实组件
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                          # §1 表 PR-FINAL-10 → IN REVIEW
ai-dev-input/10_task_claims/active/PR-FINAL-10_*.md            # 本认领卡（claim_id S02 区分 Codex 原版 S01）
ai-dev-input/10_task_claims/active_locks/PR-FINAL-10.lock      # 任务锁
ai-dev-input/13_feature_acceptance/pending/FA-PR-FINAL-10-S02.md  # acceptance
```

## Read Scope

```text
origin/codex/pr-final-10-tenant-onboarding                      # 只读：参考 Codex 实装
medkernel-mvp/src/main/java/com/medkernel/tenant/**             # 只读：后端已就绪（develop）
frontend/src/api/types.ts                                       # 只读
frontend/src/components/**                                      # 只读：复用 SourceInfo / StatusBadge
```

## Forbidden Scope

```text
frontend/src/api/types.ts                                       # 架构师专属
frontend/src/styles/tokens*.{css,ts}                            # 架构师专属
frontend/src/theme/** App.tsx main.tsx                          # 架构师专属
frontend/eslint-rules/** eslint.config.js                       # 架构师专属
medkernel-mvp/**                                                # 后端 develop 已就绪
docs/01-05_*.md                                                 # 金本位 V2
```

## Dependencies

```text
后端：tenant/TenantOnboardingController + SEC-011 已合 develop（v0.3-final-rc1 / commit 60c1f02）
前端：Codex 实装内容直接 checkout，无前置依赖
```

## Acceptance

```text
1. /tenant/onboarding 路由可访问，渲染 3 步开通向导（OnboardingWizard）
2. Step1 信息（医院名 / 编码 / 联系人）+ Step2 套餐选择（试用/标准/专业/旗舰）+ Step3 默认配置包 + SSO 初始化
3. OnboardingSuccess 成功页：管理员账号生成提示
4. 菜单：M4 用户与身份新增「租户开通向导」入口（ShopOutlined）
5. 路由：/tenant/onboarding 改真实组件
6. UI：100% CSS Modules + var(--mk-*) token（Codex 实装已合规，cherry-pick 不引入新 inline）
7. 备份保留：本 PR 合并后才删 codex 远端分支
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: in_progress
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T07:00+08:00
review_status_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
./scripts/check-inline-style-count.ps1   # 不上涨
[CI]
backend-build-test + guard-rules
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: not_required（Codex 原版未提供单元测试，本次 cherry-pick 不补；可在合并后另开 follow-up 补测）
docs_updated: yes
required_code_comments_complete: yes（Codex 实装已含 JSDoc）
feature_acceptance_created: pending
security_privacy_checked: yes（不存储 LLM API key；租户配置由后端管理）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: false
```

## Progress

```text
[2026-05-22T07:00] 认领 + 锁定 PR-FINAL-10（cherry-pick from codex/pr-final-10-tenant-onboarding）
```

## Completion

```text
commit:
push:
tests: not_required
review:
risks: Codex 原版未提供单测，本次 cherry-pick 保留同等覆盖度；可在 v0.3-final-rc2 之前另开 follow-up 补测。
```
