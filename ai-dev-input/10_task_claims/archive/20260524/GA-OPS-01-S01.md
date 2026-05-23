# AI Task Claim

claim_id: GA-OPS-01-S01
task_id: GA-OPS-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
slice: S01
title: Grafana 看板、告警规则、healthcheck、SLO 证据齐备
owner: TraeAI-2
role: architect
status: ACTIVE
branch: ai/GA-OPS-01/monitoring
target_base_branch: develop
git_base_commit: fad1969
git_status_at_claim: clean
created_at: 2026-05-23T23:45+08:00
last_heartbeat: 2026-05-23T23:45+08:00
expected_finish: 2026-05-24T12:00+08:00
heartbeat_interval_minutes: 60
write_scope:
  - deploy/monitoring/**
  - deploy/scripts/healthcheck.sh
  - deploy/docker-compose.monitoring.yml
  - docs/slo/**
read_scope:
  - medkernel-mvp/src/main/resources/application.yml
  - medkernel-mvp/src/main/java/com/medkernel/**
  - monitoring/**
forbidden_scope:
  - medkernel-mvp/src/main/java/**
  - frontend/src/**

## Acceptance

```text
1. Grafana 6 套看板 JSON 配置完整且可导入
2. Prometheus 告警规则覆盖 4 大类 15+ 条
3. AlertManager 路由和通知配置完整
4. healthcheck.sh 脚本覆盖所有关键端点
5. SLO 证据文档齐备（P95/P99/错误率/可用性）
6. docker-compose.monitoring.yml 可一键启动监控栈
7. 所有配置与 application.yml actuator 端点对齐
```
