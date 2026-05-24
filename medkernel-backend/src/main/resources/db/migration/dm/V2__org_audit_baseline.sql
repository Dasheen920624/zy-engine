-- MedKernel v1.0 GA · 达梦 8 baseline schema（兼容 Oracle 语法）
-- 引擎一切之根：组织六级树 + 审计留痕。

CREATE TABLE org_unit (
    id              NUMBER(19)    IDENTITY PRIMARY KEY,
    parent_id       NUMBER(19)    NULL,
    tenant_id       VARCHAR2(64)  NOT NULL,
    level_code      VARCHAR2(32)  NOT NULL,
    code            VARCHAR2(128) NOT NULL,
    name            VARCHAR2(256) NOT NULL,
    name_pinyin     VARCHAR2(256) NULL,
    specialty_id    VARCHAR2(64)  NULL,
    status          VARCHAR2(32)  DEFAULT 'ACTIVE' NOT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by      VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    CONSTRAINT uk_org_unit_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_org_unit_level CHECK (level_code IN ('TENANT','GROUP','HOSPITAL','CAMPUS','SITE','DEPARTMENT','WARD')),
    CONSTRAINT ck_org_unit_status CHECK (status IN ('ACTIVE','SUSPENDED','ARCHIVED'))
);

CREATE INDEX idx_org_unit_parent ON org_unit (parent_id);
CREATE INDEX idx_org_unit_tenant_lv ON org_unit (tenant_id, level_code);

CREATE TABLE audit_event (
    id              NUMBER(19)    IDENTITY PRIMARY KEY,
    event_id        VARCHAR2(64)  NOT NULL,
    trace_id        VARCHAR2(128) NULL,
    occurred_at     TIMESTAMP     NOT NULL,
    actor_user_id   VARCHAR2(64)  NULL,
    action          VARCHAR2(32)  NOT NULL,
    resource_type   VARCHAR2(128) NOT NULL,
    resource_id     VARCHAR2(128) NOT NULL,
    summary         VARCHAR2(512) NULL,
    payload_digest  VARCHAR2(128) NULL,
    tenant_id       VARCHAR2(64)  NOT NULL,
    hospital_id     VARCHAR2(64)  NULL,
    department_id   VARCHAR2(64)  NULL,
    ip_address      VARCHAR2(64)  NULL,
    user_agent      VARCHAR2(512) NULL,
    signature       VARCHAR2(512) NULL,
    status          VARCHAR2(32)  DEFAULT 'RECORDED' NOT NULL,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_audit_event_event_id UNIQUE (event_id),
    CONSTRAINT ck_audit_event_status CHECK (status IN ('RECORDED','SIGNED','TSA_SIGNED','REJECTED'))
);

CREATE INDEX idx_audit_event_resource ON audit_event (resource_type, resource_id, occurred_at);
CREATE INDEX idx_audit_event_actor    ON audit_event (actor_user_id, occurred_at);
CREATE INDEX idx_audit_event_tenant   ON audit_event (tenant_id, occurred_at);
CREATE INDEX idx_audit_event_trace    ON audit_event (trace_id);
