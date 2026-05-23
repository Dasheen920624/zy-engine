-- MedKernel v1.0 GA · 达梦 8 Flyway baseline
-- 达梦 8.x 兼容 Oracle 语法，与 oracle/V1__init.sql 等价
-- 真实 schema 将在 GA-CORE-03 / GA-TENANT-01 等任务中加入

CREATE TABLE medkernel_meta (
    id          NUMBER(19) IDENTITY PRIMARY KEY,
    schema_ver  VARCHAR2(32) NOT NULL,
    applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    note        CLOB
);

INSERT INTO medkernel_meta (schema_ver, note)
VALUES ('1.0.0-baseline', 'GA-CORE-03 baseline placeholder');
