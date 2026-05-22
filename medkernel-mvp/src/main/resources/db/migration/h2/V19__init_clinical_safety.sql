-- RISK-001 临床安全 DDL (H2 本地开发库)

-- 危险日志
CREATE TABLE IF NOT EXISTS clinical_hazard_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  hazard_code VARCHAR(64) NOT NULL,
  hazard_name VARCHAR(200) NOT NULL,
  hazard_category VARCHAR(32) NOT NULL,
  hazard_description VARCHAR(2000),
  affected_process VARCHAR(32),
  likelihood VARCHAR(32),
  severity VARCHAR(32),
  risk_level VARCHAR(16),
  control_measures VARCHAR(2000),
  residual_risk VARCHAR(16),
  status VARCHAR(16) DEFAULT 'IDENTIFIED' NOT NULL,
  accepted_by VARCHAR(64),
  accepted_time TIMESTAMP,
  acceptance_note VARCHAR(2000),
  blocking_strategy VARCHAR(32),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_hazard_log UNIQUE (tenant_id, hazard_code)
);

-- 安全案例
CREATE TABLE IF NOT EXISTS clinical_safety_case (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  case_code VARCHAR(64) NOT NULL,
  case_name VARCHAR(200) NOT NULL,
  case_type VARCHAR(32),
  scope VARCHAR(2000),
  goal VARCHAR(2000),
  argument VARCHAR(4000),
  evidence_refs VARCHAR(4000),
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  version VARCHAR(32) DEFAULT '1.0.0',
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_safety_case UNIQUE (tenant_id, case_code, version)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_clinical_hazard_tenant ON clinical_hazard_log (tenant_id, hazard_category, risk_level);
CREATE INDEX IF NOT EXISTS idx_clinical_hazard_status ON clinical_hazard_log (tenant_id, status, risk_level);
CREATE INDEX IF NOT EXISTS idx_clinical_safety_case_tenant ON clinical_safety_case (tenant_id, case_type, status);
