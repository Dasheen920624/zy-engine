# AI Task Claim

claim_id: GA-REFIT-01-S01
task_id: GA-REFIT-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REFIT-01.lock
slice: S01
title: 超长文件持续拆分（非 PR-FINAL-18 范围）
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: faa917b
git_status_at_claim: clean
created_at: 2026-05-23T21:30+08:00
last_heartbeat: 2026-05-23T21:30+08:00
expected_finish: 2026-05-24T06:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-REFIT-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/impl/**
medkernel-mvp/src/main/java/com/medkernel/notification/**
medkernel-mvp/src/main/java/com/medkernel/adapter/**
medkernel-mvp/src/main/java/com/medkernel/patient/MpiPersistenceService.java
medkernel-mvp/src/main/java/com/medkernel/patient/*Repository.java
medkernel-mvp/src/main/java/com/medkernel/dify/**
medkernel-mvp/src/main/java/com/medkernel/security/sso/**
ai-dev-input/10_task_claims/active/GA-REFIT-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-REFIT-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/com/medkernel/pathway/**
medkernel-mvp/src/main/java/com/medkernel/rule/**
medkernel-mvp/src/main/java/com/medkernel/knowledge/**
medkernel-mvp/src/main/java/com/medkernel/graph/**
medkernel-mvp/src/main/java/com/medkernel/security/SecurityPersistenceService.java
frontend/src/**
ai-dev-input/04_database/**
```

## Dependencies

```text
PR-FINAL-18 正在拆分 pathway/rule/knowledge/graph/SecurityPersistence，不可重叠
```

## Acceptance

```text
1. ImplementationService.java (1115行) 拆到可维护边界
2. NotificationRepository.java (1062行) 拆到可维护边界
3. AdapterHubService.java (978行) 拆到可维护边界
4. MpiPersistenceService.java (838行) 拆到可维护边界
5. DifyService.java (824行) 拆到可维护边界
6. SsoConfigService.java (815行) 拆到可维护边界
7. 所有拆分后文件 < 800 行
8. 后端编译通过
9. 保留现有 public facade/API
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 拆分 ImplementationService.java
- [ ] 拆分 NotificationRepository.java
- [ ] 拆分 AdapterHubService.java
- [ ] 拆分 MpiPersistenceService.java
- [ ] 拆分 DifyService.java
- [ ] 拆分 SsoConfigService.java
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
