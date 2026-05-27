-- MedKernel v1.0 GA · GA-ENG-OBS-01 可观测性骨干迁移
-- 1. 全局状态历史表（所有引擎实体状态机跳转的统一历史）
-- 2. canonical_resource 加 trace_id（第二层 API-01 retrofit 用）

CREATE TABLE state_transition_history (
    id              NUMBER(19)    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR2(64)  NOT NULL,
    entity_id       VARCHAR2(128) NOT NULL,
    tenant_id       VARCHAR2(64)  NOT NULL,
    from_status     VARCHAR2(64)  NULL,
    to_status       VARCHAR2(64)  NOT NULL,
    reason          VARCHAR2(128) NOT NULL,
    actor           VARCHAR2(64)  NULL,
    trace_id        VARCHAR2(128) NULL,
    error_code      VARCHAR2(64)  NULL,
    error_class     VARCHAR2(32)  NULL,
    error_message   VARCHAR2(512) NULL,
    retry_count     NUMBER(10)    NULL,
    next_retry_at   TIMESTAMP WITH TIME ZONE NULL,
    occurred_at     TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_sth_error_class CHECK (error_class IS NULL OR error_class IN ('INPUT','AUTH','DATA','EXTERNAL','INTERNAL'))
);

CREATE INDEX idx_sth_entity       ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX idx_sth_tenant_time  ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX idx_sth_trace        ON state_transition_history (trace_id);
CREATE INDEX idx_sth_failed       ON state_transition_history (tenant_id, error_class, occurred_at);

ALTER TABLE canonical_resource ADD trace_id VARCHAR2(128) NULL;
CREATE INDEX idx_canonical_resource_trace ON canonical_resource (trace_id);
