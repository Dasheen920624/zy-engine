# AI Task Claim

claim_id: PR-FINAL-21-SONNET-20260522
task_id: PR-FINAL-21
task_lock_path: ai-dev-input/10_task_claims/active_locks/PR-FINAL-21.lock
slice: S01
title: E2E 6 剧本 fixture + Playwright
owner: Claude-Sonnet-4.6
role: 高级 AI
status: IN_PROGRESS
branch: develop
target_base_branch: develop
git_base_commit: 140efeb
git_status_at_claim: 无未提交改动
created_at: 2026-05-22T16:30+08:00
last_heartbeat: 2026-05-22T16:30+08:00
expected_finish: 2026-05-22T18:00+08:00
heartbeat_interval_minutes: 60
database_mode: LOCAL_H2_FILE
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: false
review_id: N/A_DIRECT_TO_DEVELOP
review_status: PENDING
reviewer: self-verify
open_findings: 0
quality_gate: PENDING
feature_acceptance_required: false
feature_acceptance_id: N/A
write_scope: frontend/e2e/scenarios/*.spec.ts (6 files); frontend/e2e/fixtures/test-fixtures.ts
read_scope: docs/v0.3-DEMO-REDESIGN.md; frontend/e2e/; frontend/src/router/routes.tsx
forbidden_scope: medkernel-mvp/src/**; frontend/src/**

## 实施范围

- 6 个演示剧本 E2E 测试（Playwright spec files）：
  - S1 CIO 看治理大盘 (dashboard + qc/dashboard + 下钻)
  - S2 医学专家发布 AMI 路径 (config/packages/import)
  - S3 医生在 HIS 收到推荐 (embed/order-safety + rule engine)
  - S4 CDSS 提醒疲劳治理 (cdss/fatigue)
  - S5 AI 知识审核闭环 (aik/sources + aik/review)
  - S6 多医院身份联邦 (security/identity-binding + tenant/onboarding)
