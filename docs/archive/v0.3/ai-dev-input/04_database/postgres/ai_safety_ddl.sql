-- AI-GOV-002 AI安全红队和幻觉防护 DDL (PostgreSQL/KingbaseES)

-- 红队测试场景
CREATE TABLE IF NOT EXISTS ai_red_team_scenario (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  scenario_code VARCHAR(64) NOT NULL,
  scenario_name VARCHAR(200) NOT NULL,
  category VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  attack_prompt VARCHAR(4000),
  expected_behavior VARCHAR(2000),
  severity VARCHAR(16),
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  enabled VARCHAR(1) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_red_team_scenario UNIQUE (tenant_id, scenario_code)
);

COMMENT ON TABLE ai_red_team_scenario IS '红队测试场景';
COMMENT ON COLUMN ai_red_team_scenario.scenario_code IS '场景编码';
COMMENT ON COLUMN ai_red_team_scenario.scenario_name IS '场景名称';
COMMENT ON COLUMN ai_red_team_scenario.category IS '分类: PROMPT_INJECTION/DATA_LEAKAGE/HALLUCINATION/BIAS/SAFETY_BYPASS/MEDICAL_ERROR';
COMMENT ON COLUMN ai_red_team_scenario.description IS '描述';
COMMENT ON COLUMN ai_red_team_scenario.attack_prompt IS '攻击提示词';
COMMENT ON COLUMN ai_red_team_scenario.expected_behavior IS '期望行为';
COMMENT ON COLUMN ai_red_team_scenario.severity IS '严重程度: LOW/MEDIUM/HIGH/CRITICAL';
COMMENT ON COLUMN ai_red_team_scenario.status IS '状态: DRAFT/ACTIVE/DISABLED';
COMMENT ON COLUMN ai_red_team_scenario.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN ai_red_team_scenario.created_by IS '创建人';
COMMENT ON COLUMN ai_red_team_scenario.created_time IS '创建时间';
COMMENT ON COLUMN ai_red_team_scenario.updated_by IS '更新人';
COMMENT ON COLUMN ai_red_team_scenario.updated_time IS '更新时间';

-- 红队测试结果
CREATE TABLE IF NOT EXISTS ai_red_team_result (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  result_code VARCHAR(64) NOT NULL,
  scenario_id BIGINT NOT NULL,
  scenario_code VARCHAR(64),
  scenario_name VARCHAR(200),
  category VARCHAR(32),
  model_code VARCHAR(64),
  model_version VARCHAR(32),
  prompt_template_code VARCHAR(64),
  actual_response TEXT,
  verdict VARCHAR(16),
  vulnerability_type VARCHAR(64),
  vulnerability_detail VARCHAR(2000),
  remediation VARCHAR(2000),
  severity VARCHAR(16),
  executed_by VARCHAR(64),
  executed_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_ai_red_team_result UNIQUE (tenant_id, result_code)
);

COMMENT ON TABLE ai_red_team_result IS '红队测试结果';
COMMENT ON COLUMN ai_red_team_result.result_code IS '结果编码';
COMMENT ON COLUMN ai_red_team_result.scenario_id IS '场景ID';
COMMENT ON COLUMN ai_red_team_result.scenario_code IS '场景编码';
COMMENT ON COLUMN ai_red_team_result.scenario_name IS '场景名称';
COMMENT ON COLUMN ai_red_team_result.category IS '分类';
COMMENT ON COLUMN ai_red_team_result.model_code IS '模型编码';
COMMENT ON COLUMN ai_red_team_result.model_version IS '模型版本';
COMMENT ON COLUMN ai_red_team_result.prompt_template_code IS '提示词模板编码';
COMMENT ON COLUMN ai_red_team_result.actual_response IS '实际响应';
COMMENT ON COLUMN ai_red_team_result.verdict IS '判定: PASS/FAIL/UNCERTAIN';
COMMENT ON COLUMN ai_red_team_result.vulnerability_type IS '漏洞类型';
COMMENT ON COLUMN ai_red_team_result.vulnerability_detail IS '漏洞详情';
COMMENT ON COLUMN ai_red_team_result.remediation IS '修复建议';
COMMENT ON COLUMN ai_red_team_result.severity IS '严重程度';
COMMENT ON COLUMN ai_red_team_result.executed_by IS '执行人';
COMMENT ON COLUMN ai_red_team_result.executed_time IS '执行时间';
COMMENT ON COLUMN ai_red_team_result.created_time IS '创建时间';

