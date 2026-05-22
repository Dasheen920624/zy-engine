# AI Task Claim

claim_id: GA-REL-01-S01
task_id: GA-REL-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
slice: S01
title: 发布与分支保护证据
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T22:00+08:00
last_heartbeat: 2026-05-23T22:00+08:00
expected_finish: 2026-05-24T06:00+08:00
heartbeat_interval_minutes: 60
database_mode: none
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
```

## Write Scope

```text
docs/release/**
deploy/scripts/verify-branch-protection.sh
.github/branch-protection.json
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-REL-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-REL-01.lock
```

## Read Scope

```text
.github/**
docs/**
deploy/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md
```

## Dependencies

```text
GA-GOV-01（已完成：并发机制硬门禁）
```

## Acceptance

```text
1. 分支保护规则文档（main/develop 保护策略）
2. 分支保护验证脚本
3. 发布流程文档（tag、release evidence、changelog）
4. Release evidence 模板
5. 所有文档和脚本可追溯
```

## Verification

```text
bash -n deploy/scripts/verify-branch-protection.sh
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 分支保护规则文档
- [ ] 分支保护验证脚本
- [ ] 发布流程文档
- [ ] Release evidence 模板
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
