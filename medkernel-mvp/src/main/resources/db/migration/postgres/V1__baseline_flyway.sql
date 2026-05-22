-- PR-FINAL-25: Flyway baseline migration (PostgreSQL vendor).
-- 同时适用于 KingbaseES（KingbaseES 完全 PG 兼容，jdbc:postgresql 协议直接走）。
--
-- 用途：标记 Flyway 接入时点。本 migration 仅写入 flyway_schema_history 占位。
--
-- 后续业务 schema 变更（如 PR-FINAL-23 列宽扩展）放到 V2+。

SELECT 1;
