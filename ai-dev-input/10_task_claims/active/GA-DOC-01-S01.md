# AI Task Claim

claim_id: GA-DOC-01-S01
task_id: GA-DOC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
slice: S01
title: 用户手册
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T23:00+08:00
last_heartbeat: 2026-05-23T23:00+08:00
expected_finish: 2026-05-24T07:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
```

## Write Scope

```text
docs/user-guide/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-DOC-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-01.lock
```

## Read Scope

```text
frontend/src/pages/**
docs/**
medkernel-mvp/src/main/java/com/medkernel/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. 临床路径管理模块用户手册
2. 规则引擎模块用户手册
3. 术语服务模块用户手册
4. 知识图谱模块用户手册
5. 文档可交付医院信息科
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 临床路径管理模块手册
- [ ] 规则引擎模块手册
- [ ] 术语服务模块手册
- [ ] 知识图谱模块手册
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
