# AI Task Claim

claim_id: GA-SEC-02-S01
task_id: GA-SEC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
slice: S01
title: 国密套件与密钥轮换
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: c5d05b5
git_status_at_claim: clean
created_at: 2026-05-23T22:15+08:00
last_heartbeat: 2026-05-23T22:15+08:00
expected_finish: 2026-05-24T04:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/common/crypto/**
medkernel-mvp/src/main/java/com/medkernel/common/dataclass/**
medkernel-mvp/src/main/resources/application.yml
docs/engineering/国密套件与密钥轮换.md
ai-dev-input/10_task_claims/active/GA-SEC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/common/**
medkernel-mvp/src/main/resources/**
docs/**
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
ai-dev-input/04_database/**
```

## Dependencies

```text
GA-GOV-01 (DONE)
```

## Acceptance

```text
1. SmCryptoService 支持密钥轮换（双密钥并行解密）
2. FieldEncryptionService 支持密钥版本标记
3. application.yml 国密套件配置完整（SM2/SM3/SM4/HSM/兼容模式）
4. 国密套件与密钥轮换文档齐备
5. 后端编译通过
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text`
- [ ] 创建 claim + lock 并 push
- [ ] 实现密钥轮换机制（SmKeyRotationService）
- [ ] 更新 FieldEncryptionService 支持密钥版本
- [ ] 更新 application.yml 国密套件配置
- [ ] 创建国密套件与密钥轮换文档
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
