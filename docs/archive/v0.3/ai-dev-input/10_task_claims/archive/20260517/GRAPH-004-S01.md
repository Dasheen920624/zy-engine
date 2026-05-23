# AI Task Claim

claim_id: GRAPH-004-S01
task_id: GRAPH-004
slice: 图谱版本回滚
title: GRAPH-004 图谱版本回滚（回滚到指定版本 + 测试）
owner: AI-Codex-20260517
role: Backend AI
status: DONE
branch: main
database_mode: LOCAL_H2_FILE
oracle_available: false
review_required: false

## Write Scope

```text
medkernel-mvp/src/main/java/com/medkernel/graph/GraphService.java
medkernel-mvp/src/main/java/com/medkernel/graph/GraphController.java
medkernel-mvp/src/test/java/com/medkernel/EngineApiContractTests.java
medkernel-mvp/docs/02_任务台账.md
ai-dev-input/10_task_claims/active/GRAPH-004-S01.md
```

## Acceptance

```text
图谱版本回滚 API（POST /api/graph/versions/{version}/rollback）
回滚后当前激活版本变为指定版本
新增契约测试
更新任务台账 GRAPH-004 → DONE
```
