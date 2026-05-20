-- 知识包表：管理知识资产的导入导出包
CREATE TABLE IF NOT EXISTS knowledge_package (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  package_code VARCHAR(64) NOT NULL,
  package_name VARCHAR(200) NOT NULL,
  package_version VARCHAR(32) DEFAULT '1.0.0',
  description VARCHAR(2000),
  export_type VARCHAR(16) DEFAULT 'FULL',
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  source_tenant_id VARCHAR(64),
  source_tenant_name VARCHAR(200),
  target_tenant_id VARCHAR(64),
  target_tenant_name VARCHAR(200),
  rule_count INTEGER DEFAULT 0,
  terminology_count INTEGER DEFAULT 0,
  pathway_count INTEGER DEFAULT 0,
  graph_count INTEGER DEFAULT 0,
  source_count INTEGER DEFAULT 0,
  content_hash VARCHAR(128),
  content_json VARCHAR(4000),
  conflict_strategy VARCHAR(16) DEFAULT 'SKIP',
  sync_mode VARCHAR(16) DEFAULT 'MANUAL',
  sync_status VARCHAR(16) DEFAULT 'IDLE',
  sync_error VARCHAR(2000),
  sync_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_knowledge_package UNIQUE (tenant_id, package_code, package_version)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_package_tenant ON knowledge_package(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_knowledge_package_sync ON knowledge_package(tenant_id, sync_status);
