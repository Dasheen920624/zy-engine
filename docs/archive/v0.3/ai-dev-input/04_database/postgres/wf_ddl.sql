-- PostgreSQL WF DDL for unified workflow and approval
-- 适用 PostgreSQL 14/15/16 与 KingbaseES V8（PG 兼容模式）
-- 命名约定：wf_* workflow

-- 待办任务表
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
  metadata_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_todo_task UNIQUE (tenant_id, task_code)
);

COMMENT ON TABLE wf_todo_task IS '统一待办任务表';
COMMENT ON COLUMN wf_todo_task.business_type IS '业务类型：REVIEW/PUBLISH/ROLLBACK/RECTIFY/KNOWLEDGE/COMPLIANCE/SYNC';
COMMENT ON COLUMN wf_todo_task.priority IS '优先级：URGENT/HIGH/NORMAL/LOW';
COMMENT ON COLUMN wf_todo_task.status IS '状态：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED';
COMMENT ON COLUMN wf_todo_task.assigned_type IS '分配类型：USER/ROLE/GROUP';

-- 审批操作记录表
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
  detail_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_todo_task(id)
);

COMMENT ON TABLE wf_approval_action IS '审批操作记录表';
COMMENT ON COLUMN wf_approval_action.action_type IS '操作类型：APPROVE/REJECT/DELEGATE/CANCEL/EXPIRE/ADD_SIGN';

-- 审批规则配置表
CREATE TABLE IF NOT EXISTS wf_approval_rule (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR2(128) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  approval_type VARCHAR(32) NOT NULL,
  approver_type VARCHAR(32) NOT NULL,
  approver_value VARCHAR(200) NOT NULL,
  timeout_hours INT,
  timeout_action VARCHAR(32),
  priority INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_approval_rule UNIQUE (tenant_id, rule_code)
);

COMMENT ON TABLE wf_approval_rule IS '审批规则配置表';
COMMENT ON COLUMN wf_approval_rule.approval_type IS '审批类型：SINGLE/MULTI/SEQUENTIAL';

-- 索引
CREATE INDEX IF NOT EXISTS idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX IF NOT EXISTS idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX IF NOT EXISTS idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX IF NOT EXISTS idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);
