# AI Task Claim

claim_id: GA-REFIT-01-S01
task_id: GA-REFIT-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REFIT-01.lock
slice: S01
title: 超长文件持续拆分
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-REFIT-01/file-split
target_base_branch: develop
git_base_commit: 8573aea
git_status_at_claim: clean
created_at: 2026-05-23T22:30+08:00
last_heartbeat: 2026-05-23T22:30+08:00
expected_finish: 2026-05-24T10:30+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false

## Write Scope

```text
frontend/src/pages/Onboarding/ImplementationGuidePage.tsx
frontend/src/mocks/handlers.ts
frontend/src/pages/Terminology/MappingWorkbench.tsx
frontend/src/api/modules.test.ts
ai-dev-input/10_task_claims/active/GA-REFIT-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-REFIT-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
frontend/src/**
docs/**
```

## Acceptance

```text
1. 所有 >800 行的 .ts/.tsx 文件拆分到 800 行以下
2. 拆分后功能不变，测试通过
3. 每个拆分出的新文件有明确的职责
```

## Progress

```text
- [x] 创建 claim + lock 并 push
- [ ] 拆分 ImplementationGuidePage.tsx (941行)
- [ ] 拆分 handlers.ts (862行)
- [ ] 拆分 MappingWorkbench.tsx (802行)
- [ ] 拆分 modules.test.ts (1093行)
- [ ] 更新台账
- [ ] commit + push
```