-- 幻觉检测记录
CREATE TABLE IF NOT EXISTS ai_hallucination_detection (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  detection_code VARCHAR(64) NOT NULL,
  model_code VARCHAR(64),
  model_version VARCHAR(32),
  prompt_template_code VARCHAR(64),
  input_content TEXT,
  output_content TEXT,
  detection_type VARCHAR(32),
  confidence_score DECIMAL(5,4),
  verdict VARCHAR(32),
  evidence TEXT,
  protection_action VARCHAR(32),
  reviewer VARCHAR(64),
  review_time TIMESTAMP,
  review_note VARCHAR(2000),
  status VARCHAR(16) DEFAULT 'DETECTED' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_hallucination_detection UNIQUE (tenant_id, detection_code)
);

COMMENT ON TABLE ai_hallucination_detection IS '幻觉检测记录';
COMMENT ON COLUMN ai_hallucination_detection.detection_code IS '检测编码';
COMMENT ON COLUMN ai_hallucination_detection.model_code IS '模型编码';
COMMENT ON COLUMN ai_hallucination_detection.model_version IS '模型版本';
COMMENT ON COLUMN ai_hallucination_detection.prompt_template_code IS '提示词模板编码';
COMMENT ON COLUMN ai_hallucination_detection.input_content IS '输入内容';
COMMENT ON COLUMN ai_hallucination_detection.output_content IS '输出内容';
COMMENT ON COLUMN ai_hallucination_detection.detection_type IS '检测类型: FACTUAL/LOGICAL/CONTEXTUAL/REFERENTIAL';
COMMENT ON COLUMN ai_hallucination_detection.confidence_score IS '置信度分数';
COMMENT ON COLUMN ai_hallucination_detection.verdict IS '判定: HALLUCINATION/LIKELY_HALLUCINATION/UNCERTAIN/SAFE';
COMMENT ON COLUMN ai_hallucination_detection.evidence IS '证据';
COMMENT ON COLUMN ai_hallucination_detection.protection_action IS '防护策略: BLOCK/DEGRADE/HUMAN_REVIEW/PASS';
COMMENT ON COLUMN ai_hallucination_detection.reviewer IS '审核人';
COMMENT ON COLUMN ai_hallucination_detection.review_time IS '审核时间';
COMMENT ON COLUMN ai_hallucination_detection.review_note IS '审核备注';
COMMENT ON COLUMN ai_hallucination_detection.status IS '状态: DETECTED/REVIEWING/RESOLVED/DISMISSED';
COMMENT ON COLUMN ai_hallucination_detection.created_by IS '创建人';
COMMENT ON COLUMN ai_hallucination_detection.created_time IS '创建时间';
COMMENT ON COLUMN ai_hallucination_detection.updated_by IS '更新人';
COMMENT ON COLUMN ai_hallucination_detection.updated_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_red_team_result_category ON ai_red_team_result (tenant_id, category, verdict);
CREATE INDEX IF NOT EXISTS idx_red_team_result_severity ON ai_red_team_result (tenant_id, severity, verdict);
CREATE INDEX IF NOT EXISTS idx_hallucination_verdict ON ai_hallucination_detection (tenant_id, verdict, status);
CREATE INDEX IF NOT EXISTS idx_hallucination_model ON ai_hallucination_detection (tenant_id, model_code, status);
