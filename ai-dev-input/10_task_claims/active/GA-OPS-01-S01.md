# AI Task Claim

claim_id: GA-OPS-01-S01
task_id: GA-OPS-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
slice: S01
title: 监控告警与 SLO
owner: TraeAI-Main
role: senior
status: ACTIVE
branch: ai/GA-OPS-01/monitoring-slo
target_base_branch: develop
git_base_commit: aeeab95
git_status_at_claim: clean
created_at: 2026-05-23T21:00:00+08:00
last_heartbeat: 2026-05-23T21:00:00+08:00
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
write_scope: deploy/monitoring/**, deploy/scripts/healthcheck.*, docs/engineering/09_内网部署与版本管理.md
read_scope: docs/**, medkernel-mvp/src/main/**, deploy/**
forbidden_scope: medkernel-mvp/src/main/java/**, frontend/src/**, medkernel-mvp/pom.xml

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/GA-OPS-01.lock
```

## Write Scope

```text
deploy/monitoring/prometheus/medkernel-alert-rules.yml
deploy/monitoring/prometheus/prometheus-medkernel.yml
deploy/monitoring/grafana/dashboards/medkernel-slo.json (新建)
deploy/monitoring/grafana/dashboards/medkernel-security.json (新建)
deploy/monitoring/README.md
deploy/scripts/healthcheck.sh (增强)
deploy/scripts/healthcheck.ps1 (增强)
docs/engineering/09_内网部署与版本管理.md (SLO 章节)
```

## Read Scope

```text
docs/**
medkernel-mvp/src/main/**
deploy/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
frontend/src/**
medkernel-mvp/pom.xml
```

## Dependencies

```text
无（PR-FINAL-24 已提供基础监控设施）
```

## Acceptance

```text
1. SLO 定义文档（可用性、延迟、错误率、饱和度）
2. Prometheus 告警规则补充（业务层、数据库、安全、SLO burn rate）
3. Grafana SLO 看板和安全管理看板
4. healthcheck 脚本增强（检查 actuator/health、Prometheus 指标、SLO 状态）
5. 监控 README 更新（含 SLO 定义、告警分级、运维手册引用）
6. 内网部署文档更新 SLO 章节
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: pending
git_status_checked_before_edit: true
last_heartbeat_pushed: pending
review_status_synced: pending
task_ledger_done_synced: pending
commit_hash_recorded: pending
post_push_git_status_clean: pending
task_lock_removed_on_archive: pending
```

## Verification

```text
promtool check rules deploy/monitoring/prometheus/medkernel-alert-rules.yml
promtool check config deploy/monitoring/prometheus/prometheus-medkernel.yml
git diff --check
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

## Quality Review

```text
review_id:
review_file:
review_status:
highest_severity:
open_findings:
changes_requested:
approved_by:
approved_at:
submit_allowed:
```

## Progress

```text
认领完成，开始开发
```

## Handoff

```text

```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
