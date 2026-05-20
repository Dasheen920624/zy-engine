-- 知识包表
CREATE TABLE knowledge_package (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  package_code VARCHAR2(64) NOT NULL,
  package_name VARCHAR2(200) NOT NULL,
  package_version VARCHAR2(32) DEFAULT '1.0.0',
  description VARCHAR2(2000),
  export_type VARCHAR2(16) DEFAULT 'FULL',
  status VARCHAR2(16) DEFAULT 'DRAFT' NOT NULL,
  source_tenant_id VARCHAR2(64),
  source_tenant_name VARCHAR2(200),
  target_tenant_id VARCHAR2(64),
  target_tenant_name VARCHAR2(200),
  rule_count NUMBER(10) DEFAULT 0,
  terminology_count NUMBER(10) DEFAULT 0,
  pathway_count NUMBER(10) DEFAULT 0,
  graph_count NUMBER(10) DEFAULT 0,
  source_count NUMBER(10) DEFAULT 0,
  content_hash VARCHAR2(128),
  content_json CLOB,
  conflict_strategy VARCHAR2(16) DEFAULT 'SKIP',
  sync_mode VARCHAR2(16) DEFAULT 'MANUAL',
  sync_status VARCHAR2(16) DEFAULT 'IDLE',
  sync_error VARCHAR2(2000),
  sync_time TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_knowledge_package UNIQUE (tenant_id, package_code, package_version)
);

COMMENT ON TABLE knowledge_package IS '知识包';
COMMENT ON COLUMN knowledge_package.export_type IS '导出类型: FULL/INCREMENTAL';
COMMENT ON COLUMN knowledge_package.status IS '状态: DRAFT/PUBLISHED/IMPORTED/ARCHIVED';
COMMENT ON COLUMN knowledge_package.conflict_strategy IS '冲突策略: SKIP/OVERWRITE/MERGE';
COMMENT ON COLUMN knowledge_package.sync_mode IS '同步模式: MANUAL/AUTO';
COMMENT ON COLUMN knowledge_package.sync_status IS '同步状态: IDLE/SYNCING/SUCCESS/FAILED';

CREATE INDEX IF NOT EXISTS idx_knowledge_package_tenant ON knowledge_package(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_knowledge_package_sync ON knowledge_package(tenant_id, sync_status);
