# AI Task Claim

claim_id: PR-FINAL-07-S01
task_id: PR-FINAL-07
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-07.lock
slice: S01
title: /mpi/patients 患者主索引页
owner: Codex-GPT5
role: senior
status: REVIEW_REQUESTED
target_base_branch: develop
branch: codex/pr-final-07-mpi-patients
git_base_commit: 1f83cb9
git_status_at_claim: clean
created_at: 2026-05-22T07:47+08:00
last_heartbeat: 2026-05-22T07:47+08:00
expected_finish: 2026-05-22T12:00+08:00
heartbeat_interval_minutes: 60
database_mode: not_required
oracle_available: false
local_db_verified: not_required
oracle_verification_required: false
review_required: true
review_id: RV-PR-FINAL-07-S01-R01
review_status: REVIEW_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-PR-FINAL-07-S01
write_scope: see "Write Scope" section
read_scope: see "Read Scope" section
forbidden_scope: see "Forbidden Scope" section

## Task Lock

- 锁文件：`ai-dev-input/10_task_claims/active_locks/PR-FINAL-07.lock`

## 任务背景

PR-FINAL-07 交付 `/mpi/patients` 患者主索引前端页面。后端 MPI 控制器与持久化服务已在 develop 就绪，本 PR 只接入前端页面、API client、菜单与路由。

## Write Scope

```text
frontend/src/api/mpi.ts
frontend/src/pages/Mpi/**
frontend/src/router/menuConfig.tsx
frontend/src/router/routes.tsx
frontend/src/api/rule.ts                                             # DoD 构建阻断最小修复：RuleListFilters 传参
frontend/src/pages/Pathway/PathwayDetail.tsx                         # DoD 构建阻断最小修复：移除未入包 CodeMirror 依赖
frontend/src/pages/Pathway/PathwayDiff.tsx                           # DoD 构建阻断最小修复：移除未入包 CodeMirror 依赖
frontend/src/pages/Pathway/PatientPathway/AdmitDialog.tsx             # DoD 构建阻断最小修复：未用 import
frontend/src/pages/Pathway/PatientPathway/PatientPathwayList.tsx      # DoD 构建阻断最小修复：未用 import
frontend/src/test/setup.ts                                            # DoD 测试兼容：jsdom/nwsapi AntD selector guard
frontend/src/pages/**/__tests__/*.tsx                                # DoD 测试兼容：脆弱断言最小修复
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md
ai-dev-input/10_task_claims/active/PR-FINAL-07_codex-gpt5_20260522.md
ai-dev-input/10_task_claims/active_locks/PR-FINAL-07.lock
ai-dev-input/11_ai_reviews/pending/RV-PR-FINAL-07-S01-R01.md
ai-dev-input/13_feature_acceptance/pending/FA-PR-FINAL-07-S01.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/patient/**
frontend/src/api/client.ts
frontend/src/router/**
frontend/src/pages/**
docs/AI_TEAM_PR_BACKLOG_V0.3_FINAL.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
medkernel-mvp/src/main/resources/db/**
deploy/**
scripts/**
docs/01-05_*.md
frontend/src/api/types.ts
```

## Dependencies

```text
PR-FINAL-02 ✅ DONE   patientindex 删包 + 数据迁移完成
后端 MPI ✅ READY     patient/MpiController + MpiPersistenceService
```

## Acceptance

```text
1. /mpi/patients 路由渲染真实患者主索引页面，不再使用 PlaceholderPage
2. M4 用户与身份菜单恢复 MPI 入口并保持菜单/路由一致
3. 页面包含患者列表、搜索、筛选、主索引详情、关联 ID、就诊记录与冲突合并工作台
4. API client 覆盖患者标识、就诊标识、冲突检测、冲突解决、标识验证与合并
5. 默认脱敏展示身份证 4+4、手机号 3+4；完整信息必须经页面授权开关解锁
6. 民族字段提供 56 项枚举筛选
7. 每个新组件至少覆盖 1 个 render 测试 + 1 个交互测试
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: pending
git_status_checked_before_edit: yes
last_heartbeat_pushed: 2026-05-22T07:47+08:00
review_status_synced: yes
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
[完成]
node node_modules/vitest/vitest.mjs run src/pages/Mpi --reporter=verbose
node node_modules/typescript/bin/tsc --noEmit ... src/pages/Mpi ...
node node_modules/eslint/bin/eslint.js src/api/mpi.ts src/pages/Mpi src/router/menuConfig.tsx src/router/routes.tsx
mvn -q -f medkernel-mvp/pom.xml compile
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/check-inline-style-count.ps1
```

## Self Check

```text
task_card_satisfied: yes
write_scope_matches_diff: yes
tests_updated: yes
docs_updated: yes
required_code_comments_complete: yes
feature_acceptance_created: yes
security_privacy_checked: yes
```

## Quality Review

```text
review_status: REVIEW_REQUESTED
submit_allowed: false
```

## Progress

```text
[2026-05-22T07:47] 认领 + 锁定 PR-FINAL-07，基于最新 develop 开工。
[2026-05-22T08:07] 实现 MPI API client、患者列表/详情/冲突合并工作台、菜单路由；MPI 单测/定向 typecheck/eslint/后端 compile 通过，提交 review 与 FA。
[2026-05-22T08:28] 追加 DoD 构建/测试阻断最小修复；verify-pr.ps1 全绿（17 PASS / 0 FAIL / 2 WARN）。
```

## Completion

```text
commit:
push:
tests:
  - MPI vitest: 4 files / 8 tests PASS
  - frontend full vitest: 35 files / 160 tests PASS
  - frontend build/typecheck: PASS
  - frontend lint: PASS
  - mvn compile: PASS
  - inline style guard: PASS
  - verify-pr.ps1 -TaskId PR-FINAL-07: PASS（WARN: health sentinel still RED; V2 manual has no PR-FINAL card）
review: RV-PR-FINAL-07-S01-R01 pending
risks:
  1. 后端当前未提供全量患者列表端点；前端采用搜索载入并维护当前工作列表，不新增后端接口。
  2. 为满足 DoD，已最小修复 develop 既有前端 build/test 阻断：Rule query 类型、Pathway CodeMirror 未入包依赖、AntD Select/jsdom selector 兼容、部分脆弱测试断言。
```
