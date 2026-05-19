# AI Task Claim

claim_id: REFIT-004-S01
task_id: REFIT-004
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-004.lock
slice: S01
title: 多数据库持久化和中文注释统一补齐
owner: CodeBuddy
role: 高级
status: ACTIVE
branch: REFIT-004
target_base_branch: develop
git_base_commit: 12d9f93
git_status_at_claim: clean
created_at: 2026-05-20T00:05:00+08:00
last_heartbeat: 2026-05-20T00:05:00+08:00
expected_finish: 2026-05-20T12:05:00+08:00
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
write_scope: ai-dev-input/04_database/**,medkernel-mvp/db/**,medkernel-mvp/src/main/resources/db/local/**
read_scope: src/main/java/com/medkernel/**, docs/**
forbidden_scope: 除 write_scope 外的所有业务代码文件

## Dependencies

REFIT-001 ✅ DONE
ARCH-004 ✅ DONE

## Acceptance

1. 已实现能力的 Oracle/DM/PG/LOCAL_H2_FILE 表结构、索引、约束一致
2. 所有表和关键列有中文注释（COMMENT ON TABLE / COLUMN）
3. LOCAL_H2_FILE DDL 可成功执行建表
4. 生产库 smoke 计划（每方言一句 SELECT COUNT(*) 确认表存在）

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
git diff --check
```
