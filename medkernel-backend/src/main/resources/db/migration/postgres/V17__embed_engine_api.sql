-- MedKernel v1.0 GA · GA-ENG-API-11 页面嵌入 API（PostgreSQL）

CREATE TABLE IF NOT EXISTS embed_launch_token (
    id                        BIGSERIAL PRIMARY KEY,
    token                     VARCHAR(128)  NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    user_id                   VARCHAR(64)   NOT NULL,
    role_code                 VARCHAR(64)   NOT NULL,
    patient_id                VARCHAR(64)   NOT NULL,
    encounter_id              VARCHAR(64)   NOT NULL,
    trigger_point             VARCHAR(64)   NOT NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'UNUSED',
    expired_at                TIMESTAMPTZ   NOT NULL,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_embed_launch_token UNIQUE (token)
);

CREATE INDEX idx_embed_token_tenant ON embed_launch_token (tenant_id, token);

CREATE TABLE IF NOT EXISTS embed_origin_whitelist (
    id                        BIGSERIAL PRIMARY KEY,
    tenant_id                 VARCHAR(64)   NOT NULL,
    origin                    VARCHAR(255)  NOT NULL,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    CONSTRAINT uk_embed_origin_tenant UNIQUE (tenant_id, origin)
);

COMMENT ON TABLE embed_launch_token IS '嵌入页面启动令牌';
COMMENT ON COLUMN embed_launch_token.token IS '一次性安全启动令牌';
COMMENT ON COLUMN embed_launch_token.tenant_id IS '租户ID';
COMMENT ON COLUMN embed_launch_token.user_id IS '用户ID';
COMMENT ON COLUMN embed_launch_token.role_code IS '激活角色编码';
COMMENT ON COLUMN embed_launch_token.patient_id IS '患者ID';
COMMENT ON COLUMN embed_launch_token.encounter_id IS '就诊ID';
COMMENT ON COLUMN embed_launch_token.trigger_point IS '触发位置点';
COMMENT ON COLUMN embed_launch_token.status IS '状态(UNUSED,USED,EXPIRED)';
COMMENT ON COLUMN embed_launch_token.expired_at IS '过期时点';
COMMENT ON COLUMN embed_launch_token.trace_id IS '追踪ID';

COMMENT ON TABLE embed_origin_whitelist IS '嵌入Origin安全域名白名单';
COMMENT ON COLUMN embed_origin_whitelist.tenant_id IS '租户ID';
COMMENT ON COLUMN embed_origin_whitelist.origin IS '允许跨域或iframe加载的域名Origin';
