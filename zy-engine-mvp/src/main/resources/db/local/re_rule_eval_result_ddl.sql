-- RE_RULE_EVAL_RESULT 表 DDL (H2 本地开发库)
-- 规则评估结果表，支持跨实例查询和持久化

-- 创建表
CREATE TABLE IF NOT EXISTS re_rule_eval_result (
  id BIGINT PRIMARY KEY,
  eval_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_version VARCHAR(32),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  hit_flag BOOLEAN NOT NULL,
  severity VARCHAR(32),
  message VARCHAR(1000),
  actions CLOB,
  evidence CLOB,
  input_snapshot CLOB,
  output_snapshot CLOB,
  elapsed_ms INT,
  result_status VARCHAR(32) NOT NULL,
  error_code VARCHAR(64),
  error_message VARCHAR(1000),
  tenant_id VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  org_source VARCHAR(32),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_re_rule_eval_result UNIQUE (eval_id, rule_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_re_eval_result_eval_id ON re_rule_eval_result (eval_id);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_rule ON re_rule_eval_result (rule_code, rule_version);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_patient ON re_rule_eval_result (patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_org ON re_rule_eval_result (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_time ON re_rule_eval_result (created_time);
