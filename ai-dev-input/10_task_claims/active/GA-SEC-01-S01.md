# AI Task Claim

claim_id: GA-SEC-01-S01
task_id: GA-SEC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
slice: S01
title: 等保 2.0 三级控制点
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T22:15+08:00
last_heartbeat: 2026-05-23T22:15+08:00
expected_finish: 2026-05-24T06:00+08:00
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
write_scope:
read_scope:
forbidden_scope:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
```

## Write Scope

```text
docs/security/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-SEC-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
```

## Read Scope

```text
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/**
deploy/**
docs/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
GA-OPS-01（已完成：监控告警与 SLO）
GA-REL-01（已完成：分支保护）
```

## Acceptance

```text
1. 等保 2.0 三级控制点自查清单
2. 每个控制点的合规状态和证据
3. 不合规项的整改计划
4. 文档可交付审计
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 等保 2.0 三级控制点自查清单
- [ ] 合规证据映射
- [ ] 整改计划
- [ ] 更新台账
- [ ] commit + push
```

## Completion

```text
commit:
push:
tests:
review:
risks:
