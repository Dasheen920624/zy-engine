-- CDSS-003 覆盖审计日志表 (H2 本地开发库)
-- 记录规则覆盖/确认/升级的审计日志

CREATE TABLE IF NOT EXISTS cdss_override_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  alert_id VARCHAR(64),
  trigger_code VARCHAR(64),
  rule_code VARCHAR(64),
  risk_level VARCHAR(16),
  alert_level VARCHAR(16),
  override_type VARCHAR(32) NOT NULL,
  override_reason VARCHAR(2000),
  override_category VARCHAR(32),
  supervisor_name VARCHAR(128),
  confirmed_by VARCHAR(128),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  operator_id VARCHAR(64),
  department_code VARCHAR(64),
  is_audit_red_line VARCHAR(5) DEFAULT 'FALSE',
  fatigue_suppressed VARCHAR(5) DEFAULT 'FALSE',
  override_time TIMESTAMP NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_cdss_override_tenant ON cdss_override_log (tenant_id, override_time);
CREATE INDEX IF NOT EXISTS idx_cdss_override_operator ON cdss_override_log (tenant_id, operator_id, rule_code, override_time);
CREATE INDEX IF NOT EXISTS idx_cdss_override_patient ON cdss_override_log (tenant_id, patient_id);

-- CDSS-003 疲劳配置表 (H2 本地开发库)
-- 定义覆盖疲劳抑制策略

CREATE TABLE IF NOT EXISTS cdss_fatigue_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_code VARCHAR(64) NOT NULL,
  config_name VARCHAR(200) NOT NULL,
  rule_code VARCHAR(64),
  department_code VARCHAR(64),
  time_window_hours INTEGER DEFAULT 24 NOT NULL,
  override_threshold INTEGER DEFAULT 3 NOT NULL,
  suppress_action VARCHAR(32) DEFAULT 'SUPPRESS' NOT NULL,
  suppress_level VARCHAR(16),
  enabled VARCHAR(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR(2000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_fatigue_config UNIQUE (tenant_id, config_code)
);
