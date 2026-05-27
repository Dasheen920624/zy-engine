-- MedKernel v1.0 GA · GA-ENG-API-07 推荐/CDSS API（Oracle）

CREATE TABLE recommendation_trigger (
    id                  NUMBER(19)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trigger_id          VARCHAR2(64)  NOT NULL,
    tenant_id           VARCHAR2(64)  NOT NULL,
    trigger_code        VARCHAR2(128) NOT NULL,
    trigger_type        VARCHAR2(64)  NOT NULL,
    source_event_id     VARCHAR2(64)  NULL,
    context_snapshot_id VARCHAR2(64)  NULL,
    patient_id          VARCHAR2(128) NULL,
    encounter_id        VARCHAR2(128) NULL,
    patient_pathway_id  VARCHAR2(64)  NULL,
    scenario_code       VARCHAR2(128) NOT NULL,
    package_version     VARCHAR2(64)  NULL,
    input_digest        VARCHAR2(128) NOT NULL,
    status              VARCHAR2(32)  DEFAULT 'RECEIVED' NOT NULL,
    error_code          VARCHAR2(64)  NULL,
    occurred_at         TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by          VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by          VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    trace_id            VARCHAR2(128) NULL,
    CONSTRAINT uk_rec_trigger_id UNIQUE (trigger_id),
    CONSTRAINT uk_rec_trigger_tenant_code UNIQUE (tenant_id, trigger_code),
    CONSTRAINT ck_rec_trigger_status CHECK (status IN ('RECEIVED','EVALUATED','NO_CARD','FAILED'))
);

CREATE INDEX idx_rec_trigger_tenant_time ON recommendation_trigger (tenant_id, created_at);
CREATE INDEX idx_rec_trigger_patient     ON recommendation_trigger (tenant_id, patient_id, encounter_id, created_at);
CREATE INDEX idx_rec_trigger_status      ON recommendation_trigger (tenant_id, status, created_at);
CREATE INDEX idx_rec_trigger_scenario    ON recommendation_trigger (tenant_id, scenario_code, created_at);

CREATE TABLE recommendation_card (
    id                              NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id                         VARCHAR2(64)   NOT NULL,
    tenant_id                       VARCHAR2(64)   NOT NULL,
    trigger_id                      VARCHAR2(64)   NOT NULL,
    card_code                       VARCHAR2(128)  NOT NULL,
    card_type                       VARCHAR2(32)   NOT NULL,
    title                           VARCHAR2(256)  NOT NULL,
    summary                         VARCHAR2(1024) NOT NULL,
    suggested_action                VARCHAR2(1024) NOT NULL,
    risk_level                      VARCHAR2(32)   NOT NULL,
    interrupt_level                 VARCHAR2(32)   NOT NULL,
    status                          VARCHAR2(32)   DEFAULT 'PENDING' NOT NULL,
    requires_physician_confirmation NUMBER(1)      DEFAULT 0 NOT NULL,
    ai_generated                    NUMBER(1)      DEFAULT 0 NOT NULL,
    source_summary                  VARCHAR2(1024) NOT NULL,
    explanation_json                CLOB           NULL,
    fatigue_key                     VARCHAR2(256)  NULL,
    expires_at                      TIMESTAMP WITH TIME ZONE NULL,
    created_at                      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by                      VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    updated_at                      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by                      VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    trace_id                        VARCHAR2(128)  NULL,
    CONSTRAINT uk_rec_card_id UNIQUE (card_id),
    CONSTRAINT uk_rec_card_trigger_code UNIQUE (tenant_id, trigger_id, card_code),
    CONSTRAINT ck_rec_card_type CHECK (card_type IN (
        'MEDICATION','EXAM','LAB','PATHWAY','RISK','KNOWLEDGE','QUALITY','NURSING','FOLLOWUP'
    )),
    CONSTRAINT ck_rec_card_risk CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_rec_card_interrupt CHECK (interrupt_level IN (
        'SILENT','INFO','WEAK_INTERRUPTIVE','STRONG_INTERRUPTIVE'
    )),
    CONSTRAINT ck_rec_card_status CHECK (status IN (
        'PENDING','VIEWED','ACCEPTED','REJECTED','DEFERRED','DISMISSED','SUPPRESSED','EXPIRED'
    )),
    CONSTRAINT ck_rec_card_physician_confirmation CHECK (requires_physician_confirmation IN (0, 1)),
    CONSTRAINT ck_rec_card_ai_generated CHECK (ai_generated IN (0, 1))
);

