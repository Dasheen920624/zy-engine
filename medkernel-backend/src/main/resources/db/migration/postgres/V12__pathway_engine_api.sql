-- MedKernel v1.0 GA · GA-ENG-API-06 路径引擎 API（PostgreSQL）

CREATE TABLE IF NOT EXISTS specialty_package (
    id                BIGSERIAL PRIMARY KEY,
    package_id        VARCHAR(64)  NOT NULL,
    tenant_id         VARCHAR(64)  NOT NULL,
    package_code      VARCHAR(128) NOT NULL,
    disease_code      VARCHAR(128) NOT NULL,
    name              VARCHAR(256) NOT NULL,
    package_version   VARCHAR(64)  NOT NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    source_ref        VARCHAR(512) NOT NULL,
    description       VARCHAR(1024) NULL,
    published_at      TIMESTAMPTZ  NULL,
    published_by      VARCHAR(64)  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by        VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id          VARCHAR(128) NULL,
    CONSTRAINT uk_specialty_package_tenant_code UNIQUE (tenant_id, package_code, package_version),
    CONSTRAINT ck_specialty_package_status CHECK (status IN ('DRAFT','PUBLISHED','OFFLINE','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_specialty_package_tenant_status ON specialty_package (tenant_id, status, updated_at);
CREATE INDEX IF NOT EXISTS idx_specialty_package_disease       ON specialty_package (tenant_id, disease_code);

CREATE TABLE IF NOT EXISTS specialty_profile (
    id                   BIGSERIAL PRIMARY KEY,
    profile_id           VARCHAR(64)  NOT NULL,
    tenant_id            VARCHAR(64)  NOT NULL,
    package_id           VARCHAR(64)  NOT NULL,
    profile_code         VARCHAR(128) NOT NULL,
    name                 VARCHAR(256) NOT NULL,
    stratification_json  TEXT         NULL,
    entry_criteria_json  TEXT         NULL,
    exit_criteria_json   TEXT         NULL,
    followup_plan_json   TEXT         NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by           VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id             VARCHAR(128) NULL,
    CONSTRAINT uk_specialty_profile_package_code UNIQUE (tenant_id, package_id, profile_code)
);

CREATE INDEX IF NOT EXISTS idx_specialty_profile_package ON specialty_profile (tenant_id, package_id);

CREATE TABLE IF NOT EXISTS pathway_template (
    id                   BIGSERIAL PRIMARY KEY,
    template_id          VARCHAR(64)  NOT NULL,
    tenant_id            VARCHAR(64)  NOT NULL,
    package_id           VARCHAR(64)  NOT NULL,
    template_code        VARCHAR(128) NOT NULL,
    name                 VARCHAR(256) NOT NULL,
    disease_code         VARCHAR(128) NOT NULL,
    template_version     INT          NOT NULL DEFAULT 1,
    template_level       VARCHAR(32)  NOT NULL DEFAULT 'STANDARD',
    status               VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    start_node_code      VARCHAR(128) NULL,
    source_ref           VARCHAR(512) NOT NULL,
    description          VARCHAR(1024) NULL,
    entry_criteria_json  TEXT         NULL,
    exit_criteria_json   TEXT         NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by           VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id             VARCHAR(128) NULL,
    CONSTRAINT uk_pathway_template_tenant_code UNIQUE (tenant_id, template_code, template_version),
    CONSTRAINT ck_pathway_template_level CHECK (template_level IN (
        'STANDARD','GROUP','HOSPITAL','DEPARTMENT','SPECIALTY'
    )),
    CONSTRAINT ck_pathway_template_status CHECK (status IN ('DRAFT','PUBLISHED','OFFLINE','ARCHIVED'))
);

CREATE INDEX IF NOT EXISTS idx_pathway_template_tenant_status ON pathway_template (tenant_id, status, updated_at);
CREATE INDEX IF NOT EXISTS idx_pathway_template_package       ON pathway_template (tenant_id, package_id);
CREATE INDEX IF NOT EXISTS idx_pathway_template_disease       ON pathway_template (tenant_id, disease_code);

CREATE TABLE IF NOT EXISTS pathway_node (
    id                  BIGSERIAL PRIMARY KEY,
    node_id             VARCHAR(64)  NOT NULL,
    tenant_id           VARCHAR(64)  NOT NULL,
    template_id         VARCHAR(64)  NOT NULL,
    node_code           VARCHAR(128) NOT NULL,
    name                VARCHAR(256) NOT NULL,
    node_type           VARCHAR(32)  NOT NULL,
    sort_order          INT          NOT NULL DEFAULT 0,
    responsible_role    VARCHAR(128) NULL,
    dependency_json     TEXT         NULL,
    time_window_minutes INT          NULL,
    terminal_flag       BOOLEAN      NOT NULL DEFAULT FALSE,
    config_json         TEXT         NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id            VARCHAR(128) NULL,
    CONSTRAINT uk_pathway_node_template_code UNIQUE (tenant_id, template_id, node_code),
    CONSTRAINT ck_pathway_node_type CHECK (node_type IN (
        'SCREENING','ASSESSMENT','EXAM','LAB','MEDICATION','SURGERY',
        'NURSING','REHAB','DISCHARGE','FOLLOWUP','QUALITY'
    )),
    CONSTRAINT ck_pathway_node_terminal CHECK (terminal_flag IN (TRUE, FALSE))
);

CREATE INDEX IF NOT EXISTS idx_pathway_node_template_order ON pathway_node (tenant_id, template_id, sort_order);

CREATE TABLE IF NOT EXISTS pathway_edge (
    id             BIGSERIAL PRIMARY KEY,
    edge_id        VARCHAR(64)  NOT NULL,
    tenant_id      VARCHAR(64)  NOT NULL,
    template_id    VARCHAR(64)  NOT NULL,
    edge_code      VARCHAR(128) NOT NULL,
    from_node_code VARCHAR(128) NOT NULL,
    to_node_code   VARCHAR(128) NOT NULL,
    edge_type      VARCHAR(32)  NOT NULL DEFAULT 'DEFAULT',
    condition_json TEXT         NULL,
    priority       INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by     VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id       VARCHAR(128) NULL,
    CONSTRAINT uk_pathway_edge_template_code UNIQUE (tenant_id, template_id, edge_code),
    CONSTRAINT ck_pathway_edge_type CHECK (edge_type IN (
        'DEFAULT','CONDITION','RISK_STRATIFICATION','PATIENT_CHOICE',
        'RESOURCE_UNAVAILABLE','PHYSICIAN_DECISION','ROLLBACK'
    ))
);

CREATE INDEX IF NOT EXISTS idx_pathway_edge_template_from ON pathway_edge (tenant_id, template_id, from_node_code, priority);
CREATE INDEX IF NOT EXISTS idx_pathway_edge_template_to   ON pathway_edge (tenant_id, template_id, to_node_code);

CREATE TABLE IF NOT EXISTS patient_pathway (
    id                 BIGSERIAL PRIMARY KEY,
    patient_pathway_id VARCHAR(64)  NOT NULL,
    tenant_id          VARCHAR(64)  NOT NULL,
    patient_id         VARCHAR(128) NOT NULL,
    encounter_id       VARCHAR(128) NULL,
    template_id        VARCHAR(64)  NOT NULL,
    current_node_code  VARCHAR(128) NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'ENTERED',
    entered_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at       TIMESTAMPTZ  NULL,
    exited_at          TIMESTAMPTZ  NULL,
    exit_reason        VARCHAR(512) NULL,
    last_event_id      VARCHAR(64)  NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id           VARCHAR(128) NULL,
    CONSTRAINT uk_patient_pathway_id UNIQUE (patient_pathway_id),
    CONSTRAINT ck_patient_pathway_status CHECK (status IN (
        'ENTERED','NODE_EXECUTING','VARIANCE','COMPLETED','EXITED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_patient_pathway_patient         ON patient_pathway (tenant_id, patient_id, entered_at);
CREATE INDEX IF NOT EXISTS idx_patient_pathway_template_status ON patient_pathway (tenant_id, template_id, status);

CREATE TABLE IF NOT EXISTS pathway_variance (
    id                 BIGSERIAL PRIMARY KEY,
    variance_id        VARCHAR(64)  NOT NULL,
    tenant_id          VARCHAR(64)  NOT NULL,
    patient_pathway_id VARCHAR(64)  NOT NULL,
    node_code          VARCHAR(128) NOT NULL,
    variance_type      VARCHAR(32)  NOT NULL,
    reason             VARCHAR(1024) NOT NULL,
    resolution_action  VARCHAR(512) NULL,
    continue_node_code VARCHAR(128) NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id           VARCHAR(128) NULL,
    CONSTRAINT uk_pathway_variance_id UNIQUE (variance_id),
    CONSTRAINT ck_pathway_variance_type CHECK (variance_type IN (
        'MEDICAL','PATIENT_REASON','RESOURCE_REASON','DOCTOR_CHOICE','SYSTEM_REASON'
    ))
);

CREATE INDEX IF NOT EXISTS idx_pathway_variance_pathway_time ON pathway_variance (tenant_id, patient_pathway_id, created_at);

CREATE TABLE IF NOT EXISTS clinical_clock (
    id                 BIGSERIAL PRIMARY KEY,
    clock_id           VARCHAR(64)  NOT NULL,
    tenant_id          VARCHAR(64)  NOT NULL,
    patient_pathway_id VARCHAR(64)  NOT NULL,
    node_code          VARCHAR(128) NOT NULL,
    metric_code        VARCHAR(128) NULL,
    started_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    due_at             TIMESTAMPTZ  NULL,
    completed_at       TIMESTAMPTZ  NULL,
    status             VARCHAR(32)  NOT NULL DEFAULT 'RUNNING',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id           VARCHAR(128) NULL,
    CONSTRAINT uk_clinical_clock_id UNIQUE (clock_id),
    CONSTRAINT ck_clinical_clock_status CHECK (status IN (
        'RUNNING','COMPLETED','TIMEOUT','MISSING_DATA','VARIANCE'
    ))
);

CREATE INDEX IF NOT EXISTS idx_clinical_clock_pathway ON clinical_clock (tenant_id, patient_pathway_id, started_at);
CREATE INDEX IF NOT EXISTS idx_clinical_clock_due     ON clinical_clock (tenant_id, status, due_at);

CREATE TABLE IF NOT EXISTS specialty_metric_binding (
    id            BIGSERIAL PRIMARY KEY,
    binding_id    VARCHAR(64)  NOT NULL,
    tenant_id     VARCHAR(64)  NOT NULL,
    package_id    VARCHAR(64)  NOT NULL,
    template_id   VARCHAR(64)  NOT NULL,
    node_code     VARCHAR(128) NOT NULL,
    metric_code   VARCHAR(128) NOT NULL,
    required_flag BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(64)  NOT NULL DEFAULT 'system',
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(64)  NOT NULL DEFAULT 'system',
    trace_id      VARCHAR(128) NULL,
    CONSTRAINT uk_specialty_metric_binding UNIQUE (tenant_id, package_id, template_id, node_code, metric_code),
    CONSTRAINT ck_specialty_metric_required CHECK (required_flag IN (TRUE, FALSE))
);

CREATE INDEX IF NOT EXISTS idx_specialty_metric_package  ON specialty_metric_binding (tenant_id, package_id, metric_code);
CREATE INDEX IF NOT EXISTS idx_specialty_metric_template ON specialty_metric_binding (tenant_id, template_id, node_code);
