# AI Task Claim

claim_id: GA-DTO-02-S01
task_id: GA-DTO-02
task_lock_path: ai-dev-input/10_task_claims/active_locks/GA-DTO-02.lock
slice: S01
title: Knowledge / AI review Controller DTO 化
owner: TraeAI-GLM5
role: senior
status: ACTIVE
branch: develop
target_base_branch: develop
git_base_commit: 47b0ae7
git_status_at_claim: clean
created_at: 2026-05-23T21:00+08:00
last_heartbeat: 2026-05-23T21:00+08:00
expected_finish: 2026-05-24T03:00+08:00
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
ai-dev-input/10_task_claims/active_locks/GA-DTO-02.lock
```

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/knowledge/*Controller.java
medkernel-mvp/src/main/java/com/medkernel/knowledge/dto/**
medkernel-mvp/src/main/java/com/medkernel/quality/AiGovernanceController.java
medkernel-mvp/src/main/java/com/medkernel/quality/dto/**
ai-dev-input/10_task_claims/active/GA-DTO-02-S01.md
ai-dev-input/10_task_claims/active_locks/GA-DTO-02.lock
docs/engineering/02_任务台账.md
```

## Read Scope

```text
medkernel-mvp/src/main/java/com/medkernel/knowledge/**
medkernel-mvp/src/main/java/com/medkernel/quality/**
medkernel-mvp/src/main/java/com/medkernel/common/**
docs/engineering/06_后端开发规范.md
```

## Forbidden Scope

```text
frontend/src/**
medkernel-mvp/src/main/java/com/medkernel/persistence/**
ai-dev-input/04_database/**
medkernel-mvp/src/main/java/com/medkernel/adapter/**
```

## Dependencies

```text
无显式依赖
```

## Acceptance

```text
1. 新增 knowledge/dto/ 包，包含所有 Request DTO 类
2. 新增 quality/dto/ 包，包含 AiGovernance Request DTO 类
3. 所有 @RequestBody Map<String, Object> 替换为类型化 DTO + @Valid
4. 所有 @RequestBody Map<String, String> 替换为类型化 DTO + @Valid
5. DTO 字段添加 javax.validation 约束注解
6. 后端编译通过
7. 无新增 raw Map Controller 入参
8. 涉及 7 个 Controller、18 个方法
```

## Verification

```text
mvn compile -pl medkernel-mvp -q
```

## Progress

```text
- [ ] 创建 claim + lock 并 push
- [ ] 创建 knowledge/dto/ 包和所有 DTO 类
- [ ] 重构 KnowledgeController
- [ ] 重构 KnowledgeSyncController
- [ ] 重构 AssetQualityController
- [ ] 重构 AiCandidateReviewController
- [ ] 重构 KnowledgePackageController
- [ ] 重构 AiKnowledgeJobController
- [ ] 重构 AiGovernanceController
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
