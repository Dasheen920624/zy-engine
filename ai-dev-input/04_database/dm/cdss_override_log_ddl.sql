-- CDSS-003 覆盖审计日志表 (DM 达梦)
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

COMMENT ON TABLE cdss_override_log IS 'CDSS覆盖审计日志表';
COMMENT ON COLUMN cdss_override_log.alert_id IS '告警ID';
COMMENT ON COLUMN cdss_override_log.trigger_code IS '触发点编码';
COMMENT ON COLUMN cdss_override_log.rule_code IS '规则编码';
COMMENT ON COLUMN cdss_override_log.risk_level IS '风险等级';
COMMENT ON COLUMN cdss_override_log.alert_level IS '告警级别: NOTICE/SOFT/BLOCK/ESCALATE';
COMMENT ON COLUMN cdss_override_log.override_type IS '覆盖类型: ACKNOWLEDGE/OVERRIDE/ESCALATE';
COMMENT ON COLUMN cdss_override_log.override_reason IS '覆盖原因';
COMMENT ON COLUMN cdss_override_log.override_category IS '覆盖分类: CLINICAL_JUDGEMENT/ALTERNATIVE_THERAPY/PATIENT_REQUEST/EMERGENCY/OTHER';
COMMENT ON COLUMN cdss_override_log.supervisor_name IS '上级审批人';
COMMENT ON COLUMN cdss_override_log.confirmed_by IS '确认人';
COMMENT ON COLUMN cdss_override_log.patient_id IS '患者ID';
COMMENT ON COLUMN cdss_override_log.encounter_id IS '就诊ID';
COMMENT ON COLUMN cdss_override_log.operator_id IS '操作人ID';
COMMENT ON COLUMN cdss_override_log.department_code IS '科室编码';
COMMENT ON COLUMN cdss_override_log.is_audit_red_line IS '是否审计红线: TRUE/FALSE';
COMMENT ON COLUMN cdss_override_log.fatigue_suppressed IS '是否疲劳抑制: TRUE/FALSE';
COMMENT ON COLUMN cdss_override_log.override_time IS '覆盖时间';
COMMENT ON COLUMN cdss_override_log.created_time IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_cdss_override_tenant ON cdss_override_log (tenant_id, override_time);
CREATE INDEX IF NOT EXISTS idx_cdss_override_operator ON cdss_override_log (tenant_id, operator_id, rule_code, override_time);
CREATE INDEX IF NOT EXISTS idx_cdss_override_patient ON cdss_override_log (tenant_id, patient_id);

-- CDSS-003 疲劳配置表 (DM 达梦)
-- 定义覆盖疲劳抑制策略

CREATE TABLE IF NOT EXISTS cdss_fatigue_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_code VARCHAR(64) NOT NULL,
  config_name VARCHAR(200) NOT NULL,
  rule_code VARCHAR(64),
  department_code VARCHAR(64),
  time_window_hours INT DEFAULT 24 NOT NULL,
  override_threshold INT DEFAULT 3 NOT NULL,
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

COMMENT ON TABLE cdss_fatigue_config IS 'CDSS疲劳配置表';
COMMENT ON COLUMN cdss_fatigue_config.config_code IS '配置编码';
COMMENT ON COLUMN cdss_fatigue_config.config_name IS '配置名称';
COMMENT ON COLUMN cdss_fatigue_config.rule_code IS '规则编码';
COMMENT ON COLUMN cdss_fatigue_config.department_code IS '科室编码';
COMMENT ON COLUMN cdss_fatigue_config.time_window_hours IS '时间窗口(小时)';
COMMENT ON COLUMN cdss_fatigue_config.override_threshold IS '覆盖阈值';
COMMENT ON COLUMN cdss_fatigue_config.suppress_action IS '抑制动作: SUPPRESS/DOWNGRADE/NOTIFY_SUPERVISOR';
COMMENT ON COLUMN cdss_fatigue_config.suppress_level IS '抑制级别';
COMMENT ON COLUMN cdss_fatigue_config.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN cdss_fatigue_config.description IS '描述';
COMMENT ON COLUMN cdss_fatigue_config.created_by IS '创建人';
COMMENT ON COLUMN cdss_fatigue_config.created_time IS '创建时间';
COMMENT ON COLUMN cdss_fatigue_config.updated_by IS '更新人';
COMMENT ON COLUMN cdss_fatigue_config.updated_time IS '更新时间';
