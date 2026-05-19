# AI Task Claim

claim_id: PROV-003F-S01
task_id: PROV-003F
task_lock_path: ai-dev-input/10_task_claims/active_locks/PROV-003F.lock
slice: S01
title: SRC_ASSET_BINDING 持久化接通（REVIEW-FIX-002 follow-up）
owner: CodeBuddy
role: 中级
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: 8a91ee1
git_status_at_claim: clean
created_at: 2026-05-19T21:08:00+08:00
last_heartbeat: 2026-05-19T21:08:00+08:00
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
ai-dev-input/10_task_claims/active_locks/PROV-003F.lock
```

## Write Scope

```text
provenance/**
persistence/**
docs/**
```

## Dependencies

```text
PROV-003 ✅ DONE
```

## Acceptance

```text
- 分析 PROV-003 当前内存态实现
- 检查 DDL src_asset_binding 字段
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
commit_hash_recorded: true
post_push_git_status_clean: true
task_lock_removed_on_archive: true
```

## Progress

```text
1. 分析 DDL src_asset_binding 字段，建立与 SourceAssetBinding Java 模型的字段映射
2. EnginePersistenceService 新增 saveSourceAssetBinding + listSourceAssetBindings（Oracle MERGE + H2 UPDATE/INSERT 双模式）
3. SourceAssetBindingService 新增 @PostConstruct rebuildFromPersistence() 从 DB 重建内存索引
4. importBindings() 中加入 persistenceService.saveSourceAssetBinding(binding) 持久化调用
5. 字段映射：assetType↔asset_type, assetCode↔asset_code, citationId↔citation_code, bindingType↔binding_role
6. DDL 缺失列（documentCode/confidence/description/updatedTime）重建后为 null，需后续 DDL 变更补充
```

## Self Check

```text
- [x] saveSourceAssetBinding: Oracle MERGE ON (tenant_id,asset_type,asset_code,asset_version,citation_code,binding_role) + H2 UPDATE/INSERT 双模式
- [x] listSourceAssetBindings: SELECT 全量加载用于 @PostConstruct 重建
- [x] toSourceAssetBinding: ResultSet → Java 模型映射，bindingId 由 "BIND_" + id 重建
- [x] @PostConstruct rebuildFromPersistence: 从 DB 重建 bindingStore，putIfAbsent 避免覆盖
- [x] importBindings: 存入内存后调用 persistenceService.saveSourceAssetBinding
- [x] 字段映射与 PROV-002F 模式一致
- [x] 无 linter 错误
```

## Verification

```text
- 代码编译通过（无 linter 报错）
- Oracle MERGE 使用 DDL 唯一键 (tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)
- H2 使用 UPDATE/INSERT 两阶段，与 PROV-002F saveSourceCitationLocal 模式一致
- 重建路径：listSourceAssetBindings() → toSourceAssetBinding() → putIfAbsent()
- 已知限制：DDL 缺失 documentCode/confidence/description/updatedTime 列，重建后为 null
```

## Completion

```text
完成时间: 2026-05-19T21:15:00+08:00
commit: 62c97d4
核心改动: EnginePersistenceService + SourceAssetBindingService 持久化接通
遗留风险: DDL 需补充 documentCode/confidence/description/updated_time 列以完整持久化
```
