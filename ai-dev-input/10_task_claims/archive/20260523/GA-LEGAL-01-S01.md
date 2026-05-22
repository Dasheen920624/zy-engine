# AI Task Claim

claim_id: GA-LEGAL-01-S01
task_id: GA-LEGAL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
slice: S01
title: 合同/SLA/隐私政策/DPA
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 01bd286
git_status_at_claim: clean
created_at: 2026-05-24T02:00+08:00
last_heartbeat: 2026-05-24T02:00+08:00
expected_finish: 2026-05-24T14:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
```

## Write Scope

```text
docs/legal/**
ai-dev-input/10_task_claims/active/GA-LEGAL-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
medkernel-mvp/src/main/resources/application.yml
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
medkernel-mvp/pom.xml
```

## Acceptance

```text
1. 服务合同模板初稿齐备
2. SLA 协议初稿齐备
3. 隐私政策初稿齐备
4. DPA（数据处理协议）初稿齐备
5. 法务审核备注齐备
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 编写服务合同模板
- [ ] 编写 SLA 协议
- [ ] 编写隐私政策
- [ ] 编写 DPA
- [ ] 更新台账
- [ ] commit + push
```
