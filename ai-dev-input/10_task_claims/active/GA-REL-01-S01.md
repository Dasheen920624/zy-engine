# AI Task Claim

claim_id: GA-REL-01-S01
task_id: GA-REL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
slice: S01
title: 发布与分支保护证据
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: ai/GA-REL-01/release-protection
target_base_branch: develop
git_base_commit: 8573aea
git_status_at_claim: clean
created_at: 2026-05-23T23:00+08:00
last_heartbeat: 2026-05-23T23:00+08:00
expected_finish: 2026-05-24T11:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false

## Write Scope

```text
.github/workflows/ci.yml
.github/branch-protection.json
scripts/release.ps1
scripts/verify-release.ps1
docs/engineering/release-evidence.md
ai-dev-input/10_task_claims/active/GA-REL-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
.github/**
scripts/**
```

## Acceptance

```text
1. main/develop 分支保护规则已配置或文档化
2. release evidence 流程可校验（tag + changelog + CI 绿）
3. tag 流程可校验（语义化版本 + 签名）
4. 发布脚本可执行
```

## Progress

```text
- [x] 创建 claim + lock 并 push
- [ ] 配置分支保护规则
- [ ] 创建 release evidence 流程
- [ ] 创建 tag 流程脚本
- [ ] 更新台账
- [ ] commit + push
```
