# AI Task Claim

claim_id: GA-DOC-02-S01
task_id: GA-DOC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
slice: S01
title: 运维与应急手册
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-DOC-02/ops-handbook
target_base_branch: develop
git_base_commit: f0935a9
git_status_at_claim: clean
created_at: 2026-05-23T23:30+08:00
last_heartbeat: 2026-05-23T23:30+08:00
expected_finish: 2026-05-24T11:30+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false

## Write Scope

```text
docs/ops/
ai-dev-input/10_task_claims/active/GA-DOC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DOC-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
medkernel-mvp/scripts/**
scripts/**
deploy/**
```

## Acceptance

```text
1. 运维手册齐备（部署、配置、监控、日志）
2. 备份恢复手册齐备（数据库、配置、知识包）
3. 升级回滚手册齐备（版本升级、数据迁移、回滚步骤）
4. 故障应急手册齐备（故障分级、应急流程、常见故障处理）
```

## Progress

```text
- [x] 创建 claim + lock 并 push
- [ ] 运维手册
- [ ] 备份恢复手册
- [ ] 升级回滚手册
- [ ] 故障应急手册
- [ ] 更新台账
- [ ] commit + push
```
