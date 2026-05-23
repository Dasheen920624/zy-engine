-- SEC-002 数据权限 DDL (PostgreSQL/KingbaseES)
-- 数据权限策略表 + 数据权限分配表

-- 数据权限策略表
CREATE TABLE IF NOT EXISTS sec_data_permission_policy (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  policy_code VARCHAR(64) NOT NULL,
  policy_name VARCHAR(200) NOT NULL,
  policy_type VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  scope_expression TEXT,
  filter_expression TEXT,
  priority VARCHAR(16),
  enabled VARCHAR(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_dp_policy UNIQUE (tenant_id, policy_code)
);

COMMENT ON TABLE sec_data_permission_policy IS '数据权限策略表';
COMMENT ON COLUMN sec_data_permission_policy.policy_code IS '策略编码';
COMMENT ON COLUMN sec_data_permission_policy.policy_name IS '策略名称';
COMMENT ON COLUMN sec_data_permission_policy.policy_type IS '策略类型: DEPARTMENT/WARD/PATIENT_GROUP/DATA_CATEGORY/CUSTOM';
COMMENT ON COLUMN sec_data_permission_policy.description IS '描述';
COMMENT ON COLUMN sec_data_permission_policy.scope_expression IS '权限范围表达式 JSON';
COMMENT ON COLUMN sec_data_permission_policy.filter_expression IS '数据过滤表达式 JSON';
COMMENT ON COLUMN sec_data_permission_policy.priority IS '优先级';
COMMENT ON COLUMN sec_data_permission_policy.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN sec_data_permission_policy.created_by IS '创建人';
COMMENT ON COLUMN sec_data_permission_policy.created_time IS '创建时间';
COMMENT ON COLUMN sec_data_permission_policy.updated_by IS '更新人';
COMMENT ON COLUMN sec_data_permission_policy.updated_time IS '更新时间';

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
  conditions TEXT,
  enabled VARCHAR(2) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_dp_assignment UNIQUE (tenant_id, assignment_code)
);

COMMENT ON TABLE sec_data_permission_assignment IS '数据权限分配表';
COMMENT ON COLUMN sec_data_permission_assignment.assignment_code IS '分配编码';
COMMENT ON COLUMN sec_data_permission_assignment.principal_type IS '主体类型: USER/ROLE/DEPARTMENT/POSITION';
COMMENT ON COLUMN sec_data_permission_assignment.principal_code IS '主体编码';
COMMENT ON COLUMN sec_data_permission_assignment.principal_name IS '主体名称';
COMMENT ON COLUMN sec_data_permission_assignment.policy_id IS '关联策略ID';
COMMENT ON COLUMN sec_data_permission_assignment.policy_code IS '关联策略编码';
COMMENT ON COLUMN sec_data_permission_assignment.policy_name IS '关联策略名称';
COMMENT ON COLUMN sec_data_permission_assignment.resource_type IS '资源类型: PATIENT/ORDER/PATHWAY/RULE/KNOWLEDGE/ALL';
COMMENT ON COLUMN sec_data_permission_assignment.effect IS '效果: ALLOW/DENY';
COMMENT ON COLUMN sec_data_permission_assignment.conditions IS '条件表达式 JSON';
COMMENT ON COLUMN sec_data_permission_assignment.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN sec_data_permission_assignment.created_by IS '创建人';
COMMENT ON COLUMN sec_data_permission_assignment.created_time IS '创建时间';
COMMENT ON COLUMN sec_data_permission_assignment.updated_by IS '更新人';
COMMENT ON COLUMN sec_data_permission_assignment.updated_time IS '更新时间';

-- 索引
CREATE INDEX IF NOT EXISTS idx_sec_dp_assignment_principal ON sec_data_permission_assignment (tenant_id, principal_type, principal_code);
CREATE INDEX IF NOT EXISTS idx_sec_dp_assignment_resource ON sec_data_permission_assignment (tenant_id, resource_type, effect);
CREATE INDEX IF NOT EXISTS idx_sec_dp_policy_tenant_type ON sec_data_permission_policy (tenant_id, policy_type, enabled);
