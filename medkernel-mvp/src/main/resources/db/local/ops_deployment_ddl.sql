-- OPS-004: 部署包表（离线部署 + 回滚）
-- H2 local file database DDL for AI/offline development.

CREATE TABLE IF NOT EXISTS ops_deployment_package (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  package_code VARCHAR(64) NOT NULL,
  package_name VARCHAR(128) NOT NULL,
  version VARCHAR(64) NOT NULL,
  description VARCHAR(500),
  package_type VARCHAR(32) NOT NULL,
  target_environment VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'BUILDING',
  artifact_path VARCHAR(500),
  artifact_hash VARCHAR(128),
  artifact_size BIGINT DEFAULT 0 NOT NULL,
  config_snapshot CLOB,
  db_migration_scripts CLOB,
  rollback_script CLOB,
  deployed_by VARCHAR(64),
  deployed_time TIMESTAMP,
  rolled_back_by VARCHAR(64),
  rolled_back_time TIMESTAMP,
  rollback_reason VARCHAR(500),
  pre_check_result CLOB,
  post_check_result CLOB,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ops_deployment_package UNIQUE (tenant_id, package_code, version)
);

CREATE INDEX IF NOT EXISTS idx_ops_deploy_tenant_env_status ON ops_deployment_package (tenant_id, target_environment, status);
CREATE INDEX IF NOT EXISTS idx_ops_deploy_tenant_env_time ON ops_deployment_package (tenant_id, target_environment, deployed_time);
