-- PR-FINAL-25: Flyway baseline migration (H2 vendor).
-- 用途：标记 Flyway 接入时点。本文件意图为「占位 + 文档」，不创建任何业务表，
-- 因为业务表仍由现有 PersistenceService.@PostConstruct loadSchemaStatements()
-- 在 H2 模式下负责初始化（向后兼容）。
--
-- 后续真正的业务 schema 变更（如 PR-FINAL-23 列宽扩展）放到 V2+。
--
-- 开发者注意：
--  - 文件名必须遵守 Flyway 规则：V{version}__{description}.sql
--  - version 用 1, 2, 3... 自然数；同版本号在 4 个 vendor 目录共用语义
--  - description 用 snake_case 描述变更主题
--  - SQL 内容必须能在该 vendor 上幂等执行（CREATE TABLE IF NOT EXISTS / ALTER TABLE IF NOT EXISTS）

-- 标记 baseline。Flyway 会写入 flyway_schema_history 表记录本版本。
SELECT 1;
