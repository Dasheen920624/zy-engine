-- MedKernel v1.0 GA · GA-ENG-OBS-01 可观测性骨干迁移
-- 1. 全局状态历史表（所有引擎实体状态机跳转的统一历史）
-- 2. canonical_resource 加 trace_id（第二层 API-01 retrofit 用）

CREATE TABLE IF NOT EXISTS state_transition_history (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR(64)  NOT NULL,
    entity_id       VARCHAR(128) NOT NULL,
    tenant_id       VARCHAR(64)  NOT NULL,
    from_status     VARCHAR(64)  NULL,
    to_status       VARCHAR(64)  NOT NULL,
    reason          VARCHAR(128) NOT NULL,
    actor           VARCHAR(64)  NULL,
    trace_id        VARCHAR(128) NULL,
    error_code      VARCHAR(64)  NULL,
    error_class     VARCHAR(32)  NULL,
    error_message   VARCHAR(512) NULL,
    retry_count     INT          NULL,
    next_retry_at   TIMESTAMP    NULL,
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sth_error_class CHECK (error_class IS NULL OR error_class IN ('INPUT','AUTH','DATA','EXTERNAL','INTERNAL'))
);

CREATE INDEX IF NOT EXISTS idx_sth_entity       ON state_transition_history (entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_tenant_time  ON state_transition_history (tenant_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_sth_trace        ON state_transition_history (trace_id);
CREATE INDEX IF NOT EXISTS idx_sth_failed       ON state_transition_history (tenant_id, error_class, occurred_at);

ALTER TABLE canonical_resource ADD COLUMN IF NOT EXISTS trace_id VARCHAR(128) NULL;
CREATE INDEX IF NOT EXISTS idx_canonical_resource_trace ON canonical_resource (trace_id);
