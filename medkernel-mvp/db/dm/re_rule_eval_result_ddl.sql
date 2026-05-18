-- RE_RULE_EVAL_RESULT 表 DDL (DM 达梦)
-- 规则评估结果表，支持跨实例查询和持久化

-- 创建表
CREATE TABLE IF NOT EXISTS re_rule_eval_result (
  id BIGINT PRIMARY KEY,
  eval_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_version VARCHAR(32),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  hit_flag INT NOT NULL,
  severity VARCHAR(32),
  message VARCHAR(1000),
  actions TEXT,
  evidence TEXT,
  input_snapshot TEXT,
  output_snapshot TEXT,
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

-- 添加表注释
COMMENT ON TABLE re_rule_eval_result IS '规则评估结果表，支持跨实例查询和持久化';

-- 添加字段注释
COMMENT ON COLUMN re_rule_eval_result.id IS '主键ID';
COMMENT ON COLUMN re_rule_eval_result.eval_id IS '评估ID，唯一标识一次评估';
COMMENT ON COLUMN re_rule_eval_result.rule_code IS '规则编码';
COMMENT ON COLUMN re_rule_eval_result.rule_version IS '规则版本';
COMMENT ON COLUMN re_rule_eval_result.patient_id IS '患者ID';
COMMENT ON COLUMN re_rule_eval_result.encounter_id IS '就诊ID';
COMMENT ON COLUMN re_rule_eval_result.hit_flag IS '是否命中，0=未命中，1=命中';
COMMENT ON COLUMN re_rule_eval_result.severity IS '严重程度';
COMMENT ON COLUMN re_rule_eval_result.message IS '评估消息';
COMMENT ON COLUMN re_rule_eval_result.actions IS '建议操作，JSON数组';
COMMENT ON COLUMN re_rule_eval_result.evidence IS '证据，JSON数组';
COMMENT ON COLUMN re_rule_eval_result.input_snapshot IS '输入快照，JSON';
COMMENT ON COLUMN re_rule_eval_result.output_snapshot IS '输出快照，JSON';
COMMENT ON COLUMN re_rule_eval_result.elapsed_ms IS '耗时（毫秒）';
COMMENT ON COLUMN re_rule_eval_result.result_status IS '结果状态，SUCCESS/FAIL/ERROR';
COMMENT ON COLUMN re_rule_eval_result.error_code IS '错误编码';
COMMENT ON COLUMN re_rule_eval_result.error_message IS '错误消息';
COMMENT ON COLUMN re_rule_eval_result.tenant_id IS '租户ID';
COMMENT ON COLUMN re_rule_eval_result.group_code IS '集团编码';
COMMENT ON COLUMN re_rule_eval_result.hospital_code IS '医院编码';
COMMENT ON COLUMN re_rule_eval_result.campus_code IS '院区编码';
COMMENT ON COLUMN re_rule_eval_result.site_code IS '站点编码';
COMMENT ON COLUMN re_rule_eval_result.department_code IS '科室编码';
COMMENT ON COLUMN re_rule_eval_result.scope_level IS '作用域层级';
COMMENT ON COLUMN re_rule_eval_result.scope_code IS '作用域编码';
COMMENT ON COLUMN re_rule_eval_result.org_source IS '组织来源';
COMMENT ON COLUMN re_rule_eval_result.created_time IS '创建时间';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_re_eval_result_eval_id ON re_rule_eval_result (eval_id);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_rule ON re_rule_eval_result (rule_code, rule_version);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_patient ON re_rule_eval_result (patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_org ON re_rule_eval_result (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_re_eval_result_time ON re_rule_eval_result (created_time);
