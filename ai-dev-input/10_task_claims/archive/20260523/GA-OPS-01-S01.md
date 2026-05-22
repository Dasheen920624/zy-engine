# AI Task Claim

claim_id: GA-OPS-01-S01
task_id: GA-OPS-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
slice: S01
title: 监控告警与 SLO
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: a96b12e
git_status_at_claim: clean
created_at: 2026-05-23T23:00+08:00
last_heartbeat: 2026-05-23T23:00+08:00
expected_finish: 2026-05-24T11:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/monitoring/**
medkernel-mvp/src/main/java/com/medkernel/common/monitoring/**
ai-dev-input/10_task_claims/active/GA-OPS-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/**
docs/AI_TEAM_PR_BACKLOG_V1.0_GA.md
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/resources/application.yml
```

## Acceptance

```text
1. /actuator/health 端点可用
2. /actuator/prometheus 指标可采集
3. 关键业务指标暴露（规则评估、路径实例、质控预警等）
4. SLO 文档齐备
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 实现 healthcheck 端点
- [ ] 暴露 Prometheus 指标
- [ ] 添加业务指标
- [ ] SLO 文档
- [ ] 后端编译验证
- [ ] 更新台账
- [ ] commit + push
```
