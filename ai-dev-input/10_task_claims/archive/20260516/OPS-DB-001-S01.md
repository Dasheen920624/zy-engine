# AI Task Claim

claim_id: OPS-DB-001-S01
task_id: OPS-DB-001
slice: offline database provider for AI development
title: 支持无 Oracle 环境的本地 H2 文件数据库开发模式
owner: AI-Codex-20260516-db-provider-01
role: 架构/后端/工程 AI
status: DONE
branch: main
created_at: 2026-05-16T20:00:00+08:00
last_heartbeat: 2026-05-16T20:06:00+08:00
expected_finish: 2026-05-16T20:30:00+08:00
database_mode: LOCAL_H2
oracle_available: true
local_db_verified: true
oracle_verification_required: false

## Write Scope

```text
README.md
.gitignore
zy-engine-mvp/pom.xml
zy-engine-mvp/src/main/resources/application.yml
zy-engine-mvp/src/main/resources/db/local/h2_core_ddl.sql
zy-engine-mvp/src/main/java/com/zyengine/persistence/**
zy-engine-mvp/src/main/java/com/zyengine/system/HealthController.java
zy-engine-mvp/src/main/java/com/zyengine/pathway/PathwayService.java
zy-engine-mvp/src/test/java/com/zyengine/EngineApiContractTests.java
zy-engine-mvp/scripts/detect-db-env.*
zy-engine-mvp/scripts/start-local-db.*
zy-engine-mvp/scripts/README.md
zy-engine-mvp/docs/数据库Provider与离线AI开发约定.md
zy-engine-mvp/docs/AI接手执行手册.md
zy-engine-mvp/docs/产品化方案与AI开发编排.md
zy-engine-mvp/docs/顶级多角色评审与AI并行开发总控.md
ai-dev-input/04_database/local/h2_core_ddl.sql
ai-dev-input/README.md
ai-dev-input/09_ai_task_cards/task_card_template.md
ai-dev-input/10_task_claims/**
```

## Read Scope

```text
zy-engine-mvp/src/main/java/com/zyengine/**
zy-engine-mvp/scripts/**
zy-engine-mvp/docs/**
ai-dev-input/**
```

## Forbidden Scope

```text
Do not remove Oracle/DM DDL.
Do not make H2 the production default.
Do not weaken Oracle smoke requirements.
```

## Dependencies

```text
Existing Oracle persistence service and DB-only fallback mode.
```

## Acceptance

```text
No-Oracle AI can detect database environment.
No-Oracle AI can start LOCAL_H2_FILE on port 18082.
Local schema initializes automatically.
Current persistence writes work against local H2.
Oracle/DM DDL remains authoritative and present.
Docs require Oracle/DM/H2 structure synchronization for future DDL changes.
```

## Verification

```text
.\scripts\build.ps1
.\scripts\run-tests.ps1
.\scripts\detect-db-env.ps1 -BootstrapLocal
start-local-db.ps1 on port 18083 + run-rule-smoke.ps1 -BaseUrl http://localhost:18083/zy-engine/api
git diff --check
```

## Progress

```text
DONE: Added H2 runtime dependency and local schema initialization.
DONE: Added local H2 DDL to runtime resources and AI input package.
DONE: Added environment detection and local-db startup scripts.
DONE: Added provider status output for LOCAL_H2_FILE.
DONE: Added H2 contract tests and real local-db rule smoke verification.
DONE: Updated AI handoff, orchestration, task card, scripts README, and root README.
```

## Handoff

```text
Oracle remains production authority. If a future task changes schema, update Oracle, DM, and H2 DDL together.
For broader confidence, a future TEST task can add a one-command local-db smoke suite that runs all existing smoke scripts against port 18082.
```

## Completion

```text
commit: not created in this local turn
push: not pushed in this local turn
tests: build passed; JUnit passed; LOCAL_H2 provider startup passed; rule smoke passed; git diff --check passed
risks: Oracle smoke was not rerun for this local-provider change; existing Oracle code path remains separate and Oracle DDL files were preserved.
```
