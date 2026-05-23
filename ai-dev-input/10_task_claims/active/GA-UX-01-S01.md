# AI Task Claim

claim_id: GA-UX-01-S01
task_id: GA-UX-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
slice: S01
title: 客户可见路由无 PlaceholderPage，全部替换为真实页面
owner: TraeAI-1
role: 高级
status: ACTIVE
branch: ai/GA-UX-01/no-placeholder
target_base_branch: develop
git_base_commit: 0dbead4
git_status_at_claim: clean
created_at: 2026-05-23T14:30:00+08:00
last_heartbeat: 2026-05-23T14:30:00+08:00
expected_finish: 2026-05-24T14:30:00+08:00
heartbeat_interval_minutes: 60
write_scope:
```text
frontend/src/pages/**
ai-dev-input/10_task_claims/active/GA-UX-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-UX-01.lock
docs/engineering/02_任务台账.md
```

read_scope:
```text
frontend/src/**
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
无硬依赖（Batch 1 可并行）
```

## Acceptance

```text
1. 所有客户可见路由对应的页面不再是 PlaceholderPage
2. 每个页面有真实的业务内容（至少有标题、描述和基本布局）
3. 页面与后端 API 对接（至少有数据展示框架）
4. 无空白占位页面
```
