-- MedKernel v1.0 GA · GA-ENG-API-09 随访 API（H2 baseline，MODE=PostgreSQL 兼容）

CREATE TABLE IF NOT EXISTS followup_plan (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id                   VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    patient_id                VARCHAR(64)   NOT NULL,
    encounter_id              VARCHAR(64)   NULL,
    pathway_id                VARCHAR(64)   NULL,
    disease_code              VARCHAR(128)  NULL,
    risk_level                VARCHAR(32)   NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_followup_plan_id UNIQUE (plan_id)
);

CREATE INDEX idx_followup_plan_tenant_patient ON followup_plan (tenant_id, patient_id);
CREATE INDEX idx_followup_plan_status ON followup_plan (status);

CREATE TABLE IF NOT EXISTS followup_task (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id                   VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    plan_id                   VARCHAR(64)   NOT NULL,
    task_type                 VARCHAR(32)   NOT NULL,
    due_date                  TIMESTAMP     NOT NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    executor_id               VARCHAR(64)   NULL,
    executor_type             VARCHAR(32)   NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_followup_task_id UNIQUE (task_id)
);

CREATE INDEX idx_followup_task_tenant_plan ON followup_task (tenant_id, plan_id);
CREATE INDEX idx_followup_task_due_date ON followup_task (due_date);

CREATE TABLE IF NOT EXISTS followup_questionnaire (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    questionnaire_id          VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    task_id                   VARCHAR(64)   NOT NULL,
    form_data                 CLOB          NOT NULL,
    score                     NUMERIC(10,2) NULL,
    status                    VARCHAR(32)   NOT NULL DEFAULT 'DRAFT',
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_followup_questionnaire_id UNIQUE (questionnaire_id)
);

CREATE INDEX idx_followup_questionnaire_task ON followup_questionnaire (tenant_id, task_id);

CREATE TABLE IF NOT EXISTS followup_event (
    id                        BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id                  VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    plan_id                   VARCHAR(64)   NULL,
    event_type                VARCHAR(32)   NOT NULL,
    payload                   CLOB          NOT NULL,
    triggered_by              VARCHAR(64)   NULL,
    created_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                  VARCHAR(128)  NULL,
    CONSTRAINT uk_followup_event_id UNIQUE (event_id)
);

CREATE INDEX idx_followup_event_plan ON followup_event (tenant_id, plan_id);
CREATE INDEX idx_followup_event_type ON followup_event (tenant_id, event_type);
