# AI Task Claim

claim_id: GA-SEC-02-S01
task_id: GA-SEC-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
slice: S01
title: 国密套件与密钥轮换
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T23:45+08:00
last_heartbeat: 2026-05-23T23:45+08:00
expected_finish: 2026-05-24T07:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
```

## Write Scope

```text
docs/security/**
deploy/nginx/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-SEC-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-SEC-02.lock
```

## Read Scope

```text
deploy/**
docs/**
medkernel-mvp/src/main/resources/application.yml
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
ai-dev-input/10_task_claims/active/GA-*.md（其他 AI 的 claim）
```

## Dependencies

```text
GA-SEC-01（已完成：等保控制点自查）
```

## Acceptance

```text
1. 国密套件配置文档（Nginx TLS + 国密双证书）
2. 密钥轮换操作指南
3. 兼容模式文档（国密 + RSA 双栈）
4. Nginx 国密配置示例
5. 所有文档可交付审计
```

## Verification

```text
文档完整性检查
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 国密套件配置文档
- [ ] 密钥轮换操作指南
- [ ] 兼容模式文档
- [ ] Nginx 国密配置示例
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
