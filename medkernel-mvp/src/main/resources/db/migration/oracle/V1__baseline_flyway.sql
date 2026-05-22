-- PR-FINAL-25: Flyway baseline migration (Oracle vendor).
-- 同时适用于 DM 8+（DM 兼容 Oracle 语法）。
--
-- 用途：标记 Flyway 接入时点。生产 Oracle 已通过 DBA 部署 schema，
-- 本 migration 仅写入 flyway_schema_history 占位，避免重复创建。
--
-- 后续业务 schema 变更（如 PR-FINAL-23 列宽扩展）放到 V2+。

SELECT 1 FROM DUAL;
