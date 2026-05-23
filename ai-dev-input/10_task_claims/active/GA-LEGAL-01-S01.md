# AI Task Claim

claim_id: GA-LEGAL-01-S01
task_id: GA-LEGAL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-LEGAL-01.lock
slice: S01
title: 合同/SLA/隐私政策/DPA
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-LEGAL-01/legal-docs
target_base_branch: develop
git_base_commit: 0651274
git_status_at_claim: clean
created_at: 2026-05-24T00:00+08:00
last_heartbeat: 2026-05-24T00:00+08:00
expected_finish: 2026-05-24T12:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false

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
```

## Acceptance

```text
1. 软件许可协议初稿齐备
2. SLA 服务等级协议初稿齐备
3. 隐私政策初稿齐备
4. DPA 数据处理协议初稿齐备
5. 所有文档标注"初稿，需法务审核"
```

## Progress

```text
- [x] 创建 claim + lock 并 push
- [ ] 软件许可协议
- [ ] SLA 服务等级协议
- [ ] 隐私政策
- [ ] DPA 数据处理协议
- [ ] 更新台账
- [ ] commit + push
```
