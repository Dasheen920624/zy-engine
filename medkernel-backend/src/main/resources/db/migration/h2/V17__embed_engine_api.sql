-- MedKernel v1.0 GA · GA-ENG-API-11 页面嵌入 API（H2 baseline，MODE=PostgreSQL 兼容）

CREATE TABLE IF NOT EXISTS embed_launch_token (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token                     VARCHAR(128)  NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    user_id                   VARCHAR(64)   NOT NULL,
    role_code                 VARCHAR(64)   NOT NULL,
    patient_id                VARCHAR(64)   NOT NULL,
    encounter_id              VARCHAR(64)   NOT NULL,
    trigger_point             VARCHAR(64)   NOT NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'UNUSED',
    expired_at                TIMESTAMP     NOT NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_embed_launch_token UNIQUE (token)
);

CREATE INDEX idx_embed_token_tenant ON embed_launch_token (tenant_id, token);

CREATE TABLE IF NOT EXISTS embed_origin_whitelist (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id                 VARCHAR(64)   NOT NULL,
    origin                    VARCHAR(255)  NOT NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_embed_origin_tenant UNIQUE (tenant_id, origin)
);
