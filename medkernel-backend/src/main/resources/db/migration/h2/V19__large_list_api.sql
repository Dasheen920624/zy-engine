-- MedKernel v1.0 GA · GA-ENG-API-13 大规模列表 API（H2 baseline，MODE=PostgreSQL 兼容）
-- 所有表和列的注释必须使用简体中文

CREATE TABLE IF NOT EXISTS large_list_export_job (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_id                    VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    resource_type             VARCHAR(64)   NOT NULL,
    filter_criteria           TEXT          NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    file_name                 VARCHAR(255)  NULL,
    file_path                 VARCHAR(512)  NULL,
    file_size                 BIGINT        NOT NULL DEFAULT 0,
    error_message             VARCHAR(512)  NULL,
    time_cost_ms              BIGINT        NOT NULL DEFAULT 0,
    trace_id                  VARCHAR(128)  NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_large_list_job UNIQUE (job_id)
);

CREATE INDEX idx_large_list_job_tenant ON large_list_export_job (tenant_id, resource_type);
