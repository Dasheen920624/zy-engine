# AI Task Claim

claim_id: GA-DOC-02-S01
task_id: GA-DOC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
slice: S01
title: 运维手册、备份恢复、升级回滚、故障应急手册齐备
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-DOC-02/ops-manuals
target_base_branch: develop
git_base_commit: 5e7aef0
git_status_at_claim: clean
created_at: 2026-05-23T22:00+08:00
last_heartbeat: 2026-05-23T22:00+08:00
expected_finish: 2026-05-24T12:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
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
  - docs/ops/**
  - deploy/**
read_scope:
  - medkernel-mvp/src/main/resources/application.yml
  - deploy/**
  - monitoring/**
  - scripts/**
forbidden_scope:
  - medkernel-mvp/src/main/java/**
  - frontend/src/**
  - medkernel-mvp/pom.xml

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
```

## Write Scope

```text
docs/ops/**
deploy/**
```

## Acceptance

```text
1. docs/ops/operations-manual.md — 运维手册（系统架构/部署/监控/日常运维）
2. docs/ops/backup-restore.md — 备份恢复手册（策略/步骤/验证/演练）
3. docs/ops/upgrade-rollback.md — 升级回滚手册（版本策略/步骤/回滚/验证）
4. docs/ops/incident-response.md — 故障应急手册（分级/响应/处置/复盘）
5. 所有手册面向医院信息科运维人员，可直接交付
```

## Verification

```text
ls docs/ops/*.md → ≥4 个文档
grep -r "备份" docs/ops/ → ≥3 匹配
grep -r "回滚" docs/ops/ → ≥3 匹配
grep -r "应急" docs/ops/ → ≥2 匹配
```
