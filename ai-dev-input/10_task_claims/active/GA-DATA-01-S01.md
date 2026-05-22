# AI Task Claim

claim_id: GA-DATA-01-S01
task_id: GA-DATA-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DATA-01.lock
slice: S01
title: 健康数据加密与脱敏
owner: TraeAI-Main
role: senior
status: ACTIVE
branch: ai/GA-DATA-01/health-data-encryption
target_base_branch: develop
git_base_commit: b1ecd06
git_status_at_claim: clean
created_at: 2026-05-23T23:00:00+08:00
last_heartbeat: 2026-05-23T23:00:00+08:00
expected_finish: 2026-05-24T07:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
write_scope: medkernel-mvp/src/main/java/com/medkernel/**, docs/security/djbp-2.0-level3-checklist.md, docs/engineering/SM_CRYPTO_SUITE.md
read_scope: docs/**, medkernel-mvp/src/main/**
forbidden_scope: medkernel-mvp/pom.xml, frontend/src/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DATA-01.lock
```

## Acceptance

```text
1. 所有 HEALTH_DATA Entity 的 Repository 集成 FieldEncryptionService 透明加解密
2. 最小化展示策略：按角色/权限决定脱敏级别
3. DataMaskingService 在 Controller 层自动脱敏
4. 数据治理文档更新
5. 等保合规检查清单更新
```

## Progress

```text
认领完成，开始开发
```
