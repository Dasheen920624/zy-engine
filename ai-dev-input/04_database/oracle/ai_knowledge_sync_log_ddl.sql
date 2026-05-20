-- AI 知识同步日志表：记录知识来源同步历史、差异和审核状态
-- 支持定时同步、手动同步、差异预览、失败重试、前台审核和驳回

CREATE TABLE ai_knowledge_sync_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  sync_code VARCHAR2(64) NOT NULL,
  source_code VARCHAR2(64) NOT NULL,
  subscription_id VARCHAR2(64),
  sync_type VARCHAR2(32) NOT NULL,
  sync_mode VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  diff_summary CLOB,
  diff_detail CLOB,
  items_added NUMBER(10) DEFAULT 0,
  items_updated NUMBER(10) DEFAULT 0,
  items_deleted NUMBER(10) DEFAULT 0,
  items_total NUMBER(10) DEFAULT 0,
  review_status VARCHAR2(32) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(2000),
  ops_task_id NUMBER(19),
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  started_time TIMESTAMP,
  completed_time TIMESTAMP,
  duration_ms NUMBER(10),
  triggered_by VARCHAR2(64),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_knowledge_sync_log UNIQUE (tenant_id, sync_code)
);

COMMENT ON TABLE ai_knowledge_sync_log IS 'AI知识同步日志';
COMMENT ON COLUMN ai_knowledge_sync_log.sync_type IS '同步类型: AUTO/MANUAL';
COMMENT ON COLUMN ai_knowledge_sync_log.sync_mode IS '同步模式: FULL/INCREMENTAL/DRY_RUN';
COMMENT ON COLUMN ai_knowledge_sync_log.status IS '状态: PENDING/RUNNING/DIFF_READY/APPROVED/SYNCING/COMPLETED/FAILED/CANCELLED';
COMMENT ON COLUMN ai_knowledge_sync_log.review_status IS '审核状态: PENDING/APPROVED/REJECTED';

-- 索引
CREATE INDEX idx_aik_sync_log_tenant ON ai_knowledge_sync_log(tenant_id, status);
CREATE INDEX idx_aik_sync_log_source ON ai_knowledge_sync_log(tenant_id, source_code);
CREATE INDEX idx_aik_sync_log_review ON ai_knowledge_sync_log(tenant_id, review_status);
CREATE INDEX idx_aik_sync_log_ops ON ai_knowledge_sync_log(ops_task_id);
