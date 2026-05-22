-- SEC-002 数据权限 DDL (H2 local file database)
-- 数据权限策略表 + 数据权限分配表
-- H2-compatible syntax: BIGINT, VARCHAR, CLOB, CURRENT_TIMESTAMP

-- 数据权限策略表
CREATE TABLE IF NOT EXISTS sec_data_permission_policy (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  policy_code VARCHAR(64) NOT NULL,
  policy_name VARCHAR(200) NOT NULL,
  policy_type VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  scope_expression CLOB,
  filter_expression CLOB,
  priority VARCHAR(16),
  enabled VARCHAR(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_dp_policy UNIQUE (tenant_id, policy_code)
);

-- 数据权限分配表
CREATE TABLE IF NOT EXISTS sec_data_permission_assignment (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  assignment_code VARCHAR(64) NOT NULL,
  principal_type VARCHAR(32) NOT NULL,
  principal_code VARCHAR(64) NOT NULL,
  principal_name VARCHAR(200),
  policy_id BIGINT NOT NULL,
  policy_code VARCHAR(64),
  policy_name VARCHAR(200),
  resource_type VARCHAR(32) NOT NULL,
  effect VARCHAR(16) NOT NULL,
  conditions CLOB,
  enabled VARCHAR(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_dp_assignment UNIQUE (tenant_id, assignment_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sec_dp_assignment_principal ON sec_data_permission_assignment (tenant_id, principal_type, principal_code);
CREATE INDEX IF NOT EXISTS idx_sec_dp_assignment_resource ON sec_data_permission_assignment (tenant_id, resource_type, effect);
CREATE INDEX IF NOT EXISTS idx_sec_dp_policy_tenant_type ON sec_data_permission_policy (tenant_id, policy_type, enabled);
