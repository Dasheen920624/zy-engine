-- CDSS-003 覆盖审计日志表 (Oracle)
-- 记录规则覆盖/确认/升级的审计日志

CREATE TABLE cdss_override_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  alert_id VARCHAR2(64),
  trigger_code VARCHAR2(64),
  rule_code VARCHAR2(64),
  risk_level VARCHAR2(16),
  alert_level VARCHAR2(16),
  override_type VARCHAR2(32) NOT NULL,
  override_reason VARCHAR2(2000),
  override_category VARCHAR2(32),
  supervisor_name VARCHAR2(128),
  confirmed_by VARCHAR2(128),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  operator_id VARCHAR2(64),
  department_code VARCHAR2(64),
  is_audit_red_line VARCHAR2(5) DEFAULT 'FALSE',
  fatigue_suppressed VARCHAR2(5) DEFAULT 'FALSE',
  override_time TIMESTAMP NOT NULL,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
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

CREATE INDEX idx_cdss_override_tenant ON cdss_override_log (tenant_id, override_time);
CREATE INDEX idx_cdss_override_operator ON cdss_override_log (tenant_id, operator_id, rule_code, override_time);
CREATE INDEX idx_cdss_override_patient ON cdss_override_log (tenant_id, patient_id);

-- CDSS-003 疲劳配置表 (Oracle)
-- 定义覆盖疲劳抑制策略

CREATE TABLE cdss_fatigue_config (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  config_code VARCHAR2(64) NOT NULL,
  config_name VARCHAR2(200) NOT NULL,
  rule_code VARCHAR2(64),
  department_code VARCHAR2(64),
  time_window_hours NUMBER(10) DEFAULT 24 NOT NULL,
  override_threshold NUMBER(10) DEFAULT 3 NOT NULL,
  suppress_action VARCHAR2(32) DEFAULT 'SUPPRESS' NOT NULL,
  suppress_level VARCHAR2(16),
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
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
