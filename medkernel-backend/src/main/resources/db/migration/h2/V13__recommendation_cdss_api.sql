-- MedKernel v1.0 GA · GA-ENG-API-07 推荐/CDSS API（H2 baseline，MODE=PostgreSQL 兼容）

CREATE TABLE IF NOT EXISTS recommendation_trigger (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trigger_id          VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    trigger_code        VARCHAR(128) NOT NULL,
    trigger_type        VARCHAR(64)  NOT NULL,
    source_event_id     VARCHAR(64)  NULL,
    context_snapshot_id VARCHAR(64)  NULL,
    patient_id          VARCHAR(128) NULL,
    encounter_id        VARCHAR(128) NULL,
    patient_pathway_id  VARCHAR(64)  NULL,
    scenario_code       VARCHAR(128) NOT NULL,
    package_version     VARCHAR(64)  NULL,
    input_digest        VARCHAR(128) NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'RECEIVED',
    error_code          VARCHAR(64)  NULL,
    occurred_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id            VARCHAR(128) NULL,
    CONSTRAINT uk_rec_trigger_id UNIQUE (trigger_id),
    CONSTRAINT uk_rec_trigger_tenant_code UNIQUE (tenant_id, trigger_code),
    CONSTRAINT ck_rec_trigger_status CHECK (status IN ('RECEIVED','EVALUATED','NO_CARD','FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_rec_trigger_tenant_time ON recommendation_trigger (tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_patient     ON recommendation_trigger (tenant_id, patient_id, encounter_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_status      ON recommendation_trigger (tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_trigger_scenario    ON recommendation_trigger (tenant_id, scenario_code, created_at);

CREATE TABLE IF NOT EXISTS recommendation_card (
    id                              BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id                         VARCHAR(64)   NOT NULL,
    tenant_id                       VARCHAR(64)   NOT NULL,
    trigger_id                      VARCHAR(64)   NOT NULL,
    card_code                       VARCHAR(128)  NOT NULL,
    card_type                       VARCHAR(32)   NOT NULL,
    title                           VARCHAR(256)  NOT NULL,
    summary                         VARCHAR(1024) NOT NULL,
    suggested_action                VARCHAR(1024) NOT NULL,
    risk_level                      VARCHAR(32)   NOT NULL,
    interrupt_level                 VARCHAR(32)   NOT NULL,
    status                          VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    requires_physician_confirmation BOOLEAN       NOT NULL DEFAULT FALSE,
    ai_generated                    BOOLEAN       NOT NULL DEFAULT FALSE,
    source_summary                  VARCHAR(1024) NOT NULL,
    explanation_json                CLOB          NULL,
    fatigue_key                     VARCHAR(256)  NULL,
    expires_at                      TIMESTAMP     NULL,
    created_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                      VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at                      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                      VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id                        VARCHAR(128)  NULL,
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
    CONSTRAINT ck_rec_card_physician_confirmation CHECK (requires_physician_confirmation IN (TRUE, FALSE)),
    CONSTRAINT ck_rec_card_ai_generated CHECK (ai_generated IN (TRUE, FALSE))
);

CREATE INDEX IF NOT EXISTS idx_rec_card_trigger       ON recommendation_card (tenant_id, trigger_id);
CREATE INDEX IF NOT EXISTS idx_rec_card_tenant_status ON recommendation_card (tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_card_risk          ON recommendation_card (tenant_id, risk_level, interrupt_level, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_card_fatigue       ON recommendation_card (tenant_id, fatigue_key, created_at);

CREATE TABLE IF NOT EXISTS recommendation_source (
    id               BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    source_id        VARCHAR(64)   NOT NULL,
    tenant_id        VARCHAR(64)   NOT NULL,
    card_id          VARCHAR(64)   NOT NULL,
    source_type      VARCHAR(32)   NOT NULL,
    source_ref_id    VARCHAR(128)  NULL,
    source_version   VARCHAR(128)  NULL,
    source_title     VARCHAR(256)  NOT NULL,
    citation_locator VARCHAR(256)  NULL,
    source_hash      VARCHAR(128)  NULL,
    summary          VARCHAR(1024) NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by       VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by       VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id         VARCHAR(128)  NULL,
    CONSTRAINT uk_rec_source_id UNIQUE (source_id),
    CONSTRAINT ck_rec_source_type CHECK (source_type IN (
        'RULE','PATHWAY','KNOWLEDGE','CONTEXT','TERMINOLOGY','MANUAL'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_source_card ON recommendation_source (tenant_id, card_id, source_type);

CREATE TABLE IF NOT EXISTS recommendation_feedback (
    id            BIGINT        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    feedback_id   VARCHAR(64)   NOT NULL,
    tenant_id     VARCHAR(64)   NOT NULL,
    card_id       VARCHAR(64)   NOT NULL,
    feedback_type VARCHAR(32)   NOT NULL,
    reason_code   VARCHAR(128)  NULL,
    reason_text   VARCHAR(1024) NULL,
    operator_id   VARCHAR(64)   NOT NULL,
    operator_role VARCHAR(128)  NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by    VARCHAR(64)   NOT NULL DEFAULT 'system',
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    VARCHAR(64)   NOT NULL DEFAULT 'system',
    trace_id      VARCHAR(128)  NULL,
    CONSTRAINT uk_rec_feedback_id UNIQUE (feedback_id),
    CONSTRAINT ck_rec_feedback_type CHECK (feedback_type IN (
        'VIEW_SOURCE','ACCEPT','REJECT','DEFER','DISMISS'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_feedback_card_time ON recommendation_feedback (tenant_id, card_id, created_at);

CREATE TABLE IF NOT EXISTS recommendation_fatigue_signal (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    signal_id         VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    trigger_id        VARCHAR(64)  NULL,
    card_id           VARCHAR(64)  NULL,
    fatigue_key       VARCHAR(256) NULL,
    patient_id        VARCHAR(128) NULL,
    encounter_id      VARCHAR(128) NULL,
    operator_id       VARCHAR(64)  NULL,
    signal_type       VARCHAR(32)  NOT NULL,
    occurrence_count  INT          NOT NULL DEFAULT 1,
    window_started_at TIMESTAMP    NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id          VARCHAR(128) NULL,
    CONSTRAINT uk_rec_fatigue_id UNIQUE (signal_id),
    CONSTRAINT ck_rec_fatigue_signal CHECK (signal_type IN (
        'SHOWN','SILENT_RECORDED','VIEWED','ACCEPTED','REJECTED','DEFERRED','DISMISSED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_rec_fatigue_card        ON recommendation_fatigue_signal (tenant_id, card_id, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_fatigue_key         ON recommendation_fatigue_signal (tenant_id, fatigue_key, signal_type, created_at);
CREATE INDEX IF NOT EXISTS idx_rec_fatigue_tenant_time ON recommendation_fatigue_signal (tenant_id, created_at);
