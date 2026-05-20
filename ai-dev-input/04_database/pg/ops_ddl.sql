-- PostgreSQL OPS DDL for async task management
-- 异步任务管理：统一的异步任务执行、重试、审计机制
-- 中文备注规范：表和关键字段必须有中文注释

-- 异步任务表：记录异步任务执行状态和重试信息
CREATE TABLE ops_sync_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
  retry_count INT DEFAULT 0 NOT NULL,
  max_retries INT DEFAULT 3 NOT NULL,
  error_message TEXT,
  result_summary TEXT,
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

COMMENT ON TABLE ops_sync_task IS '异步任务表：记录异步任务执行状态和重试信息';
COMMENT ON COLUMN ops_sync_task.tenant_id IS '所属租户ID';
COMMENT ON COLUMN ops_sync_task.task_code IS '任务编码，租户内唯一';
COMMENT ON COLUMN ops_sync_task.task_type IS '任务类型：KNOWLEDGE_SYNC/GRAPH_SYNC/CONFIG_SYNC/EXPORT/IMPORT';
COMMENT ON COLUMN ops_sync_task.status IS '任务状态：PENDING/RUNNING/COMPLETED/FAILED/RETRYING/CANCELLED';
COMMENT ON COLUMN ops_sync_task.retry_count IS '当前重试次数';
COMMENT ON COLUMN ops_sync_task.max_retries IS '最大重试次数，默认3';
COMMENT ON COLUMN ops_sync_task.error_message IS '错误信息（最后一次执行失败时）';
COMMENT ON COLUMN ops_sync_task.result_summary IS '执行结果摘要';
COMMENT ON COLUMN ops_sync_task.scheduled_time IS '计划执行时间';
COMMENT ON COLUMN ops_sync_task.started_time IS '实际开始时间';
COMMENT ON COLUMN ops_sync_task.completed_time IS '完成时间';
COMMENT ON COLUMN ops_sync_task.triggered_by IS '触发人（user_id 或 SCHEDULED/SYSTEM）';

-- 索引
CREATE INDEX idx_ops_sync_task_tenant ON ops_sync_task(tenant_id, status);
CREATE INDEX idx_ops_sync_task_type ON ops_sync_task(tenant_id, task_type);
CREATE INDEX idx_ops_sync_task_scheduled ON ops_sync_task(scheduled_time);
