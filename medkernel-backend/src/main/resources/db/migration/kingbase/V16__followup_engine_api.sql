-- MedKernel v1.0 GA · GA-ENG-API-09 随访 API（Kingbase）

CREATE TABLE IF NOT EXISTS followup_plan (
    id                        BIGSERIAL PRIMARY KEY,
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
    id                        BIGSERIAL PRIMARY KEY,
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
    id                        BIGSERIAL PRIMARY KEY,
    questionnaire_id          VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    task_id                   VARCHAR(64)   NOT NULL,
    form_data                 TEXT          NOT NULL,
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
    id                        BIGSERIAL PRIMARY KEY,
    event_id                  VARCHAR(64)   NOT NULL,
    tenant_id                 VARCHAR(64)   NOT NULL,
    plan_id                   VARCHAR(64)   NULL,
    event_type                VARCHAR(32)   NOT NULL,
    payload                   TEXT          NOT NULL,
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

COMMENT ON TABLE followup_plan IS '随访计划';
COMMENT ON COLUMN followup_plan.plan_id IS '计划唯一ID';
COMMENT ON COLUMN followup_plan.tenant_id IS '租户ID';
COMMENT ON COLUMN followup_plan.patient_id IS '患者ID';
COMMENT ON COLUMN followup_plan.encounter_id IS '就诊记录ID';
COMMENT ON COLUMN followup_plan.pathway_id IS '关联临床路径ID';
COMMENT ON COLUMN followup_plan.disease_code IS '疾病编码';
COMMENT ON COLUMN followup_plan.risk_level IS '风险等级';
COMMENT ON COLUMN followup_plan.status IS '状态(DRAFT,ACTIVE,COMPLETED,CANCELLED)';
COMMENT ON COLUMN followup_plan.trace_id IS '追踪ID';

COMMENT ON TABLE followup_task IS '随访任务';
COMMENT ON COLUMN followup_task.task_id IS '任务唯一ID';
COMMENT ON COLUMN followup_task.plan_id IS '所属计划ID';
COMMENT ON COLUMN followup_task.task_type IS '任务类型(QUESTIONNAIRE,EXAM,LAB,OUTPATIENT)';
COMMENT ON COLUMN followup_task.due_date IS '执行期限';
COMMENT ON COLUMN followup_task.status IS '状态(PENDING,COMPLETED,OVERDUE,CANCELLED)';
COMMENT ON COLUMN followup_task.executor_id IS '执行人ID';
COMMENT ON COLUMN followup_task.executor_type IS '执行人类型(DOCTOR,NURSE,SYSTEM)';

COMMENT ON TABLE followup_questionnaire IS '随访问卷';
COMMENT ON COLUMN followup_questionnaire.questionnaire_id IS '问卷唯一ID';
COMMENT ON COLUMN followup_questionnaire.task_id IS '关联任务ID';
COMMENT ON COLUMN followup_questionnaire.form_data IS '表单数据(JSON)';
COMMENT ON COLUMN followup_questionnaire.score IS '评分结果';

COMMENT ON TABLE followup_event IS '随访异常事件';
COMMENT ON COLUMN followup_event.event_id IS '事件唯一ID';
COMMENT ON COLUMN followup_event.event_type IS '事件类型(ABNORMAL_RETURN,RESULT_INFLOW)';
COMMENT ON COLUMN followup_event.payload IS '事件载荷数据(JSON)';
COMMENT ON COLUMN followup_event.triggered_by IS '触发方';
