-- IMPL-001: 客户实施、培训和试运行包
-- H2 local file database DDL for AI/offline development.

-- 实施清单模板
CREATE TABLE IF NOT EXISTS impl_checklist_template (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  phase VARCHAR(32),
  category VARCHAR(64),
  is_active BOOLEAN DEFAULT TRUE NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_chk_template UNIQUE (tenant_id, template_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_chk_template_tenant ON impl_checklist_template (tenant_id, phase, is_active);

-- 清单检查项
CREATE TABLE IF NOT EXISTS impl_checklist_item (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  template_id BIGINT NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  sort_order INT DEFAULT 0 NOT NULL,
  is_required BOOLEAN DEFAULT TRUE NOT NULL,
  check_method VARCHAR(256),
  expected_result VARCHAR(500),
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
  checked_by VARCHAR(64),
  checked_time TIMESTAMP,
  remark VARCHAR(500),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_chk_item UNIQUE (tenant_id, template_id, item_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_chk_item_template ON impl_checklist_item (tenant_id, template_id, sort_order);

-- 培训材料
CREATE TABLE IF NOT EXISTS impl_training_material (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  material_type VARCHAR(32) NOT NULL,
  category VARCHAR(64),
  content_path VARCHAR(500),
  version VARCHAR(32),
  duration_minutes INT,
  target_audience VARCHAR(256),
  is_published BOOLEAN DEFAULT FALSE NOT NULL,
  published_by VARCHAR(64),
  published_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_training_material UNIQUE (tenant_id, material_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_training_material_tenant ON impl_training_material (tenant_id, category, is_published);

-- 演示数据包
CREATE TABLE IF NOT EXISTS impl_demo_data_package (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  package_code VARCHAR(64) NOT NULL,
  package_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  data_scope VARCHAR(256),
  data_version VARCHAR(32),
  record_count BIGINT DEFAULT 0 NOT NULL,
  artifact_path VARCHAR(500),
  artifact_hash VARCHAR(128),
  status VARCHAR(32) DEFAULT 'DRAFT' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_demo_data_package UNIQUE (tenant_id, package_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_demo_data_tenant ON impl_demo_data_package (tenant_id, status);

-- 试运行计划
CREATE TABLE IF NOT EXISTS impl_trial_plan (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  plan_code VARCHAR(64) NOT NULL,
  plan_name VARCHAR(128) NOT NULL,
  description VARCHAR(500),
  start_date DATE,
  end_date DATE,
  scope VARCHAR(256),
  objectives CLOB,
  responsible_person VARCHAR(64),
  status VARCHAR(32) DEFAULT 'DRAFT' NOT NULL,
  approval_status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_trial_plan UNIQUE (tenant_id, plan_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_trial_plan_tenant ON impl_trial_plan (tenant_id, status, approval_status);

-- 试运行记录
CREATE TABLE IF NOT EXISTS impl_trial_record (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  plan_id BIGINT NOT NULL,
  record_date DATE NOT NULL,
  participant_count INT DEFAULT 0 NOT NULL,
  issue_count INT DEFAULT 0 NOT NULL,
  resolved_count INT DEFAULT 0 NOT NULL,
  summary CLOB,
  metrics_json CLOB,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_impl_trial_record_plan ON impl_trial_record (tenant_id, plan_id, record_date);

-- 问题反馈
CREATE TABLE IF NOT EXISTS impl_issue_feedback (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  plan_id BIGINT,
  issue_code VARCHAR(64) NOT NULL,
  title VARCHAR(256) NOT NULL,
  description CLOB,
  issue_type VARCHAR(32) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  status VARCHAR(32) DEFAULT 'OPEN' NOT NULL,
  reported_by VARCHAR(64),
  reported_time TIMESTAMP,
  assigned_to VARCHAR(64),
  resolved_by VARCHAR(64),
  resolved_time TIMESTAMP,
  resolution CLOB,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_impl_issue_feedback UNIQUE (tenant_id, issue_code)
);

CREATE INDEX IF NOT EXISTS idx_impl_issue_tenant ON impl_issue_feedback (tenant_id, plan_id, status, severity);
