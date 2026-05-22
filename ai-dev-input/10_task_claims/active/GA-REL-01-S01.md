# AI Task Claim

claim_id: GA-REL-01-S01
task_id: GA-REL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
slice: S01
title: 发布与分支保护证据
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: f8df93a
git_status_at_claim: clean
created_at: 2026-05-23T22:00+08:00
last_heartbeat: 2026-05-23T22:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
```

## Write Scope

```text
.github/workflows/**
scripts/**
VERSIONING.md
docs/engineering/分支策略与发布管理.md
ai-dev-input/10_task_claims/active/GA-REL-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
docs/**
scripts/**
.github/**
VERSIONING.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/04_database/**
```

## Dependencies

```text
GA-GOV-01 (DONE)
```

## Acceptance

```text
1. GitHub Actions CI workflow 配置完成（develop push 触发）
2. main/develop 分支保护规则文档化
3. release evidence 流程文档化（tag + changelog）
4. verify-pr 脚本存在且可执行
5. VERSIONING.md 更新
```

## Verification

```text
ls -la .github/workflows/ && ls -la scripts/verify-pr*
```

## Progress

```text`
- [ ] 创建 claim + lock 并 push
- [ ] 创建 GitHub Actions CI workflow
- [ ] 更新分支保护文档
- [ ] 更新 VERSIONING.md
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
