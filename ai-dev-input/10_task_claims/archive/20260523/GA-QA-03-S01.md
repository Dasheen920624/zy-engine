# AI Task Claim

claim_id: GA-QA-03-S01
task_id: GA-QA-03
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-QA-03.lock
slice: S01
title: 6 大剧本 E2E
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-QA-03/e2e-scenarios
target_base_branch: develop
git_base_commit: 8a9602cd89c43b917e333351d4701ee01f039c07
git_status_at_claim: clean
created_at: 2026-05-23T23:00:00+08:00
last_heartbeat: 2026-05-23T23:00:00+08:00
expected_finish: 2026-05-24T05:00:00+08:00
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
write_scope: frontend/e2e/**, ai-dev-input/06_samples/scenarios/**
read_scope: docs/**, frontend/src/**
forbidden_scope: medkernel-mvp/src/main/java/**, medkernel-mvp/pom.xml

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-QA-03.lock
```

## Write Scope

```text
frontend/e2e/**
ai-dev-input/06_samples/scenarios/**
```

## Read Scope

```text
docs/**
frontend/src/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
medkernel-mvp/pom.xml
```

## Dependencies

```text
无
```

## Acceptance

```text
1. 6 大客户剧本 E2E 测试用例编写
2. E2E 测试稳定 PASS
3. fixture 数据可一键加载
4. CI 集成 E2E 测试
```

## Verification

```text
npx playwright test --project=chromium
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
