# AI Task Claim

claim_id: GA-DOC-01-S01
task_id: GA-DOC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
slice: S01
title: 用户手册
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: ce6d35a
git_status_at_claim: clean
created_at: 2026-05-23T23:30+08:00
last_heartbeat: 2026-05-23T23:30+08:00
expected_finish: 2026-05-24T05:00+08:00
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

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
```

## Write Scope

```text
docs/user-guide/**
ai-dev-input/10_task_claims/active/GA-DOC-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
medkernel-mvp/src/main/java/com/medkernel/**
frontend/src/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/com/medkernel/persistence/**
ai-dev-input/04_database/**
```

## Dependencies

```text
无
```

## Acceptance

```text
1. 4 治理模块用户手册可交医院信息科
2. 覆盖：规则引擎、临床路径、知识管理、数据治理
3. 每个模块：功能概述、操作步骤、常见问题
4. 中文撰写
```

## Verification

```text
ls docs/user-guide/
```

## Progress

```text`
- [ ] 创建 claim + lock 并 push
- [ ] 创建规则引擎用户手册
- [ ] 创建临床路径用户手册
- [ ] 创建知识管理用户手册
- [ ] 创建数据治理用户手册
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
