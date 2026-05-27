CREATE TABLE evaluation_indicator (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    indicator_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    indicator_code VARCHAR2(64 CHAR) NOT NULL,
    version_no NUMBER(10) NOT NULL,
    name VARCHAR2(256 CHAR) NOT NULL,
    subject_type VARCHAR2(32 CHAR) NOT NULL,
    denominator_definition CLOB NOT NULL,
    numerator_definition CLOB NOT NULL,
    exclusion_definition CLOB,
    scoring_definition CLOB,
    time_window VARCHAR2(128 CHAR) NOT NULL,
    organization_scope CLOB NOT NULL,
    responsible_department_id VARCHAR2(64 CHAR),
    source_ref VARCHAR2(256 CHAR),
    package_version VARCHAR2(64 CHAR),
    status VARCHAR2(32 CHAR) DEFAULT 'DRAFT' NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    published_by VARCHAR2(64 CHAR),
    activated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_eval_indicator_id UNIQUE (tenant_id, indicator_id),
    CONSTRAINT uk_eval_indicator_tenant_version UNIQUE (tenant_id, indicator_code, version_no),
    CONSTRAINT ck_eval_indicator_subject CHECK (subject_type IN ('PATIENT', 'MEDICAL_RECORD', 'DEPARTMENT', 'DOCTOR', 'DISEASE', 'PATHWAY', 'CLAIM', 'FOLLOWUP')),
    CONSTRAINT ck_eval_indicator_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'ACTIVE', 'OFFLINE', 'ARCHIVED'))
);

CREATE INDEX idx_eval_indicator_tenant_status ON evaluation_indicator (tenant_id, status, updated_at);
CREATE INDEX idx_eval_indicator_code_status ON evaluation_indicator (tenant_id, indicator_code, status);

CREATE TABLE evaluation_run (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    run_code VARCHAR2(64 CHAR) NOT NULL,
    run_type VARCHAR2(32 CHAR) NOT NULL,
    source_event_id VARCHAR2(64 CHAR),
    context_snapshot_id VARCHAR2(64 CHAR),
    patient_id VARCHAR2(64 CHAR),
    encounter_id VARCHAR2(64 CHAR),
    scenario_code VARCHAR2(64 CHAR),
    package_version VARCHAR2(64 CHAR),
    input_digest VARCHAR2(128 CHAR),
    status VARCHAR2(32 CHAR) DEFAULT 'RECEIVED' NOT NULL,
    error_code VARCHAR2(64 CHAR),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_eval_run_id UNIQUE (tenant_id, run_id),
    CONSTRAINT uk_eval_run_tenant_code UNIQUE (tenant_id, run_code),
    CONSTRAINT ck_eval_run_type CHECK (run_type IN ('MANUAL_SAMPLE', 'UPSTREAM_RESULT', 'BATCH_IMPORT')),
    CONSTRAINT ck_eval_run_status CHECK (status IN ('RECEIVED', 'RECORDED', 'FAILED'))
);

CREATE INDEX idx_eval_run_tenant_time ON evaluation_run (tenant_id, occurred_at);
CREATE INDEX idx_eval_run_context ON evaluation_run (tenant_id, encounter_id, scenario_code);

CREATE TABLE evaluation_result (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    result_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    run_id VARCHAR2(64 CHAR) NOT NULL,
    indicator_id VARCHAR2(64 CHAR) NOT NULL,
    indicator_code VARCHAR2(64 CHAR) NOT NULL,
    indicator_version NUMBER(10) NOT NULL,
    subject_type VARCHAR2(32 CHAR) NOT NULL,
    subject_ref_id VARCHAR2(64 CHAR) NOT NULL,
    score_value NUMBER(18,4),
    result_level VARCHAR2(32 CHAR) NOT NULL,
    hit_flag NUMBER(1) DEFAULT 0 NOT NULL,
    evidence_summary VARCHAR2(2048 CHAR),
    source_ref VARCHAR2(256 CHAR),
    responsible_department_id VARCHAR2(64 CHAR),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_eval_result_id UNIQUE (tenant_id, result_id),
    CONSTRAINT ck_eval_result_subject CHECK (subject_type IN ('PATIENT', 'MEDICAL_RECORD', 'DEPARTMENT', 'DOCTOR', 'DISEASE', 'PATHWAY', 'CLAIM', 'FOLLOWUP')),
    CONSTRAINT ck_eval_result_level CHECK (result_level IN ('PASS', 'ATTENTION', 'NON_COMPLIANT', 'CRITICAL'))
);

CREATE INDEX idx_eval_result_run ON evaluation_result (tenant_id, run_id);
CREATE INDEX idx_eval_result_indicator ON evaluation_result (tenant_id, indicator_code, created_at);

