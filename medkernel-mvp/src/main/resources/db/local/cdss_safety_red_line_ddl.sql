-- CDSS-004 医疗安全红线扫描 DDL (H2 本地开发库)

-- 安全红线定义
CREATE TABLE IF NOT EXISTS cdss_safety_red_line (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  red_line_code VARCHAR(64) NOT NULL,
  red_line_name VARCHAR(200) NOT NULL,
  category VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  condition_expression VARCHAR(2000),
  blocking_action VARCHAR(32),
  severity VARCHAR(16),
  applicable_scenarios VARCHAR(4000),
  enabled VARCHAR(1) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_safety_red_line UNIQUE (tenant_id, red_line_code)
);

-- 红线扫描结果
CREATE TABLE IF NOT EXISTS cdss_red_line_scan_result (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  scan_code VARCHAR(64) NOT NULL,
  scan_type VARCHAR(16),
  red_line_code VARCHAR(64) NOT NULL,
  red_line_name VARCHAR(200),
  category VARCHAR(32),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  trigger_context VARCHAR(4000),
  violation_detail VARCHAR(2000),
  severity VARCHAR(16),
  blocking_action VARCHAR(32),
  status VARCHAR(16) DEFAULT 'DETECTED' NOT NULL,
  overridden_by VARCHAR(64),
  override_reason VARCHAR(2000),
  resolved_by VARCHAR(64),
  resolution_note VARCHAR(2000),
  resolved_time TIMESTAMP,
  scan_by VARCHAR(64),
  scan_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_cdss_red_line_scan_result UNIQUE (tenant_id, scan_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_red_line_scan_patient ON cdss_red_line_scan_result (tenant_id, patient_id, status);
CREATE INDEX IF NOT EXISTS idx_red_line_scan_severity ON cdss_red_line_scan_result (tenant_id, severity, status);
