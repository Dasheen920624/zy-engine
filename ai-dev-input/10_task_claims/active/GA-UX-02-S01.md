# AI Task Claim

claim_id: GA-UX-02-S01
task_id: GA-UX-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-UX-02.lock
slice: S01
title: 全页面极简交互复核
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-UX-02/page-simplification
target_base_branch: develop
git_base_commit: e284bc3
git_status_at_claim: clean
created_at: 2026-05-23T22:00+08:00
last_heartbeat: 2026-05-23T22:00+08:00
expected_finish: 2026-05-24T10:00+08:00
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

## Write Scope

```text
frontend/src/pages/**
docs/04_页面规格书.md
ai-dev-input/10_task_claims/active/GA-UX-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-UX-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/PRODUCT_SIMPLIFICATION_V1_GA.md
docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md
frontend/src/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/api/**
frontend/src/router/**
frontend/src/store/**
frontend/src/styles/**
frontend/src/components/**
ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md
ai-dev-input/10_task_claims/active/GA-QA-02-S01.md
```

## Dependencies

```text
GA-UX-01 (DONE) — 路由完整度已确保
```

## Acceptance

```text
1. 所有页面默认路径清晰，主操作按钮突出
2. 高级参数/高级操作折叠在 Collapse 或高级搜索中
3. 客户不需要理解技术名词即可完成主任务
4. 每个页面最多 3 个默认筛选条件
5. 复杂表单使用分步向导或分 Tab 组织
```

## Verification

```text
cd frontend && npm run lint && npm run typecheck && npm run build
```

## Progress

```text
- [x] 创建 claim + lock 并 push
- [ ] 逐页面复核并简化交互
- [ ] 更新台账
- [ ] commit + push
```
