# AI Task Claim

claim_id: ADAPT-001-S01
task_id: ADAPT-001
task_lock_path: ai-dev-input/10_task_claims/active_locks/ADAPT-001.lock
slice: S01
title: 适配器定义按组织绑定
owner: CodeBuddy
role: 中级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 9e203a22cef6b6ddcd35d8035f9ff863019fcd90
git_status_at_claim: clean
created_at: 2026-05-19T22:52:00+08:00
last_heartbeat: 2026-05-19T22:52:00+08:00
expected_finish: 2026-05-20T10:52:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-ADAPT-001-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-ADAPT-001-S01
write_scope: src/main/java/com/medkernel/adapter/**
read_scope: src/main/java/com/medkernel/**, frontend/src/**, docs/**
forbidden_scope: 除 write_scope 外的所有业务代码文件

## Dependencies

ORG-003 ✅ DONE

## Acceptance

1. 适配器定义按组织绑定
2. 不同组织不同适配器

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
git diff --check
```