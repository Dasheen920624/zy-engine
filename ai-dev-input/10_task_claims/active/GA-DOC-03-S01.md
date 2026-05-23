# AI Task Claim

claim_id: GA-DOC-03-S01
task_id: GA-DOC-03
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-03.lock
slice: S01
title: 培训材料
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T23:15+08:00
last_heartbeat: 2026-05-23T23:15+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DOC-03.lock
```

## Write Scope

```text
docs/training/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-DOC-03-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-03.lock
```

## Read Scope

```text
docs/**
frontend/src/pages/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
GA-DOC-01（已完成：用户手册）
```

## Acceptance

```text
1. 医院 IT 培训大纲
2. 临床医生培训大纲
3. 实施工程师培训大纲
4. 培训考核要点
5. 材料可交付医院信息科
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 医院 IT 培训大纲
- [ ] 临床医生培训大纲
- [ ] 实施工程师培训大纲
- [ ] 培训考核要点
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
