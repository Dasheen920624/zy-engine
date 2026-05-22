# AI Task Claim

claim_id: GA-DTO-01-S01
task_id: GA-DTO-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DTO-01.lock
slice: S01
title: Adapter Controller DTO 化
owner: TraeAI-5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 47b0ae7
git_status_at_claim: clean
created_at: 2026-05-23T19:40+08:00
last_heartbeat: 2026-05-23T19:40+08:00
expected_finish: 2026-05-23T23:00+08:00
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
read_scope:
forbidden_scope:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DTO-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/adapter/**Controller.java
medkernel-mvp/src/main/java/com/medkernel/adapter/dto/**
ai-dev-input/10_task_claims/active/GA-DTO-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DTO-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/adapter/**
medkernel-mvp/src/main/java/com/medkernel/common/**
docs/engineering/06_后端开发规范.md
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
ai-dev-input/04_database/**
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. 新增 adapter/dto/ 包，包含所有 Request DTO 类
2. 所有 @RequestBody Map<String, Object> 替换为类型化 DTO + @Valid
3. 所有 @RequestBody Object 替换为类型化 DTO + @Valid
4. @RequestBody CdssTriggerPointEntity 改为专用 Request DTO
5. DTO 字段添加 javax.validation 约束注解
6. 后端编译通过
7. 无新增 raw Map Controller 入参
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 创建 adapter/dto/ 包和所有 DTO 类
- [ ] 重构 TriggerPointController
- [ ] 重构 InteropController
- [ ] 重构 AdapterHubController
- [ ] 后端编译验证
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
```
