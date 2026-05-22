# AI Task Claim

claim_id: GA-PERF-01-S01
task_id: GA-PERF-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-PERF-01.lock
slice: S01
title: 100 并发 30 min 压测报告，P95/P99/错误率可追溯
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-PERF-01/perf-baseline
target_base_branch: develop
git_base_commit: 3865a14
git_status_at_claim: clean
created_at: 2026-05-23T21:30+08:00
last_heartbeat: 2026-05-23T21:30+08:00
expected_finish: 2026-05-24T12:00+08:00
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
write_scope:
  - scripts/perf/**
  - docs/performance/**
read_scope:
  - medkernel-mvp/src/main/java/com/medkernel/**
  - frontend/src/**
  - deploy/**
  - monitoring/**
  - .github/workflows/**
forbidden_scope:
  - medkernel-mvp/src/main/java/com/medkernel/**
  - frontend/src/**
  - medkernel-mvp/pom.xml
  - medkernel-mvp/src/main/resources/application.yml

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-PERF-01.lock
```

## Write Scope

```text
scripts/perf/**
docs/performance/**
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
frontend/src/**
deploy/**
monitoring/**
.github/workflows/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
frontend/src/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/resources/application.yml
```

## Dependencies

```text
无外部依赖。k6 为独立工具，不修改业务代码。
```

## Acceptance

```text
1. scripts/perf/ 下有完整的 k6 压测脚本
2. 覆盖核心 API（规则评估、路径入径、CDSS、知识查询等）
3. 支持 100 并发 30 分钟持续压测
4. 有 P95/P99/错误率/吞吐量指标采集和报告模板
5. 有 CI 集成脚本可自动运行
6. docs/performance/ 下有基线报告和 SLO 定义
```

## Verification

```text
ls scripts/perf/*.js → ≥5 个测试脚本
ls docs/performance/ → ≥2 个文档
grep -r "p95\|p99\|error_rate" scripts/perf/ → ≥3 匹配
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: pending
tests_updated: N/A
samples_or_api_examples_updated: N/A
docs_updated: pending
db_only_checked: N/A
oracle_dm_h2_schema_synced: N/A
production_development_schema_synced: N/A
table_and_column_comments_complete: N/A
required_code_comments_complete: pending
feature_acceptance_created: N/A
claim_status_synced: pending
security_privacy_checked: true
```

## Progress

```text
- 创建 k6 压测脚本
- 创建性能基线报告模板
- 创建 SLO 定义文档
```
