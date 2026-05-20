-- H2 OPS DDL for async task management
-- 异步任务管理：统一的异步任务执行、重试、审计机制

-- 异步任务表：记录异步任务执行状态和重试信息
CREATE TABLE IF NOT EXISTS ops_sync_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
  retry_count INT DEFAULT 0 NOT NULL,
  max_retries INT DEFAULT 3 NOT NULL,
  error_message CLOB,
  result_summary CLOB,
  scheduled_time TIMESTAMP,
  started_time TIMESTAMP,
  completed_time TIMESTAMP,
  triggered_by VARCHAR(64),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ops_sync_task UNIQUE (tenant_id, task_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_ops_sync_task_tenant ON ops_sync_task(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_ops_sync_task_type ON ops_sync_task(tenant_id, task_type);
CREATE INDEX IF NOT EXISTS idx_ops_sync_task_scheduled ON ops_sync_task(scheduled_time);