CREATE INDEX idx_rec_card_trigger       ON recommendation_card (tenant_id, trigger_id);
CREATE INDEX idx_rec_card_tenant_status ON recommendation_card (tenant_id, status, created_at);
CREATE INDEX idx_rec_card_risk          ON recommendation_card (tenant_id, risk_level, interrupt_level, created_at);
CREATE INDEX idx_rec_card_fatigue       ON recommendation_card (tenant_id, fatigue_key, created_at);

CREATE TABLE recommendation_source (
    id               NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_id        VARCHAR2(64)   NOT NULL,
    tenant_id        VARCHAR2(64)   NOT NULL,
    card_id          VARCHAR2(64)   NOT NULL,
    source_type      VARCHAR2(32)   NOT NULL,
    source_ref_id    VARCHAR2(128)  NULL,
    source_version   VARCHAR2(128)  NULL,
    source_title     VARCHAR2(256)  NOT NULL,
    citation_locator VARCHAR2(256)  NULL,
    source_hash      VARCHAR2(128)  NULL,
    summary          VARCHAR2(1024) NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by       VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by       VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    trace_id         VARCHAR2(128)  NULL,
    CONSTRAINT uk_rec_source_id UNIQUE (source_id),
    CONSTRAINT ck_rec_source_type CHECK (source_type IN (
        'RULE','PATHWAY','KNOWLEDGE','CONTEXT','TERMINOLOGY','MANUAL'
    ))
);

CREATE INDEX idx_rec_source_card ON recommendation_source (tenant_id, card_id, source_type);

CREATE TABLE recommendation_feedback (
    id            NUMBER(19)     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feedback_id   VARCHAR2(64)   NOT NULL,
    tenant_id     VARCHAR2(64)   NOT NULL,
    card_id       VARCHAR2(64)   NOT NULL,
    feedback_type VARCHAR2(32)   NOT NULL,
    reason_code   VARCHAR2(128)  NULL,
    reason_text   VARCHAR2(1024) NULL,
    operator_id   VARCHAR2(64)   NOT NULL,
    operator_role VARCHAR2(128)  NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by    VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by    VARCHAR2(64)   DEFAULT 'system' NOT NULL,
    trace_id      VARCHAR2(128)  NULL,
    CONSTRAINT uk_rec_feedback_id UNIQUE (feedback_id),
    CONSTRAINT ck_rec_feedback_type CHECK (feedback_type IN (
        'VIEW_SOURCE','ACCEPT','REJECT','DEFER','DISMISS'
    ))
);

CREATE INDEX idx_rec_feedback_card_time ON recommendation_feedback (tenant_id, card_id, created_at);

CREATE TABLE recommendation_fatigue_signal (
    id                NUMBER(19)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    signal_id         VARCHAR2(64)  NOT NULL,
    tenant_id         VARCHAR2(64)  NOT NULL,
    trigger_id        VARCHAR2(64)  NULL,
    card_id           VARCHAR2(64)  NULL,
    fatigue_key       VARCHAR2(256) NULL,
    patient_id        VARCHAR2(128) NULL,
    encounter_id      VARCHAR2(128) NULL,
    operator_id       VARCHAR2(64)  NULL,
    signal_type       VARCHAR2(32)  NOT NULL,
    occurrence_count  NUMBER(10)    DEFAULT 1 NOT NULL,
    window_started_at TIMESTAMP WITH TIME ZONE NULL,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by        VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by        VARCHAR2(64)  DEFAULT 'system' NOT NULL,
    trace_id          VARCHAR2(128) NULL,
    CONSTRAINT uk_rec_fatigue_id UNIQUE (signal_id),
    CONSTRAINT ck_rec_fatigue_signal CHECK (signal_type IN (
        'SHOWN','SILENT_RECORDED','VIEWED','ACCEPTED','REJECTED','DEFERRED','DISMISSED'
    ))
);

CREATE INDEX idx_rec_fatigue_card        ON recommendation_fatigue_signal (tenant_id, card_id, created_at);
CREATE INDEX idx_rec_fatigue_key         ON recommendation_fatigue_signal (tenant_id, fatigue_key, signal_type, created_at);
CREATE INDEX idx_rec_fatigue_tenant_time ON recommendation_fatigue_signal (tenant_id, created_at);
