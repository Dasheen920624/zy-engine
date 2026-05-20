# AI Task Claim

claim_id: REFIT-003-S01
task_id: REFIT-003
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-003.lock
slice: S01
title: 来源/审计/traceId/发布门禁统一改造
owner: CodeBuddy
role: 高级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 069410f
git_status_at_claim: clean
created_at: 2026-05-19T23:50:00+08:00
last_heartbeat: 2026-05-19T23:50:00+08:00
expected_finish: 2026-05-20T11:50:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-REFIT-003-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-REFIT-003-S01
write_scope: src/main/java/com/medkernel/config/**,src/main/java/com/medkernel/rule/**,src/main/java/com/medkernel/pathway/**,src/main/java/com/medkernel/graph/**,src/main/java/com/medkernel/dify/**,src/main/java/com/medkernel/audit/**
read_scope: src/main/java/com/medkernel/**, frontend/src/**, docs/**
forbidden_scope: 除 write_scope 外的所有业务代码文件

## Dependencies

REFIT-001 ✅ DONE
PROV-003 ✅ DONE
AUDIT-001 ✅ DONE

## Acceptance

1. 医学/医保/质控资产发布前统一来源检查
2. 运行结果和高风险操作可查证据、审计和 traceId

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
git diff --check
```
