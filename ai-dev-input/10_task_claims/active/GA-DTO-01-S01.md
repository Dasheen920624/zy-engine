# AI Task Claim

claim_id: GA-DTO-01-S01
task_id: GA-DTO-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DTO-01.lock
slice: S01
title: Adapter Controller 入参 DTO + @Valid，无新增 raw Map
owner: TraeAI-1
role: 高级
status: ACTIVE
branch: ai/GA-DTO-01/adapter-dto
target_base_branch: develop
git_base_commit: 98e2a87
git_status_at_claim: clean
created_at: 2026-05-23T12:00:00+08:00
last_heartbeat: 2026-05-23T12:00:00+08:00
expected_finish: 2026-05-24T12:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope:
```text
medkernel-mvp/src/main/java/com/medkernel/adapter/**
ai-dev-input/10_task_claims/active/GA-DTO-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DTO-01.lock
docs/engineering/02_任务台账.md
```

read_scope:
```text
medkernel-mvp/src/main/java/com/medkernel/**
docs/**
```

forbidden_scope:
```text
frontend/src/api/types.ts
frontend/src/router/menuConfig.tsx
frontend/src/router/routes.tsx
frontend/src/styles/tokens.css
frontend/src/App.tsx
medkernel-mvp/src/main/java/com/medkernel/common/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
scripts/verify-pr.ps1
```

## Dependencies

```text
无硬依赖（Batch 4 可并行）
```

## Acceptance

```text
1. Adapter Controller 所有入参改为 DTO + @Valid
2. 无新增 raw Map<String, Object> 入参
3. 所有新增 DTO 有中文注释
4. 契约测试更新
```

## Progress

```text
- 分析 Adapter Controller 现有入参
- 创建 DTO 类
- 替换 raw Map 为 DTO
- 添加 @Valid 校验
- 更新测试
```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