CREATE TABLE quality_finding (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    finding_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    run_id VARCHAR2(64 CHAR) NOT NULL,
    result_id VARCHAR2(64 CHAR) NOT NULL,
    indicator_id VARCHAR2(64 CHAR) NOT NULL,
    finding_code VARCHAR2(64 CHAR) NOT NULL,
    title VARCHAR2(256 CHAR) NOT NULL,
    description VARCHAR2(2048 CHAR) NOT NULL,
    severity VARCHAR2(8 CHAR) NOT NULL,
    status VARCHAR2(32 CHAR) DEFAULT 'NEW' NOT NULL,
    evidence_summary VARCHAR2(2048 CHAR) NOT NULL,
    responsible_department_id VARCHAR2(64 CHAR),
    due_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_quality_finding_id UNIQUE (tenant_id, finding_id),
    CONSTRAINT uk_quality_finding_result_code UNIQUE (tenant_id, result_id, finding_code),
    CONSTRAINT ck_quality_finding_severity CHECK (severity IN ('P0', 'P1', 'P2', 'P3')),
    CONSTRAINT ck_quality_finding_status CHECK (status IN ('NEW', 'ASSIGNED', 'REMEDIATING', 'CLOSED', 'WAIVED'))
);

CREATE INDEX idx_quality_finding_status ON quality_finding (tenant_id, status, severity);
CREATE INDEX idx_quality_finding_department ON quality_finding (tenant_id, responsible_department_id, due_at);

CREATE TABLE rectification_task (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    finding_id VARCHAR2(64 CHAR) NOT NULL,
    responsible_department_id VARCHAR2(64 CHAR) NOT NULL,
    assignee_user_id VARCHAR2(64 CHAR),
    status VARCHAR2(32 CHAR) DEFAULT 'ASSIGNED' NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    rectification_summary VARCHAR2(2048 CHAR),
    evidence_ref VARCHAR2(256 CHAR),
    submitted_at TIMESTAMP WITH TIME ZONE,
    submitted_by VARCHAR2(64 CHAR),
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_rect_task_id UNIQUE (tenant_id, task_id),
    CONSTRAINT uk_rect_task_finding UNIQUE (tenant_id, finding_id),
    CONSTRAINT ck_rect_task_status CHECK (status IN ('ASSIGNED', 'SUBMITTED', 'RETURNED', 'CLOSED', 'WAIVED'))
);

CREATE INDEX idx_rect_task_finding ON rectification_task (tenant_id, finding_id);
CREATE INDEX idx_rect_task_department_status ON rectification_task (tenant_id, responsible_department_id, status, due_at);

CREATE TABLE rectification_review (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id VARCHAR2(64 CHAR) NOT NULL,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    finding_id VARCHAR2(64 CHAR) NOT NULL,
    task_id VARCHAR2(64 CHAR) NOT NULL,
    decision VARCHAR2(32 CHAR) NOT NULL,
    comment VARCHAR2(2048 CHAR),
    evidence_ref VARCHAR2(256 CHAR),
    reviewer_id VARCHAR2(64 CHAR) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    updated_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_rect_review_id UNIQUE (tenant_id, review_id),
    CONSTRAINT ck_rect_review_decision CHECK (decision IN ('APPROVED', 'RETURNED', 'WAIVED'))
);

CREATE INDEX idx_rect_review_finding ON rectification_review (tenant_id, finding_id, reviewed_at);

CREATE TABLE evaluation_idempotency_key (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id VARCHAR2(64 CHAR) NOT NULL,
    idem_key VARCHAR2(128 CHAR) NOT NULL,
    operation_type VARCHAR2(32 CHAR) NOT NULL,
    finding_id VARCHAR2(64 CHAR) NOT NULL,
    task_id VARCHAR2(64 CHAR) NOT NULL,
    review_id VARCHAR2(64 CHAR),
    request_digest VARCHAR2(128 CHAR) NOT NULL,
    finding_status VARCHAR2(32 CHAR) NOT NULL,
    task_status VARCHAR2(32 CHAR) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(64 CHAR) NOT NULL,
    trace_id VARCHAR2(64 CHAR),
    CONSTRAINT uk_eval_idempotency_operation_key UNIQUE (tenant_id, operation_type, idem_key),
    CONSTRAINT ck_eval_idempotency_operation CHECK (operation_type IN ('RECTIFICATION_SUBMIT', 'RECTIFICATION_REVIEW')),
    CONSTRAINT ck_eval_idempotency_finding_status CHECK (finding_status IN ('NEW', 'ASSIGNED', 'REMEDIATING', 'CLOSED', 'WAIVED')),
    CONSTRAINT ck_eval_idempotency_task_status CHECK (task_status IN ('ASSIGNED', 'SUBMITTED', 'RETURNED', 'CLOSED', 'WAIVED'))
);

CREATE INDEX idx_eval_idempotency_resource
    ON evaluation_idempotency_key (tenant_id, finding_id, operation_type, created_at);
