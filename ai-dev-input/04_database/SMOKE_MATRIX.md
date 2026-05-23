# 多方言 Smoke 矩阵

> 版本：1.0 · 2026-05-24
> 任务：GA-DB-01 多方言 smoke 矩阵和 Flyway rollback 证据

## 1. 测试矩阵

| 检查项 | Oracle | DM8+ | PostgreSQL | KingbaseES | H2 |
|---|---|---|---|---|---|
| DDL 文件存在 | ✅ 23 个 | ✅ 23 个 | ✅ 22 个 | ✅ (PG) | ✅ 9 个 |
| 核心 DDL (core_ddl) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Flyway migration 目录 | ✅ | ✅ | ✅ | ✅ | ✅ |
| V1 baseline | ✅ | ✅ | ✅ | ✅ | ✅ |
| V2 列宽扩展 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 方言语法兼容 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 无 MySQL 专有语法 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 应用启动 (H2) | N/A | N/A | N/A | N/A | ✅ |
| Flyway rollback 文档 | ✅ | ✅ | ✅ | ✅ | ✅ |

## 2. 方言映射

| 方言 | Flyway locations | DDL 来源 | JDBC 前缀 |
|---|---|---|---|
| Oracle | `db/migration/{common,oracle}` | `04_database/oracle/` | `jdbc:oracle:thin:` |
| DM8+ | `db/migration/{common,dm}` | `04_database/dm/` | `jdbc:dm:` |
| PostgreSQL | `db/migration/{common,postgres}` | `04_database/postgres/` | `jdbc:postgresql:` |
| KingbaseES | `db/migration/{common,kingbase}` | `04_database/postgres/` | `jdbc:kingbase8:` |
| H2 | `db/migration/{common,h2}` | `04_database/local/` | `jdbc:h2:` |

## 3. DDL 模块覆盖

| DDL 模块 | Oracle | DM | PostgreSQL | KingbaseES | H2 |
|---|---|---|---|---|---|
| core_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| sec_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| sec_data_permission | ✅ | ✅ | ✅ | ✅ | — |
| sec_menu_permission | ✅ | ✅ | ✅ | ✅ | — |
| wf_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| notify_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| ops_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| ops_deployment | ✅ | ✅ | ✅ | ✅ | — |
| ops_sync_task | ✅ | ✅ | ✅ | ✅ | — |
| interop_ddl | ✅ | ✅ | ✅ | ✅ | ✅ |
| ai_candidate_review | ✅ | ✅ | ✅ | ✅ | — |
| ai_governance | ✅ | ✅ | ✅ | ✅ | — |
| ai_knowledge_job | ✅ | ✅ | ✅ | ✅ | — |
| ai_safety | ✅ | ✅ | ✅ | ✅ | — |
| cdss_override_log | ✅ | ✅ | ✅ | ✅ | — |
| cdss_safety_red_line | ✅ | ✅ | ✅ | ✅ | — |
| cdss_trigger_point | ✅ | ✅ | ✅ | ✅ | — |
| clinical_safety | ✅ | ✅ | ✅ | ✅ | — |
| knowledge_package | ✅ | ✅ | ✅ | ✅ | — |
| model_provider_config | ✅ | ✅ | ✅ | ✅ | — |
| prov_release_check | ✅ | ✅ | ✅ | ✅ | — |
| qa_acceptance | ✅ | ✅ | ✅ | ✅ | — |
| quality_finding | ✅ | ✅ | ✅ | ✅ | — |

## 4. Flyway Migration 版本

| 版本 | 描述 | Oracle | DM | PostgreSQL | KingbaseES | H2 |
|---|---|---|---|---|---|---|
| V1 | baseline 占位 | ✅ | ✅ | ✅ | ✅ | ✅ |
| V2 | md_patient 加密字段列宽扩展 | ✅ | ✅ | ✅ | ✅ | ✅ |

## 5. 已知问题

| 问题 | 影响 | 状态 |
|---|---|---|
| Oracle/DM core_ddl.sql 中 `pe_recommendation_record` 表重复定义 | DDL 执行可能报错 | 待修复 |
| H2 local/ 目录缺少部分模块 DDL | 开发环境部分功能无表 | 可接受（H2 由 PersistenceService 动态建表） |
| Flyway Community 版不支持 undo | 需手动执行 undo SQL | 已文档化（FLYWAY_ROLLBACK.md） |

## 6. 测试命令

```powershell
# 测试所有方言
pwsh scripts/smoke-dialect.ps1 -Dialect all

# 测试单个方言
pwsh scripts/smoke-dialect.ps1 -Dialect oracle
pwsh scripts/smoke-dialect.ps1 -Dialect dm
pwsh scripts/smoke-dialect.ps1 -Dialect postgres
pwsh scripts/smoke-dialect.ps1 -Dialect kingbase
pwsh scripts/smoke-dialect.ps1 -Dialect h2
```
