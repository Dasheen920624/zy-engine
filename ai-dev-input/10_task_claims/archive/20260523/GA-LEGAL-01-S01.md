# AI Task Claim

claim_id: GA-LEGAL-01-S01
task_id: GA-LEGAL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
slice: S01
title: 合同/SLA/隐私政策/DPA
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T22:45+08:00
last_heartbeat: 2026-05-23T22:45+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
```

## Write Scope

```text
docs/legal/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-LEGAL-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
```

## Read Scope

```text
docs/**
deploy/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
GA-OPS-01（已完成：SLO/SLA 定义）
GA-SEC-01（已完成：等保控制点自查）
```

## Acceptance

```text
1. 软件许可协议模板
2. SLA 服务等级协议模板
3. 隐私政策模板
4. DPA 数据处理协议模板
5. 所有模板可交付法务审核
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 软件许可协议
- [ ] SLA 服务等级协议
- [ ] 隐私政策
- [ ] DPA 数据处理协议
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
