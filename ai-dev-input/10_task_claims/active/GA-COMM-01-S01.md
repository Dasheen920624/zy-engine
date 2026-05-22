# AI Task Claim

claim_id: GA-COMM-01-S01
task_id: GA-COMM-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
slice: S01
title: License 与用量报告
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: f15428f
git_status_at_claim: clean
created_at: 2026-05-24T03:00+08:00
last_heartbeat: 2026-05-24T03:00+08:00
expected_finish: 2026-05-24T15:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/commercial/**
docs/legal/05_License与用量报告.md
ai-dev-input/10_task_claims/active/GA-COMM-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-COMM-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/security/**
medkernel-mvp/src/main/resources/application.yml
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
1. License 验证机制可运行
2. 用量报告 API 可用
3. 到期提醒闭环
4. 文档齐备
5. 后端编译通过
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 实现 License 验证服务
- [ ] 实现用量报告服务
- [ ] 实现到期提醒
- [ ] 编写文档
- [ ] 后端编译验证
- [ ] 更新台账
- [ ] commit + push
```
