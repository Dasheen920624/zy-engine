-- MedKernel v1.0 GA · GA-ENG-BASE-02 · 身份权限闭环（达梦 DM8 Oracle 兼容模式）

CREATE TABLE role_permission (
    id              NUMBER(19)    IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR2(64)  NOT NULL,
    role_code       VARCHAR2(64)  NOT NULL,
    permission_code VARCHAR2(128) NOT NULL,
    effect          VARCHAR2(16)  NOT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    CONSTRAINT uk_role_permission UNIQUE (tenant_id, role_code, permission_code),
    CONSTRAINT ck_role_permission_effect CHECK (effect IN ('ALLOW','DENY'))
);

CREATE INDEX idx_role_permission_tenant_role
    ON role_permission (tenant_id, role_code);

CREATE TABLE user_role_assignment (
    id              NUMBER(19)    IDENTITY PRIMARY KEY,
    tenant_id       VARCHAR2(64)  NOT NULL,
    user_id         VARCHAR2(128) NOT NULL,
    role_code       VARCHAR2(64)  NOT NULL,
    scope_level     VARCHAR2(32)  DEFAULT 'TENANT' NOT NULL,
    scope_code      VARCHAR2(128) NOT NULL,
    active_flag     CHAR(1)       DEFAULT 'Y' NOT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    CONSTRAINT uk_user_role_assignment UNIQUE (tenant_id, user_id, role_code, scope_level, scope_code),
    CONSTRAINT ck_user_role_assignment_active CHECK (active_flag IN ('Y','N'))
);

CREATE INDEX idx_user_role_assignment_user
    ON user_role_assignment (tenant_id, user_id, active_flag);
