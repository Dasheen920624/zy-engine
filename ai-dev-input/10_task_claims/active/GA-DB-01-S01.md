# AI Task Claim

claim_id: GA-DB-01-S01
task_id: GA-DB-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DB-01.lock
slice: S01
title: 四方言 smoke 与 Flyway rollback
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T21:30+08:00
last_heartbeat: 2026-05-23T21:30+08:00
expected_finish: 2026-05-24T05:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DB-01.lock
```

## Write Scope

```text
deploy/profiles/kingbase-x86_64.env
deploy/scripts/smoke-ddl-consistency.sh
deploy/scripts/smoke-ddl-consistency.ps1
deploy/scripts/lib/common.sh
medkernel-mvp/src/main/resources/db/migration/common/.gitkeep
docs/engineering/smoke-plan-ddl-consistency.md
docs/engineering/flyway-rollback-guide.md
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-DB-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DB-01.lock
```

## Read Scope

```text
medkernel-mvp/src/main/resources/db/**
medkernel-mvp/src/main/resources/application.yml
deploy/**
docs/**
ai-dev-input/04_database/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
PR-FINAL-25（已完成：Flyway + 部署脚本）
PR-FINAL-23（已完成：加密字段列宽扩展）
```

## Acceptance

```text
1. db/migration/common/ 目录创建
2. KingbaseES 部署 profile
3. check_db_kingbase() 函数
4. 可执行 DDL 一致性冒烟脚本（4 方言）
5. Flyway rollback 操作指南
6. 所有脚本语法正确
```

## Verification

```text
bash -n deploy/scripts/smoke-ddl-consistency.sh
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] db/migration/common/ 目录
- [ ] KingbaseES profile
- [ ] check_db_kingbase() 函数
- [ ] DDL 一致性冒烟脚本
- [ ] Flyway rollback 指南
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
