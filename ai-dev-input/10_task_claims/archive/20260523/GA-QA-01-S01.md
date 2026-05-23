# AI Task Claim

claim_id: GA-QA-01-S01
task_id: GA-QA-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-01.lock
slice: S01
title: 后端覆盖率与 CI
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T23:30+08:00
last_heartbeat: 2026-05-23T23:30+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-QA-01.lock
```

## Write Scope

```text
medkernel-mvp/pom.xml
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-QA-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-QA-01.lock
```

## Read Scope

```text
medkernel-mvp/**
.github/workflows/ci.yml
docs/**
```

## Forbidden Scope

```text
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md（其他 AI 的 claim）
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. Jacoco 覆盖率阈值从 35% 提升到 70%
2. CI 中 Jacoco verify 步骤已存在
3. 覆盖率不足 70% 时构建失败
4. 排除列表合理（配置类/实体/DTO 等）
```

## Verification

```text
mvn -f medkernel-mvp/pom.xml verify
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 提升覆盖率阈值到 70%
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
