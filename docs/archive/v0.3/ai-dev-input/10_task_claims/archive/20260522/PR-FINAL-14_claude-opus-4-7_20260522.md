# AI Task Claim

claim_id: PR-FINAL-14-S01
task_id: PR-FINAL-14
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-14.lock
slice: S01
title: 砍菜单 + Dashboard PENDING 卡更新（PR-FINAL-10/13 实装后收口）
owner: claude-opus-4-7@stoic-kirch-da158e
role: senior
status: ACTIVE
target_base_branch: develop
branch: claude/pr-final-14-menu-sync
git_base_commit: 1f83cb9
git_status_at_claim: clean
created_at: 2026-05-22T15:30+08:00
last_heartbeat: 2026-05-22T15:30+08:00
expected_finish: 2026-05-22T16:00+08:00
heartbeat_interval_minutes: 30
database_mode: not_required
oracle_available: false
local_db_verified: not_required
oracle_verification_required: false
review_required: false
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: PASS_ON_GREEN_CI
feature_acceptance_required: false
feature_acceptance_id:
write_scope: see "Write Scope" section
read_scope: see "Read Scope" section
forbidden_scope: see "Forbidden Scope" section

## Task Lock

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-14.lock`

## 任务背景

PR-FINAL-14 是阶段 2 唯一的初级任务（0.5 天），但因涉及 `docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md`（§3 共享文件清单：架构师专属），由架构师 AI 顺手收口。

PR-FINAL-10/11/13 已合 develop（merge commits `1f83cb9` / `f20c2a7` / `8158a8d`），但 Dashboard 卡片状态仍标 PENDING：
- AI 工作流引擎（PR-FINAL-13）→ 应 READY
- 租户开通（PR-FINAL-10）→ 应 READY

`menuConfig.tsx` 已被 PR-FINAL-10/11/13 各自 PR 维护完毕（rule-definitions / ai-workflows / tenant-onboarding 都已加回 items 数组），本 PR **无需改 menuConfig**。

## Write Scope

```text
frontend/src/pages/Dashboard.tsx                                   # 2 处 PENDING → READY
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md                              # §1 状态表更新（10/13/14/24 标 DONE）
docs/engineering/02_任务台账.md                                     # §2.5.2 新增 10/13/14/24 行
ai-dev-input/10_task_claims/active/PR-FINAL-14_*.md                # 本认领卡
ai-dev-input/10_task_claims/active_locks/PR-FINAL-14.lock          # 任务锁
```

## Read Scope

```text
frontend/src/router/menuConfig.tsx              # 只读：确认 10/11/13 入口已在
frontend/src/router/routes.tsx                  # 只读：确认路由已就绪
```

## Forbidden Scope

```text
frontend/src/router/menuConfig.tsx              # 已干净，不动；架构师专属
frontend/src/router/routes.tsx                  # 架构师专属
frontend/src/api/types.ts                       # 架构师专属
frontend/src/styles/tokens*.{css,ts}            # 架构师专属
medkernel-mvp/**                                # 后端 0 改动
docs/01-05_*.md                                 # 金本位 V2（DOC-V2-* 流程）
```

## Dependencies

```text
PR-FINAL-10（#33 已合 develop `1f83cb9`，租户开通向导）
PR-FINAL-11（已合 develop `f20c2a7`，规则库）
PR-FINAL-13（#32 已合 develop `8158a8d`，AI 工作流引擎页）
```

## Acceptance

```text
1. Dashboard.tsx 「AI 工作流引擎」卡 status 由 PENDING 改 READY（点击可跳 /ai-workflows）
2. Dashboard.tsx 「租户开通」卡 status 由 PENDING 改 READY（点击可跳 /tenant/onboarding）
3. 仍正确标 PENDING 的卡：适配器中心 (PR-FINAL-12) / 患者主索引 (PR-FINAL-07) / 用户管理 (PR-FINAL-08) / 审计日志 (PR-FINAL-09)
4. backlog §1 状态表：PR-FINAL-10/13/14/24 标 ✅ DONE，对应 merge commit / PR 号填上
5. 02_任务台账.md §2.5.2 新增 4 行（PR-FINAL-10/13/14/24）保持与 backlog §1 同步
6. CI 全绿（backend-build-test + guard-rules）
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: yes
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T15:30+08:00
review_status_synced: not_required
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[本地]
./scripts/verify-pr.ps1 -TaskId PR-FINAL-14
./scripts/check-inline-style-count.ps1   # 不上涨（本 PR 0 新增 inline）
[CI]
backend-build-test + guard-rules
```

## Self Check

```text
task_card_satisfied: pending
write_scope_matches_diff: pending
tests_updated: not_required（纯 status 字符串改动，无逻辑变更，依赖 PR-FINAL-10/11/13 已有的页面测试）
docs_updated: yes（backlog + 任务台账同步）
required_code_comments_complete: yes
feature_acceptance_created: not_required（文档维护性 PR，非高风险）
security_privacy_checked: yes（仅前端展示状态改动）
```

## Quality Review

```text
review_status: NOT_REQUESTED
submit_allowed: true_after_local_ci_pass
```

## Progress

```text
[2026-05-22T15:30] 认领 + 锁定 PR-FINAL-14（菜单已干净，仅 Dashboard 2 处 PENDING→READY + 文档同步）
```

## Completion

```text
commit:
push:
tests: not_required（无 Java/TSX 逻辑变更）
review:
risks: 无；纯文案性 status 改动
```
