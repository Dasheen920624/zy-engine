# AI Task Claim

claim_id: GRAPH-003-S01
task_id: GRAPH-003
slice: 图谱证据查询
title: GRAPH-003 图谱证据查询（返回证据 + 来源）
owner: AI-Codex-20260517
role: Backend AI
status: DONE
branch: main
database_mode: LOCAL_H2_FILE
oracle_available: false
review_required: false

## Write Scope

```text
zy-engine-mvp/src/main/java/com/zyengine/graph/GraphService.java
zy-engine-mvp/src/main/java/com/zyengine/graph/GraphController.java
zy-engine-mvp/src/test/java/com/zyengine/EngineApiContractTests.java
zy-engine-mvp/docs/02_任务台账.md
ai-dev-input/10_task_claims/active/GRAPH-003-S01.md
```

## Acceptance

```text
图谱证据查询 API 返回来源信息（reference_document_code / reference_binding_type）
图谱证据支持按来源文档过滤
新增契约测试
更新任务台账 GRAPH-003 → DONE
```
