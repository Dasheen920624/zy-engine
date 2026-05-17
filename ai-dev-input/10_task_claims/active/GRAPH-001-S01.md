# AI Task Claim

claim_id: GRAPH-001-S01
task_id: GRAPH-001
slice: 图谱版本生产库主数据
title: GRAPH-001 图谱版本生产库主数据（来源绑定 + 激活检查 + 测试）
owner: AI-Codex-20260517
role: Backend AI
status: ACTIVE
branch: main
git_base_commit: 7ebf11b
git_status_at_claim: clean, synced with origin/main
created_at: 2026-05-17 22:15:00 +08:00
last_heartbeat: 2026-05-17 22:15:00 +08:00
expected_finish: 2026-05-17 23:00:00 +08:00
database_mode: LOCAL_H2_FILE
oracle_available: false
review_required: false

## Write Scope

```text
zy-engine-mvp/src/main/java/com/zyengine/graph/GraphService.java
zy-engine-mvp/src/main/java/com/zyengine/graph/GraphController.java
zy-engine-mvp/src/test/java/com/zyengine/EngineApiContractTests.java
zy-engine-mvp/docs/02_任务台账.md
ai-dev-input/10_task_claims/active/GRAPH-001-S01.md
```

## Read Scope

```text
zy-engine-mvp/src/main/java/com/zyengine/graph/**
zy-engine-mvp/src/main/java/com/zyengine/provenance/SourceAssetBindingService.java
zy-engine-mvp/src/test/java/com/zyengine/EngineApiContractTests.java
```

## Forbidden Scope

```text
frontend/**
zy-engine-mvp/src/main/java/com/zyengine/rule/**
zy-engine-mvp/src/main/java/com/zyengine/pathway/**
zy-engine-mvp/src/main/java/com/zyengine/provenance/**
```

## Acceptance

```text
图谱版本支持 reference_document_code / reference_binding_type 字段
图谱版本激活时检查来源绑定（缺来源报 WARNING 不阻断）
图谱版本详情返回来源信息
新增契约测试
更新任务台账 GRAPH-001 → DONE
```
