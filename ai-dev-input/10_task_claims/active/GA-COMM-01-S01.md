# AI Task Claim

claim_id: GA-COMM-01-S01
task_id: GA-COMM-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
slice: S01
title: License 与用量报告
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-24T00:00+08:00
last_heartbeat: 2026-05-24T00:00+08:00
expected_finish: 2026-05-24T08:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
```

## Write Scope

```text
docs/commercial/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-COMM-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
```

## Read Scope

```text
docs/**
deploy/**
medkernel-mvp/src/main/resources/application.yml
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
GA-LEGAL-01（已完成：合同/SLA/隐私政策/DPA）
```

## Acceptance

```text
1. License 授权模型文档
2. 用量指标定义与采集方案
3. 用量报告模板
4. 到期提醒机制文档
5. 所有文档可交付商务团队
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] License 授权模型文档
- [ ] 用量指标定义与采集方案
- [ ] 用量报告模板
- [ ] 到期提醒机制
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
