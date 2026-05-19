# AI Task Claim

claim_id: REFIT-004-S01
task_id: REFIT-004
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-004.lock
slice: S01
title: 多数据库持久化和中文注释统一补齐
owner: TraeAI-Main
role: 高级
status: DONE
branch: develop
target_base_branch: develop
git_base_commit: a35563f
git_status_at_claim: clean
created_at: 2026-05-20T01:00:00+08:00
last_heartbeat: 2026-05-20T01:00:00+08:00
expected_finish: 2026-05-20T12:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-REFIT-004-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-REFIT-004-S01
write_scope: ai-dev-input/04_database/**, medkernel-mvp/db/**, medkernel-mvp/src/main/resources/db/local/**, medkernel-mvp/src/main/java/com/medkernel/persistence/**
read_scope: docs/**, medkernel-mvp/src/**
forbidden_scope: 其他任务独占文件

## Dependencies

REFIT-001 (DONE), ARCH-004 (DONE)

## Acceptance

1. EnginePersistenceService Oracle MERGE 语句统一为 UPDATE+INSERT 两阶段（兼容 DM/PG/Kingbase）
2. resources/db/local/ 下 H2 DDL 补齐缺失表（sec、notify、wf 等）
3. loadLocalSchemaStatements() 注册所有 H2 DDL 文件
4. 四方言 DDL（Oracle/DM/PG/H2）表结构、索引、约束一致
5. 中文注释覆盖所有公开方法
6. build 通过

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
git diff --check
```
