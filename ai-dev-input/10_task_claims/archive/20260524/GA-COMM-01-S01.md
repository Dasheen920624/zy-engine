# AI Task Claim

claim_id: GA-COMM-01-S01
task_id: GA-COMM-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
slice: S01
title: License、用量报告、授权到期提醒闭环
owner: TraeAI-1
role: 高级
status: ACTIVE
branch: ai/GA-COMM-01/license-usage
target_base_branch: develop
git_base_commit: c3d76df
git_status_at_claim: clean
created_at: 2026-05-23T16:00:00+08:00
last_heartbeat: 2026-05-23T16:00:00+08:00
expected_finish: 2026-05-24T16:00:00+08:00
heartbeat_interval_minutes: 60
write_scope:
```text
medkernel-mvp/src/main/java/com/medkernel/license/**
frontend/src/pages/**
ai-dev-input/10_task_claims/active/GA-COMM-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
docs/engineering/02_任务台账.md
```

read_scope:
```text
medkernel-mvp/src/main/java/com/medkernel/**
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
无硬依赖（Batch 3 可并行）
```

## Acceptance

```text
1. License 验证服务：启动时校验、运行时校验、过期拒绝
2. 用量报告：API 调用量、活跃用户数、功能使用统计
3. 授权到期提醒：到期前30/15/7天通知、到期后降级模式
4. 前端页面：License 状态页、用量仪表盘
5. 完整闭环：License -> 用量 -> 到期提醒 -> 降级
```
