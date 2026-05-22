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
git_base_commit: 8be0210
git_status_at_claim: clean
created_at: 2026-05-24T00:30+08:00
last_heartbeat: 2026-05-24T00:30+08:00
expected_finish: 2026-05-24T12:30+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/common/crypto/**
medkernel-mvp/src/main/java/com/medkernel/security/KeyManagementService.java
medkernel-mvp/src/main/java/com/medkernel/security/KeyRotationService.java
docs/engineering/SM_CRYPTO_SUITE.md
ai-dev-input/10_task_claims/active/GA-SEC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/common/crypto/**
medkernel-mvp/src/main/java/com/medkernel/security/**
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/persistence/**
```

## Acceptance

```text
1. SM2/SM3/SM4 国密套件配置完整
2. 密钥轮换机制可运行
3. 兼容模式文档（国密/国际双栈）齐备
4. 后端编译通过
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text`
- [ ] 创建 claim + lock 并 push
- [ ] 审查现有 SmCryptoService 和 KeyManagementService
- [ ] 补充密钥轮换兼容模式
- [ ] 编写国密套件文档
- [ ] 后端编译验证
- [ ] 更新台账
- [ ] commit + push
```
