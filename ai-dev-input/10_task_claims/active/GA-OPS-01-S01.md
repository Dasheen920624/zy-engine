# AI Task Claim

claim_id: GA-OPS-01-S01
task_id: GA-OPS-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
slice: S01
title: 运维监控与 SLO 证据
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-OPS-01/ops-monitoring
target_base_branch: develop
git_base_commit: fad1969
git_status_at_claim: clean
created_at: 2026-05-24T02:00:00+08:00
last_heartbeat: 2026-05-24T02:00:00+08:00
expected_finish: 2026-05-24T08:00:00+08:00
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
write_scope: monitoring/**, deploy/**
read_scope: docs/**, medkernel-mvp/**
forbidden_scope: medkernel-mvp/src/main/java/**, frontend/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
```

## Write Scope

```text
monitoring/**
deploy/**
```

## Read Scope

```text
docs/**
medkernel-mvp/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/**
```

## Dependencies

```text
无
```

## Acceptance

```text
1. Grafana 看板 JSON 导出文件
2. Prometheus 告警规则
3. healthcheck 端点验证
4. SLO 证据文档
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
