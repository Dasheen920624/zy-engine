-- PostgreSQL WF DDL for unified workflow and approval
-- 统一待办和审批工作流：审核、发布、回滚、整改、知识包、合规、同步异常统一进入待办
-- 命名约定：wf_* workflow

-- 待办任务表：统一待办入口
CREATE TABLE wf_todo_task (
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_todo_task UNIQUE (tenant_id, task_code)
);

COMMENT ON TABLE wf_todo_task IS '统一待办任务表：审核、发布、回滚、整改、知识包、合规、同步异常统一入口';
COMMENT ON COLUMN wf_todo_task.tenant_id IS '租户ID';
COMMENT ON COLUMN wf_todo_task.task_code IS '任务编码，唯一标识';
COMMENT ON COLUMN wf_todo_task.business_type IS '业务类型：REVIEW/PUBLISH/ROLLBACK/RECTIFY/KNOWLEDGE/COMPLIANCE/SYNC';
COMMENT ON COLUMN wf_todo_task.business_code IS '业务编码（如配置包编码、规则编码等）';
COMMENT ON COLUMN wf_todo_task.business_version IS '业务版本号';
COMMENT ON COLUMN wf_todo_task.title IS '待办标题';
COMMENT ON COLUMN wf_todo_task.description IS '待办描述';
COMMENT ON COLUMN wf_todo_task.priority IS '优先级：URGENT/HIGH/NORMAL/LOW';
COMMENT ON COLUMN wf_todo_task.status IS '状态：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED';
COMMENT ON COLUMN wf_todo_task.assigned_type IS '分配类型：USER/ROLE/GROUP';
COMMENT ON COLUMN wf_todo_task.assigned_to IS '分配目标（用户ID或角色编码）';
COMMENT ON COLUMN wf_todo_task.created_by IS '创建人';
COMMENT ON COLUMN wf_todo_task.due_time IS '截止时间';
COMMENT ON COLUMN wf_todo_task.completed_by IS '完成人';
COMMENT ON COLUMN wf_todo_task.completed_time IS '完成时间';
COMMENT ON COLUMN wf_todo_task.completed_comment IS '完成备注';
COMMENT ON COLUMN wf_todo_task.cancelled_by IS '取消人';
COMMENT ON COLUMN wf_todo_task.cancelled_time IS '取消时间';
COMMENT ON COLUMN wf_todo_task.cancel_reason IS '取消原因';
COMMENT ON COLUMN wf_todo_task.metadata_json IS '扩展元数据JSON';

-- 审批操作记录表：审批流程日志
CREATE TABLE wf_approval_action (
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_todo_task(id)
);

COMMENT ON TABLE wf_approval_action IS '审批操作记录表：审批流程日志';
COMMENT ON COLUMN wf_approval_action.task_id IS '关联待办任务ID';
COMMENT ON COLUMN wf_approval_action.action_type IS '操作类型：APPROVE/REJECT/DELEGATE/CANCEL/EXPIRE/ADD_SIGN';
COMMENT ON COLUMN wf_approval_action.action_result IS '操作结果：SUCCESS/FAILURE';
COMMENT ON COLUMN wf_approval_action.operator_id IS '操作人ID';
COMMENT ON COLUMN wf_approval_action.comment IS '审批意见';
COMMENT ON COLUMN wf_approval_action.delegate_to IS '转办目标用户ID';

-- 审批规则配置表：审批流程规则
CREATE TABLE wf_approval_rule (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(128) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  business_type VARCHAR(64) NOT NULL,
  approval_type VARCHAR(32) NOT NULL,
  approver_type VARCHAR(32) NOT NULL,
  approver_value VARCHAR(200) NOT NULL,
  timeout_hours INT,
  timeout_action VARCHAR(32),
  priority INT DEFAULT 0 NOT NULL,
  status VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_approval_rule UNIQUE (tenant_id, rule_code)
);

COMMENT ON TABLE wf_approval_rule IS '审批规则配置表：审批流程规则';
COMMENT ON COLUMN wf_approval_rule.rule_code IS '规则编码';
COMMENT ON COLUMN wf_approval_rule.business_type IS '业务类型：REVIEW/PUBLISH/ROLLBACK/RECTIFY/KNOWLEDGE/COMPLIANCE/SYNC';
COMMENT ON COLUMN wf_approval_rule.approval_type IS '审批类型：SINGLE/MULTI/SEQUENTIAL';
COMMENT ON COLUMN wf_approval_rule.approver_type IS '审批人类型：USER/ROLE/DEPARTMENT_HEAD';
COMMENT ON COLUMN wf_approval_rule.approver_value IS '审批人值（用户ID或角色编码）';
COMMENT ON COLUMN wf_approval_rule.timeout_hours IS '超时时间（小时）';
COMMENT ON COLUMN wf_approval_rule.timeout_action IS '超时动作：AUTO_APPROVE/AUTO_REJECT/NOTIFY';

-- 索引
CREATE INDEX idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);
