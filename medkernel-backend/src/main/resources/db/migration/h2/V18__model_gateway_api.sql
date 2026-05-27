-- MedKernel v1.0 GA · GA-ENG-API-12 模型能力网关 API（H2 baseline，MODE=PostgreSQL 兼容）

CREATE TABLE IF NOT EXISTS model_capability_task (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id                   VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    capability_code           VARCHAR(64)   NOT NULL,
    input_hash                VARCHAR(64)   NOT NULL,
    input_summary             VARCHAR(512)  NOT NULL,
    output_content            TEXT          NULL,
    model_mode                VARCHAR(32)   NOT NULL,
    model_version             VARCHAR(64)   NULL,
    prompt_version            VARCHAR(64)   NULL,
    source_citations          VARCHAR(1024) NULL,
    confidence                DOUBLE        NULL,
    risk_level                VARCHAR(32)   NULL,
    fallback_used             BOOLEAN       NOT NULL DEFAULT FALSE,
    fallback_reason           VARCHAR(255)  NULL,
    time_cost_ms              BIGINT        NOT NULL DEFAULT 0,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    trace_id                  VARCHAR(128)  NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_model_task_id UNIQUE (task_id)
);

CREATE INDEX idx_model_task_tenant ON model_capability_task (tenant_id, capability_code);

CREATE TABLE IF NOT EXISTS model_capability_policy (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                 VARCHAR(64)   NOT NULL,
    capability_code           VARCHAR(64)   NOT NULL,
    route_strategy            VARCHAR(32)   NOT NULL DEFAULT 'BASEPLAY',
    desensitize_strategy      VARCHAR(64)   NOT NULL DEFAULT 'DEFAULT',
    expected_schema           TEXT          NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_model_policy_tenant UNIQUE (tenant_id, capability_code)
);
