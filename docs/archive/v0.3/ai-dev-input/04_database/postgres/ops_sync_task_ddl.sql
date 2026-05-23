-- GRAPH-005: 同步任务表（Neo4j 图谱同步等）
-- PostgreSQL DDL for production database.
-- 与 OPS-002 共用表结构，GRAPH-005 优先落地图谱同步场景。

CREATE TABLE IF NOT EXISTS ops_sync_task (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  task_code VARCHAR(64) NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  target_system VARCHAR(32) NOT NULL,
  target_version VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  dry_run SMALLINT NOT NULL DEFAULT 0,
  total_count INTEGER NOT NULL DEFAULT 0,
  success_count INTEGER NOT NULL DEFAULT 0,
  failed_count INTEGER NOT NULL DEFAULT 0,
  skip_count INTEGER NOT NULL DEFAULT 0,
  error_message VARCHAR(2000),
  retry_count INTEGER NOT NULL DEFAULT 0,
  max_retries INTEGER NOT NULL DEFAULT 3,
  started_time TIMESTAMP,
  finished_time TIMESTAMP,
  duration_ms BIGINT,
  triggered_by VARCHAR(64),
  detail_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_ops_sync_task UNIQUE (tenant_id, task_code)
);

CREATE TABLE IF NOT EXISTS ops_sync_task_detail (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  task_id BIGINT NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  item_code VARCHAR(128) NOT NULL,
  operation VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  error_message VARCHAR(1000),
  neo4j_node_id VARCHAR(128),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_ops_sync_task_detail UNIQUE (task_id, item_type, item_code)
);

CREATE INDEX IF NOT EXISTS idx_ops_sync_task_status ON ops_sync_task (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_ops_sync_task_type ON ops_sync_task (tenant_id, task_type);
CREATE INDEX IF NOT EXISTS idx_ops_sync_task_detail_task ON ops_sync_task_detail (task_id);
