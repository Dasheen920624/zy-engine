-- H2 local file database DDL for workflow tables
-- 统一待办和审批工作流

CREATE TABLE IF NOT EXISTS wf_todo_task (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  task_code VARCHAR2(128) NOT NULL,
  business_type VARCHAR2(64) NOT NULL,
  business_code VARCHAR2(128) NOT NULL,
  business_version VARCHAR2(64),
  title VARCHAR2(500) NOT NULL,
  description VARCHAR2(2000),
  priority VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  assigned_type VARCHAR2(32) NOT NULL,
  assigned_to VARCHAR2(128),
  created_by VARCHAR2(64) NOT NULL,
  due_time TIMESTAMP,
  completed_by VARCHAR2(64),
  completed_time TIMESTAMP,
  completed_comment VARCHAR2(1000),
  cancelled_by VARCHAR2(64),
  cancelled_time TIMESTAMP,
  cancel_reason VARCHAR2(1000),
  tenant_code VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  metadata_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_todo_task UNIQUE (tenant_id, task_code)
);

CREATE TABLE IF NOT EXISTS wf_approval_action (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  task_id NUMBER(20) NOT NULL,
  task_code VARCHAR2(128) NOT NULL,
  action_type VARCHAR2(32) NOT NULL,
  action_result VARCHAR2(32) NOT NULL,
  operator_id VARCHAR2(64) NOT NULL,
  operator_name VARCHAR2(100),
  comment VARCHAR2(2000),
  delegate_to VARCHAR2(64),
  delegate_to_name VARCHAR2(100),
  tenant_code VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_todo_task(id)
);

CREATE TABLE IF NOT EXISTS wf_approval_rule (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  rule_code VARCHAR2(128) NOT NULL,
  rule_name VARCHAR2(200) NOT NULL,
  business_type VARCHAR2(64) NOT NULL,
  approval_type VARCHAR2(32) NOT NULL,
  approver_type VARCHAR2(32) NOT NULL,
  approver_value VARCHAR2(200) NOT NULL,
  timeout_hours NUMBER(10),
  timeout_action VARCHAR2(32),
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  status VARCHAR2(32) NOT NULL,
  description VARCHAR2(500),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_approval_rule UNIQUE (tenant_id, rule_code)
);

CREATE INDEX IF NOT EXISTS idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX IF NOT EXISTS idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX IF NOT EXISTS idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX IF NOT EXISTS idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);
