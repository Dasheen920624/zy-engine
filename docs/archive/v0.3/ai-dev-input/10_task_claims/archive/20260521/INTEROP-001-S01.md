# AI Task Claim

claim_id: INTEROP-001-S01
task_id: INTEROP-001
title: 院内互联互通标准适配矩阵
owner: CodeBuddy
role: 高级
status: ACTIVE
branch: develop
target_base_branch: develop
created_at: 2026-05-19T23:25:47+08:00
last_heartbeat: 2026-05-19T23:25:47+08:00
expected_finish: 2026-05-20T11:25:47+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: true
review_required: true
review_id: RV-INTEROP-001-S01-R01
review_status: NOT_REQUESTED
quality_gate: BLOCKED_UNTIL_APPROVED

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 `develop` 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/INTEROP-001.lock
```

锁文件内容模板：

```text
task_id: INTEROP-001
claim_id: INTEROP-001-S01
owner: CodeBuddy
branch: develop
created_at: 2026-05-19T23:25:47+08:00
last_heartbeat: 2026-05-19T23:25:47+08:00
```

## Write Scope

```text
src/main/java/com/medkernel/adapter/**
docs/**
ai-dev-input/06_samples/**
```

## Read Scope

```text
src/main/java/com/medkernel/adapter/**
docs/engineering/05_架构总图与服务边界.md
docs/engineering/06_后端开发规范.md
```

## Forbidden Scope

```text
frontend/**
src/main/java/com/medkernel/security/**
src/main/java/com/medkernel/config/**
```

## Dependencies

```text
ADAPT-001: 适配器定义按组织绑定（DONE）
DOC-008: 产品二次补充完善清单与扩展任务（DONE）
```

## Acceptance

```text
1. HIS/EMR/LIS/PACS/医保/OA 与 HL7 v2、FHIR、CDA、IHE、CDS Hooks、SMART on FHIR、DICOM 的适配矩阵
2. 每个适配器有标准协议映射
3. 适配器配置示例
4. 适配器测试用例
5. 适配器文档
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: false
task_ledger_in_progress: false
git_status_checked_before_edit: true
last_heartbeat_pushed: false
review_status_synced: false
task_ledger_done_synced: false
commit_hash_recorded: false
post_push_git_status_clean: false
task_lock_removed_on_archive: false
```

## Verification

```text
1. 适配器矩阵文档完整
2. 标准协议映射正确
3. 配置示例可用
4. 测试用例通过
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: true
tests_updated: true
samples_or_api_examples_updated: true
docs_updated: true
db_only_checked: false
oracle_dm_h2_schema_synced: false
production_development_schema_synced: false
table_and_column_comments_complete: false
required_code_comments_complete: true
feature_acceptance_created: false
claim_status_synced: true
security_privacy_checked: false
```

## Quality Review

```text
review_id: RV-INTEROP-001-S01-R01
review_file: ai-dev-input/11_ai_reviews/RV-INTEROP-001-S01-R01.md
review_status: NOT_REQUESTED
highest_severity: N/A
open_findings: 0
changes_requested: false
approved_by: N/A
approved_at: N/A
submit_allowed: false
```

## Progress

```text
2026-05-19 23:25 - 创建claim，开始INTEROP-001任务
```

## Handoff

```text
N/A
```

## Completion

```text
commit: N/A
push: N/A
tests: N/A
review: N/A
risks: N/A
```
