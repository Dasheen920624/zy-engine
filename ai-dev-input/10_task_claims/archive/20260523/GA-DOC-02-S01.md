# AI Task Claim

claim_id: GA-DOC-02-S01
task_id: GA-DOC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
slice: S01
title: 运维与应急手册
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 1e71ea4
git_status_at_claim: clean
created_at: 2026-05-24T01:00+08:00
last_heartbeat: 2026-05-24T01:00+08:00
expected_finish: 2026-05-24T13:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
```

## Write Scope

```text
docs/ops/**
deploy/**
ai-dev-input/10_task_claims/active/GA-DOC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
deploy/**
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
1. 运维手册齐备（部署/配置/监控/备份恢复）
2. 升级回滚手册齐备
3. 故障应急手册齐备
4. 文档可交付医院信息科
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 编写运维手册
- [ ] 编写升级回滚手册
- [ ] 编写故障应急手册
- [ ] 更新台账
- [ ] commit + push
```
