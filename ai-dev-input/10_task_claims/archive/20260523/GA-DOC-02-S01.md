# AI Task Claim

claim_id: GA-DOC-02-S01
task_id: GA-DOC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
slice: S01
title: 运维与应急手册
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T22:30+08:00
last_heartbeat: 2026-05-23T22:30+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
```

## Write Scope

```text
docs/ops/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-DOC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
```

## Read Scope

```text
deploy/**
docs/**
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
GA-OPS-01（已完成：监控告警与 SLO）
GA-DB-01（已完成：Flyway 回滚指南）
GA-REL-01（已完成：发布流程）
```

## Acceptance

```text
1. 日常运维手册（启动/停止/配置/监控/日志）
2. 备份恢复手册（全量/增量/验证）
3. 升级回滚手册（版本升级/回滚/验证）
4. 故障应急手册（常见故障/排查流程/应急预案）
5. 文档可交付医院信息科
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 日常运维手册
- [ ] 备份恢复手册
- [ ] 升级回滚手册
- [ ] 故障应急手册
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
