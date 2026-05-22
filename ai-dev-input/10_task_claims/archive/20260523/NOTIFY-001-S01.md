# AI Task Claim

claim_id: NOTIFY-001-S01
task_id: NOTIFY-001
task_lock_path: ai-dev-input/10_task_claims/active_locks/NOTIFY-001.lock
slice: S01
title: 通知和消息中心
owner: TraeAI-Main
role: senior
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: 7a744abea894438856699a94f8a35bf7c0dce396
git_status_at_claim: clean
created_at: 2026-05-23T10:00+08:00
last_heartbeat: 2026-05-23T10:00+08:00
expected_finish: 2026-05-23T22:00+08:00
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
feature_acceptance_required: true
feature_acceptance_id:
write_scope:
read_scope:
forbidden_scope:

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/NOTIFY-001.lock
```

锁文件内容模板：

```text
task_id: NOTIFY-001
claim_id: NOTIFY-001-S01
owner: TraeAI-Main
branch: develop
git_base_commit: 7a744abea894438856699a94f8a35bf7c0dce396
created_at: 2026-05-23T10:00+08:00
last_heartbeat: 2026-05-23T10:00+08:00
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/notification/**
frontend/src/pages/Notification/**
frontend/src/api/notification.ts
ai-dev-input/04_database/local/notify_ddl.sql
ai-dev-input/04_database/pg/notify_ddl.sql
ai-dev-input/04_database/oracle/notify_ddl.sql
ai-dev-input/04_database/dm/notify_ddl.sql
ai-dev-input/13_feature_acceptance/FA-NOTIFY-001-S01.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/persistence/EnginePersistenceService.java
medkernel-mvp/src/main/java/com/medkernel/persistence/PersistenceRepositorySupport.java
medkernel-mvp/src/main/java/com/medkernel/workflow/**
medkernel-mvp/src/main/java/com/medkernel/audit/AuditController.java
medkernel-mvp/src/main/java/com/medkernel/organization/**
medkernel-mvp/src/main/java/com/medkernel/common/**
frontend/src/api/client.ts
frontend/src/router/**
docs/engineering/06_后端开发规范.md
docs/engineering/07_前端开发规范.md
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/com/medkernel/persistence/EnginePersistenceService.java
medkernel-mvp/src/main/java/com/medkernel/persistence/PersistenceRepositorySupport.java
frontend/src/router/routes.tsx
frontend/src/router/menuConfig.tsx
frontend/src/api/client.ts
```

## Dependencies

```text
WF-001: DONE - 工作流/待办模块已完成
SEC-001: DONE - 安全模块已完成
```

## Acceptance

```text
1. NotificationService 接入 EnginePersistenceService 持久化（非内存存储）
2. 新增 NotificationRepository 支持 CRUD + 过滤查询
3. 新增渠道配置（ChannelConfig）实体/Service/API
4. 新增通知模板（Template）实体/Service/API
5. 新增用户订阅设置（Subscription）实体/Service/API
6. 新增投递日志（DeliveryLog）实体/Service/API
7. 前端 NotificationSettings 页面接入后端 API
8. 通知与工作流联动（待办创建时自动发通知）
9. 后端编译通过 + 前端 typecheck/lint/build 通过
10. H2 DDL 与 PG/Oracle/DM 方言同步
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress:
git_status_checked_before_edit:
last_heartbeat_pushed:
review_status_synced:
task_ledger_done_synced:
commit_hash_recorded:
post_push_git_status_clean:
task_lock_removed_on_archive:
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
cd frontend && npx tsc --noEmit && npx eslint src/ --max-warnings 0 && npm run build
```

## Self Check

```text
task_card_satisfied:
write_scope_matches_diff:
tests_updated:
samples_or_api_examples_updated:
docs_updated:
db_only_checked:
oracle_dm_h2_schema_synced:
production_development_schema_synced:
table_and_column_comments_complete:
required_code_comments_complete:
feature_acceptance_created:
claim_status_synced:
security_privacy_checked:
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
- [ ] 创建 claim + lock 并 push
- [ ] NotificationService 接入持久化
- [ ] 新增 NotificationRepository
- [ ] 新增 ChannelConfig/Template/Subscription/DeliveryLog 实体
- [ ] 新增对应 Service 和 Controller 端点
- [ ] 前端 Settings 页面接入后端
- [ ] 工作流联动通知
- [ ] 后端编译 + 前端验证
- [ ] 创建 FA 验收记录
- [ ] verify-pr + commit + push
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
