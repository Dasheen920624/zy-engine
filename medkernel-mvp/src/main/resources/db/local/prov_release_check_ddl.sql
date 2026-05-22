-- PROV-004 发布检查阻断 DDL (H2 本地开发库)

-- 发布检查清单表
CREATE TABLE IF NOT EXISTS prov_release_checklist (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  checklist_code VARCHAR(64) NOT NULL,
  checklist_name VARCHAR(200) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  check_items VARCHAR(4000),
  blocking_rules VARCHAR(4000),
  approval_required VARCHAR(5) DEFAULT 'FALSE',
  approval_roles VARCHAR(500),
  enabled VARCHAR(5) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_prov_release_checklist UNIQUE (tenant_id, checklist_code)
);

-- 发布检查结果表
CREATE TABLE IF NOT EXISTS prov_release_check_result (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  check_code VARCHAR(64) NOT NULL,
  checklist_code VARCHAR(64),
  checklist_name VARCHAR(200),
  resource_type VARCHAR(32) NOT NULL,
  resource_code VARCHAR(128) NOT NULL,
  resource_version VARCHAR(32),
  check_status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
  check_detail VARCHAR(4000),
  total_items INTEGER DEFAULT 0,
  passed_items INTEGER DEFAULT 0,
  failed_items INTEGER DEFAULT 0,
  blocked_items INTEGER DEFAULT 0,
  blocked_reason VARCHAR(2000),
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  approval_note VARCHAR(2000),
  waived_by VARCHAR(64),
  waive_reason VARCHAR(2000),
  checked_by VARCHAR(64),
  checked_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_prov_release_check_result UNIQUE (tenant_id, check_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_prov_rc_result_tenant_status ON prov_release_check_result (tenant_id, resource_type, check_status);
CREATE INDEX IF NOT EXISTS idx_prov_rc_result_resource ON prov_release_check_result (tenant_id, resource_code, resource_version);
