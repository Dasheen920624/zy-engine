# AI Task Claim

claim_id: GA-DTO-03-S01
task_id: GA-DTO-03
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DTO-03.lock
slice: S01
title: 质控/CDSS Controller DTO 化
owner: TraeAI-5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 0291183
git_status_at_claim: clean
created_at: 2026-05-23T21:15+08:00
last_heartbeat: 2026-05-23T21:15+08:00
expected_finish: 2026-05-24T05:00+08:00
heartbeat_interval_minutes: 60
database_mode: local
oracle_available: false
local_db_verified: true
oracle_verification_required: false
review_required: true
review_id:
review_status: NOT_REQUESTED
reviewer:
open_findings: 0
quality_gate: BLOCKED_UNTIL_APPROVED
feature_acceptance_required: false
feature_acceptance_id:

## Task Lock

```text
ai-dev-input/10_task_claims/active_locks/GA-DTO-03.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/quality/QualityController.java
medkernel-mvp/src/main/java/com/medkernel/quality/QualityDashboardController.java
medkernel-mvp/src/main/java/com/medkernel/quality/EvalController.java
medkernel-mvp/src/main/java/com/medkernel/quality/EvalReportController.java
medkernel-mvp/src/main/java/com/medkernel/quality/AiSafetyController.java
medkernel-mvp/src/main/java/com/medkernel/quality/AcceptanceTestController.java
medkernel-mvp/src/main/java/com/medkernel/quality/dto/**
medkernel-mvp/src/main/java/com/medkernel/cdss/**Controller.java
medkernel-mvp/src/main/java/com/medkernel/cdss/dto/**
ai-dev-input/10_task_claims/active/GA-DTO-03-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DTO-03.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/quality/**
medkernel-mvp/src/main/java/com/medkernel/cdss/**
medkernel-mvp/src/main/java/com/medkernel/common/**
medkernel-mvp/src/main/java/com/medkernel/llm/**
docs/engineering/06_后端开发规范.md
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
ai-dev-input/04_database/**
medkernel-mvp/pom.xml
medkernel-mvp/src/main/java/com/medkernel/quality/AiGovernanceController.java
medkernel-mvp/src/main/java/com/medkernel/knowledge/**
medkernel-mvp/src/main/java/com/medkernel/adapter/**
```

## Dependencies

```text
无显式依赖（AiGovernanceController 由 GA-DTO-02 负责）
```

## Acceptance

```text
1. 新增 quality/dto/ 包，包含 Quality/Eval/EvalReport/AiSafety/AcceptanceTest Request DTO 类
2. 新增 cdss/dto/ 包，包含所有 CDSS Request DTO 类
3. 所有 @RequestBody Map<String, Object> 替换为类型化 DTO + @Valid
4. 所有 @RequestBody Map<String, String> 替换为类型化 DTO + @Valid
5. @RequestBody 实体类改用专用 Request DTO（quality 包内实体）
6. DTO 字段添加 javax.validation 约束注解
7. 后端编译通过
8. 无新增 raw Map Controller 入参
9. 审核类接口统一 ReviewRequest DTO
10. 涉及 11 个 Controller（6 quality + 5 cdss）
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 创建 quality/dto/ 包和 DTO 类
- [ ] 创建 cdss/dto/ 包和 DTO 类
- [ ] 重构 QualityController
- [ ] 重构 QualityDashboardController
- [ ] 重构 EvalController
- [ ] 重构 EvalReportController
- [ ] 重构 AiSafetyController
- [ ] 重构 AcceptanceTestController
- [ ] 重构 CdssController
- [ ] 重构 CdssOverrideController
- [ ] 重构 SafetyRedLineController
- [ ] 重构 ClinicalSafetyController
- [ ] 重构 AlertFatigueController
- [ ] 后端编译验证
- [ ] 更新台账
- [ ] commit + push
```

## Completion

```text
commit:
push:
tests:
review:
risks:
```
