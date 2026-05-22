# AI Task Claim

claim_id: GA-OPS-01-S01
task_id: GA-OPS-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
slice: S01
title: 监控告警与 SLO
owner: TraeAI-GLM5-1
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: HEAD
git_status_at_claim: clean
created_at: 2026-05-23T21:15+08:00
last_heartbeat: 2026-05-23T21:15+08:00
expected_finish: 2026-05-24T05:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
```

## Write Scope

```text
deploy/monitoring/**
deploy/docker-compose.monitoring.yml
docs/slo/**
docs/engineering/02_任务台账.md
ai-dev-input/10_task_claims/active/GA-OPS-01-S01.md
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
```

## Read Scope

```text
medkernel-mvp/src/main/resources/application.yml
medkernel-mvp/src/main/java/com/medkernel/system/**
deploy/**
docs/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**（不修改 Java 代码）
frontend/src/**
ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md
ai-dev-input/10_task_claims/active/GA-DTO-02-S01.md
ai-dev-input/10_task_claims/active/GA-DTO-03-S01.md
ai-dev-input/10_task_claims/active/GA-QA-03-S01.md
ai-dev-input/10_task_claims/active/GA-UX-01-S01.md
```

## Dependencies

```text
PR-FINAL-24（已完成：Actuator + Prometheus + Grafana 基础设施）
PR-FINAL-25（已完成：Flyway + 部署脚本）
```

## Acceptance

```text
1. Alertmanager 配置文件（路由、分组、通知渠道）
2. Docker Compose 编排文件（Prometheus + Grafana + Alertmanager）
3. SLO/SLI 正式定义文档（量化目标、error budget、合规证据）
4. 告警规则扩展（磁盘、CPU、业务指标）
5. 所有配置文件语法正确、可部署
```

## Verification

```text
# 语法验证
docker compose -f deploy/docker-compose.monitoring.yml config
# Prometheus 规则验证
promtool check rules deploy/monitoring/prometheus/medkernel-alert-rules.yml
```

## Self Check

```text
task_card_satisfied: pending
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
security_privacy_checked: N/A
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] Alertmanager 配置
- [ ] Docker Compose 编排
- [ ] SLO/SLI 定义文档
- [ ] 告警规则扩展
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
