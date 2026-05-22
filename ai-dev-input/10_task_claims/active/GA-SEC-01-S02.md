# AI Task Claim

claim_id: GA-SEC-01-S02
task_id: GA-SEC-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
slice: S02
title: 等保 2.0 三级控制点
owner: TraeAI-Main
role: senior
status: ACTIVE
branch: ai/GA-SEC-01/djbp-compliance
target_base_branch: develop
git_base_commit: b8f7294
git_status_at_claim: clean
created_at: 2026-05-23T22:00:00+08:00
last_heartbeat: 2026-05-23T22:00:00+08:00
expected_finish: 2026-05-24T06:00:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings:
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:
write_scope: docs/engineering/COMP-001_合规基线与证据包.md, medkernel-mvp/src/main/java/com/medkernel/security/**, deploy/scripts/security-baseline.sh (新建), deploy/scripts/security-baseline.ps1 (新建)
read_scope: docs/**, medkernel-mvp/src/main/**, deploy/**
forbidden_scope: medkernel-mvp/pom.xml, frontend/src/**

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-SEC-01.lock
```

## Write Scope

```text
docs/engineering/COMP-001_合规基线与证据包.md
medkernel-mvp/src/main/java/com/medkernel/security/**
deploy/scripts/security-baseline.sh (新建)
deploy/scripts/security-baseline.ps1 (新建)
```

## Acceptance

```text
1. COMP-001 文档更新：所有"待实现"控制点补充为可验证的自查闭环
2. 安全基线检查脚本：security-baseline.sh/ps1，验证等保控制点合规状态
3. 边界防护：Nginx 安全配置模板
4. 入侵防范：异常登录检测增强
5. 审计管理：审计管理员角色分离文档
6. 集中管控：安全事件监控与告警映射
7. CSRF 强制模式配置文档
8. 所有控制点有对应的代码/配置/文档证据
```

## Verification

```text
bash -n deploy/scripts/security-baseline.sh
pwsh -File deploy/scripts/security-baseline.ps1 -CheckOnly
git diff --check
```

## Progress

```text
认领完成，开始开发
```
