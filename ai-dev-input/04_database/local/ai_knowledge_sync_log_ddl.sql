-- H2 AI 知识同步日志表
-- 记录知识来源同步历史、差异和审核状态

CREATE TABLE IF NOT EXISTS ai_knowledge_sync_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sync_code VARCHAR(64) NOT NULL,
  source_code VARCHAR(64) NOT NULL,
  subscription_id VARCHAR(64),
  sync_type VARCHAR(32) NOT NULL,
  sync_mode VARCHAR(32) NOT NULL,
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
  diff_summary CLOB,
  diff_detail CLOB,
  items_added INT DEFAULT 0,
  items_updated INT DEFAULT 0,
  items_deleted INT DEFAULT 0,
  items_total INT DEFAULT 0,
  review_status VARCHAR(32) DEFAULT 'PENDING',
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR(2000),
  ops_task_id BIGINT,
  error_code VARCHAR(64),
  error_message VARCHAR(2000),
  started_time TIMESTAMP,
  completed_time TIMESTAMP,
  duration_ms INT,
  triggered_by VARCHAR(64),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_knowledge_sync_log UNIQUE (tenant_id, sync_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_aik_sync_log_tenant ON ai_knowledge_sync_log(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_aik_sync_log_source ON ai_knowledge_sync_log(tenant_id, source_code);
CREATE INDEX IF NOT EXISTS idx_aik_sync_log_review ON ai_knowledge_sync_log(tenant_id, review_status);
CREATE INDEX IF NOT EXISTS idx_aik_sync_log_ops ON ai_knowledge_sync_log(ops_task_id);
