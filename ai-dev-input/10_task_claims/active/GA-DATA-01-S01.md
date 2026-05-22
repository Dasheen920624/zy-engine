# AI Task Claim

claim_id: GA-DATA-01-S01
task_id: GA-DATA-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DATA-01.lock
slice: S01
title: 健康数据加密与脱敏
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 69b46dd
git_status_at_claim: clean
created_at: 2026-05-23T23:00+08:00
last_heartbeat: 2026-05-23T23:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DATA-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/common/dataclass/**
medkernel-mvp/src/main/java/com/medkernel/datagovernance/**
medkernel-mvp/src/main/java/com/medkernel/patient/**
medkernel-mvp/src/main/resources/application.yml
ai-dev-input/10_task_claims/active/GA-DATA-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DATA-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
docs/**
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/src/main/java/com/medkernel/adapter/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
```

## Dependencies

```text
GA-SEC-02 (DONE) - 密钥轮换已实现
```

## Acceptance

```text
1. 所有 HEALTH_DATA 级别字段标注 @Encrypted + @DataClass(HEALTH_DATA)
2. PatientEntity 敏感字段加密脱敏完整
3. DataMaskingService 在 API 响应前自动脱敏
4. 最小化展示策略：非授权用户仅看到脱敏数据
5. 后端编译通过
```

## Verification

```text
grep -rn "@Encrypted" medkernel-mvp/src/main/java/com/medkernel/datagovernance/
grep -rn "HEALTH_DATA" medkernel-mvp/src/main/java/com/medkernel/
```

## Progress

```text`
- [ ] 创建 claim + lock 并 push
- [ ] 审查现有加密脱敏覆盖度
- [ ] 补充缺失的 @Encrypted 和 @DataClass 注解
- [ ] 确保最小化展示策略落地
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
