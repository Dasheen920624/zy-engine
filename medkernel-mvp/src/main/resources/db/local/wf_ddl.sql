-- H2 local file database DDL for workflow tables
-- 统一待办和审批工作流
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

CREATE TABLE IF NOT EXISTS wf_todo_task (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  task_code VARCHAR(128) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  business_code VARCHAR(128) NOT NULL,
  business_version VARCHAR(64),
  title VARCHAR(500) NOT NULL,
  description VARCHAR(2000),
  priority VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  assigned_type VARCHAR(32) NOT NULL,
  assigned_to VARCHAR(128),
  created_by VARCHAR(64) NOT NULL,
  due_time TIMESTAMP,
  completed_by VARCHAR(64),
  completed_time TIMESTAMP,
  completed_comment VARCHAR(1000),
  cancelled_by VARCHAR(64),
  cancelled_time TIMESTAMP,
  cancel_reason VARCHAR(1000),
  tenant_code VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  metadata_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_todo_task UNIQUE (tenant_id, task_code)
);

CREATE TABLE IF NOT EXISTS wf_approval_action (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  task_id BIGINT NOT NULL,
  task_code VARCHAR(128) NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  action_result VARCHAR(32) NOT NULL,
  operator_id VARCHAR(64) NOT NULL,
  operator_name VARCHAR(100),
  comment VARCHAR(2000),
  delegate_to VARCHAR(64),
  delegate_to_name VARCHAR(100),
  tenant_code VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_todo_task(id)
);

CREATE TABLE IF NOT EXISTS wf_approval_rule (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(128) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  approval_type VARCHAR(32) NOT NULL,
  approver_type VARCHAR(32) NOT NULL,
  approver_value VARCHAR(200) NOT NULL,
  timeout_hours INTEGER,
  timeout_action VARCHAR(32),
  priority INTEGER DEFAULT 0 NOT NULL,
  status VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_approval_rule UNIQUE (tenant_id, rule_code)
);

CREATE INDEX IF NOT EXISTS idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX IF NOT EXISTS idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX IF NOT EXISTS idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX IF NOT EXISTS idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);
