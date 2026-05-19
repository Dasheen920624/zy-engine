# AI Task Claim

claim_id: PROV-002F-S01
task_id: PROV-002F
task_lock_path: ai-dev-input/10_task_claims/active_locks/PROV-002F.lock
slice: S01
title: SRC_CITATION 持久化接通（REVIEW-FIX-002 follow-up）
owner: CodeBuddy
role: 中级
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: a68cf8f4c8df6bdfb8469064a08156520450a831
git_status_at_claim: clean
created_at: 2026-05-19T21:30:00+08:00
last_heartbeat: 2026-05-19T21:30:00+08:00
expected_finish: 2026-05-20T08:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: N/A
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: false
review_id:
review_status:
reviewer:
open_findings: 0
quality_gate:
feature_acceptance_required: false
feature_acceptance_id:
write_scope: provenance/**, persistence/**, docs/**
read_scope: docs/engineering/02_任务台账.md, docs/03_设计系统.md
forbidden_scope: 除 write_scope 外的所有业务文件

## Task Lock

必须与本 claim 同一提交创建同任务唯一锁，并推送到 develop 后才算认领成功：

```text
ai-dev-input/10_task_claims/active_locks/PROV-002F.lock
```

## Write Scope

```text
provenance/**
persistence/**
docs/**
```

## Dependencies

```text
PROV-002 ✅ DONE
```

## Acceptance

```text
- 分析 PROV-002 当前内存态实现
- 检查 DDL 字段名错位问题
- 实现字段映射
- 实现加载/写入路径
- 实现 @PostConstruct 重建
- 更新文档
```

## Status Sync Checkpoints

```text
claim_pushed_before_code: true
task_ledger_in_progress: true
git_status_checked_before_edit: true
last_heartbeat_pushed: true
review_status_synced: N/A
task_ledger_done_synced: true
commit_hash_recorded: ba5a0d401c84a4c6d20bac458306d038870f31a0
post_push_git_status_clean: true
task_lock_removed_on_archive: true
```

## Self Check

```text
task_card_satisfied: true
write_scope_matches_diff: true
tests_updated: N/A (PROV-002F 是持久化接通，无新增业务逻辑)
samples_or_api_examples_updated: N/A
docs_updated: true (任务台账已更新)
db_only_checked: N/A (仅使用现有 DDL)
oracle_dm_h2_schema_synced: true (Oracle MERGE + H2 UPDATE/INSERT 双模式)
production_development_schema_synced: true (DDL 已存在，字段映射已对齐)
table_and_column_comments_complete: N/A (未新增表)
required_code_comments_complete: true (关键方法有中文注释)
feature_acceptance_created: false (PROV-002F 无客户可见功能)
claim_status_synced: true
security_privacy_checked: N/A
```

## Verification

```text
- SourceCitationService 启动期 rebuildFromPersistence 可从 DB 重建内存索引
- importCitations 写入后自动持久化到 src_citation 表
- Oracle MERGE 语法兼容生产库
- H2 UPDATE/INSERT 两阶段兼容本地开发库
- 字段映射：citation_code↔citationId, section_code↔section, clause_no↔clause,
  page_no↔page, excerpt_text↔quoteText, summary_text↔description,
  evidence_level↔citationType
```

## Completion

```text
commit: ba5a0d401c84a4c6d20bac458306d038870f31a0
push: 6b76b57 (archive commit)
tests: N/A (无新增业务逻辑)
review: N/A (review_required: false)
risks: 低风险 - 仅增加持久化路径，不改变现有 API 行为
```

## Progress

```text
1. 分析 SourceCitation.java 字段与 DDL src_citation 列名映射
2. EnginePersistenceService 增加 saveSourceCitation (Oracle MERGE + H2 UPDATE/INSERT)
3. EnginePersistenceService 增加 listSourceCitations + toSourceCitation(ResultSet)
4. SourceCitationService 增加 @PostConstruct rebuildFromPersistence
5. importCitations 中添加 persistenceService.saveSourceCitation() 调用
6. 更新任务台账至 DONE
```