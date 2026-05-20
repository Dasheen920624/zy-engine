# AI Task Claim

claim_id: NOTIFY-001-S01
task_id: NOTIFY-001
task_lock_path: ai-dev-input/10_task_claims/active_locks/NOTIFY-001.lock
slice: S01
title: 通知和消息中心
owner: CodeBuddy
role: 中级
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 54a8a59
git_status_at_claim: clean
created_at: 2026-05-20T22:30:00+08:00
last_heartbeat: 2026-05-20T22:30:00+08:00
expected_finish: 2026-05-21T10:30:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: false
oracle_verification_required: false
review_required: true
review_id: RV-NOTIFY-001-S01-R01
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: true
feature_acceptance_id: FA-NOTIFY-001-S01
write_scope: notification/**, frontend/src/pages/notification/**, ai-dev-input/04_database/**/*notify*, ai-dev-input/04_database/**/*notification*
read_scope: workflow/**, frontend/src/**, medkernel-mvp/src/**
forbidden_scope: 除 write_scope 外的所有业务代码文件

## Dependencies

WF-001 ✅ DONE
SEC-001 ✅ DONE

## Acceptance

1. 站内信通知列表，按时间倒序
2. 支持已读/未读状态标记
3. 邮件/短信/企微适配器接口
4. 通知模板管理
5. 订阅设置（按事件类型）
6. 超时提醒
7. 失败重试机制

## Verification

```powershell
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/build.ps1
cd medkernel-mvp && powershell -ExecutionPolicy Bypass -File scripts/run-tests.ps1
git diff --check
```