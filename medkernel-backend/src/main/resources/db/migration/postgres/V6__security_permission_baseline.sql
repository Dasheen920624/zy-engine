-- MedKernel v1.0 GA · GA-ENG-BASE-02 · 身份权限闭环（PostgreSQL 15+）

CREATE TABLE IF NOT EXISTS role_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    role_code       VARCHAR(64)  NOT NULL,
    permission_code VARCHAR(128) NOT NULL,
    effect          VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_role_permission UNIQUE (tenant_id, role_code, permission_code),
    CONSTRAINT ck_role_permission_effect CHECK (effect IN ('ALLOW','DENY'))
);

CREATE INDEX IF NOT EXISTS idx_role_permission_tenant_role
    ON role_permission (tenant_id, role_code);

COMMENT ON TABLE role_permission IS '租户级角色权限覆盖：在默认权限矩阵上显式 ALLOW / DENY';

CREATE TABLE IF NOT EXISTS user_role_assignment (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64)  NOT NULL,
    user_id         VARCHAR(128) NOT NULL,
    role_code       VARCHAR(64)  NOT NULL,
    scope_level     VARCHAR(32)  NOT NULL DEFAULT 'TENANT',
    scope_code      VARCHAR(128) NOT NULL,
    active_flag     CHAR(1)      NOT NULL DEFAULT 'Y',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(64)  NOT NULL DEFAULT 'system',
    CONSTRAINT uk_user_role_assignment UNIQUE (tenant_id, user_id, role_code, scope_level, scope_code),
    CONSTRAINT ck_user_role_assignment_active CHECK (active_flag IN ('Y','N'))
);

CREATE INDEX IF NOT EXISTS idx_user_role_assignment_user
    ON user_role_assignment (tenant_id, user_id, active_flag);

COMMENT ON TABLE user_role_assignment IS '用户角色分配与数据范围审计；JWT roles 之外的医院侧补充分配';
