# AI Task Claim

claim_id: GA-PROD-01-S01
task_id: GA-PROD-01
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-PROD-01.lock
slice: S01
title: 全功能极简/完整/易用收口
owner: TraeAI-1
role: senior
status: ACTIVE
branch: ai/GA-PROD-01/product-simplification
target_base_branch: develop
git_base_commit: 0e7c3828c560ac03e63853b3875f0fa953916efd
git_status_at_claim: clean
created_at: 2026-05-24T00:00:00+08:00
last_heartbeat: 2026-05-24T00:00:00+08:00
expected_finish: 2026-05-24T12:00:00+08:00
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
write_scope: docs/PRODUCT_SIMPLIFICATION_V1_GA.md, docs/04_页面规格书.md, frontend/src/pages/{ConfigPackages,Pathway,Rule,Graph}/**
read_scope: docs/**, frontend/src/**
forbidden_scope: medkernel-mvp/src/main/java/**, medkernel-mvp/pom.xml, ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-PROD-01.lock
```

## Write Scope

```text
docs/PRODUCT_SIMPLIFICATION_V1_GA.md
docs/04_页面规格书.md
frontend/src/pages/ConfigPackages/**
frontend/src/pages/Pathway/**
frontend/src/pages/Rule/**
frontend/src/pages/Graph/**
```

## Read Scope

```text
docs/**
frontend/src/**
```

## Forbidden Scope

```text
medkernel-mvp/src/main/java/**
medkernel-mvp/pom.xml
ai-dev-input/10_task_claims/active/GA-GOV-02-S01.md
```

## Dependencies

```text
GA-GOV-02 (DONE)
```

## Acceptance

```text
1. 配置包、路径、规则、图谱等全部客户可见功能符合极简/完整/易用原则
2. 1 个主目标、1 个主按钮、最多 3 个默认筛选
3. 高级能力折叠
4. 主菜单保持左侧 SideMenu，不改顶部导航
```

## Progress

```text
认领完成，开始开发
```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
