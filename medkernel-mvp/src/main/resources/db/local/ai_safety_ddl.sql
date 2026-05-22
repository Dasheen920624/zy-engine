-- AI-GOV-002 AI安全红队和幻觉防护 DDL (H2 本地开发库)

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
  actual_response VARCHAR(4000),
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

-- 幻觉检测记录
CREATE TABLE IF NOT EXISTS ai_hallucination_detection (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  detection_code VARCHAR(64) NOT NULL,
  model_code VARCHAR(64),
  model_version VARCHAR(32),
  prompt_template_code VARCHAR(64),
  input_content VARCHAR(4000),
  output_content VARCHAR(4000),
  detection_type VARCHAR(32),
  confidence_score DECIMAL(5,4),
  verdict VARCHAR(32),
  evidence VARCHAR(4000),
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

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_red_team_result_category ON ai_red_team_result (tenant_id, category, verdict);
CREATE INDEX IF NOT EXISTS idx_red_team_result_severity ON ai_red_team_result (tenant_id, severity, verdict);
CREATE INDEX IF NOT EXISTS idx_hallucination_verdict ON ai_hallucination_detection (tenant_id, verdict, status);
CREATE INDEX IF NOT EXISTS idx_hallucination_model ON ai_hallucination_detection (tenant_id, model_code, status);
