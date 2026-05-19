# AI Task Claim

claim_id: REFIT-002-S01
task_id: REFIT-002
task_lock_path: ai-dev-input/10_task_claims/active_locks/REFIT-002.lock
slice: S01
title: 租户/组织/身份贯通改造
owner: TraeAI-Main
role: 高级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: a0d2f46dd0de2ffd8d2c8d721799ba72e6ffbf52
git_status_at_claim: clean
created_at: 2026-05-20T00:00:00+08:00
last_heartbeat: 2026-05-20T00:00:00+08:00
expected_finish: 2026-05-20T12:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-REFIT-002-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-REFIT-002-S01
write_scope: medkernel-mvp/src/main/java/com/medkernel/**, frontend/src/api/client.ts
read_scope: docs/**, frontend/src/**, medkernel-mvp/src/**
forbidden_scope: 其他任务独占文件

## Dependencies

REFIT-001 (DONE), SEC-001 (DONE), ORG-003 (DONE)

## Acceptance

1. 所有业务 Controller 注入 OrganizationContextService
2. 列表接口按组织上下文过滤
3. 写入接口自动填充组织字段
4. 前端 API client 自动注入组织 Header
5. 契约测试通过

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
git diff --check
```
