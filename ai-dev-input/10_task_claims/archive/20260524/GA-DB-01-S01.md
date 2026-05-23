# AI Task Claim

claim_id: GA-DB-01-S01
task_id: GA-DB-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DB-01.lock
slice: S01
title: 多方言 smoke 矩阵和 Flyway rollback 证据
owner: TraeAI-1
role: senior
status: DONE
branch: ai/GA-DB-01/multi-dialect-smoke
target_base_branch: develop
git_base_commit: ea0cbcb
git_status_at_claim: clean
created_at: 2026-05-24T01:00:00+08:00
last_heartbeat: 2026-05-24T01:00:00+08:00
expected_finish: 2026-05-24T07:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope: ai-dev-input/04_database/**, medkernel-mvp/src/main/resources/db/**
read_scope: docs/**, medkernel-mvp/**
forbidden_scope: medkernel-mvp/src/main/java/com/medkernel/security/**, medkernel-mvp/src/main/java/com/medkernel/common/crypto/**, frontend/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DB-01.lock
```

## Write Scope

```text
ai-dev-input/04_database/**
medkernel-mvp/src/main/resources/db/**
```

## Read Scope

```text
docs/**
medkernel-mvp/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/com/medkernel/security/**
medkernel-mvp/src/main/java/com/medkernel/common/crypto/**
frontend/**
```

## Dependencies

```text
无
```

## Acceptance

```text
1. 4 方言（Oracle、达梦、PostgreSQL、KingbaseES）smoke 矩阵齐备
2. Flyway migration 可升级可回滚证据齐备
3. 每种方言有独立的 smoke 测试脚本
4. 矩阵结果文档化
```

## Progress

```text
认领完成，开始开发
```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
