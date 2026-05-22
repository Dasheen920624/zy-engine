-- PKG-005: 配置包回滚记录表
-- H2 local file database DDL for AI/offline development.

CREATE TABLE IF NOT EXISTS cfg_package_rollback_record (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64),
  package_code VARCHAR(128),
  package_version VARCHAR(64),
  target_version VARCHAR(64),
  rollback_type VARCHAR(32),
  status VARCHAR(32) DEFAULT 'PENDING',
  pre_check_result CLOB,
  post_check_result CLOB,
  snapshot_before CLOB,
  snapshot_after CLOB,
  rollback_reason VARCHAR(512),
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  rolled_back_by VARCHAR(64),
  rolled_back_time TIMESTAMP,
  completed_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cfg_rollback_tenant_pkg ON cfg_package_rollback_record (tenant_id, package_code);
CREATE INDEX IF NOT EXISTS idx_cfg_rollback_tenant_status ON cfg_package_rollback_record (tenant_id, status);
