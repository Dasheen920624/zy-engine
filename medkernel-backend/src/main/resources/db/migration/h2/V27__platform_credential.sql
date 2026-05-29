-- MedKernel v1.0 GA · V26 平台自建身份凭证表（H2 2.2）
CREATE TABLE IF NOT EXISTS platform_credential (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    credential_id   VARCHAR(64)  NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(128) NOT NULL,
    username        VARCHAR(128) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    must_change_pwd CHAR(1)      NOT NULL DEFAULT 'Y',
    mfa_secret      VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id        VARCHAR(64),
    CONSTRAINT uk_platform_credential_id UNIQUE (credential_id),
    CONSTRAINT uk_platform_credential_username UNIQUE (tenant_id, username),
    CONSTRAINT ck_platform_credential_status CHECK (status IN ('ACTIVE','DISABLED','LOCKED')),
    CONSTRAINT ck_platform_credential_mustchg CHECK (must_change_pwd IN ('Y','N'))
);

CREATE INDEX IF NOT EXISTS idx_platform_credential_login
    ON platform_credential (tenant_id, username, status);

COMMENT ON TABLE platform_credential IS '平台自建身份凭证表（外网 SaaS / 本地 dev 登录）';
