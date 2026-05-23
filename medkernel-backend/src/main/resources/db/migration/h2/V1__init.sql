-- MedKernel v1.0 GA · H2 2.2 Flyway baseline（本地开发 + CI）
-- 真实 schema 将在 GA-CORE-03 / GA-TENANT-01 等任务中加入

CREATE TABLE IF NOT EXISTS medkernel_meta (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    schema_ver  VARCHAR(32) NOT NULL,
    applied_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note        CLOB
);

INSERT INTO medkernel_meta (schema_ver, note)
VALUES ('1.0.0-baseline', 'GA-CORE-03 baseline placeholder');
