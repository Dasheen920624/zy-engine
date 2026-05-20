-- CDSS-004 医疗安全红线扫描 DDL (Oracle)

-- 安全红线定义
CREATE TABLE cdss_safety_red_line (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  red_line_code VARCHAR2(64) NOT NULL,
  red_line_name VARCHAR2(200) NOT NULL,
  category VARCHAR2(32) NOT NULL,
  description VARCHAR2(2000),
  condition_expression VARCHAR2(2000),
  blocking_action VARCHAR2(32),
  severity VARCHAR2(16),
  applicable_scenarios CLOB,
  enabled VARCHAR2(1) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_safety_red_line UNIQUE (tenant_id, red_line_code)
);

COMMENT ON TABLE cdss_safety_red_line IS '安全红线定义';
COMMENT ON COLUMN cdss_safety_red_line.red_line_code IS '红线编码';
COMMENT ON COLUMN cdss_safety_red_line.red_line_name IS '红线名称';
COMMENT ON COLUMN cdss_safety_red_line.category IS '分类: MEDICATION/DIAGNOSIS/PROCEDURE/PATHWAY/AI_OUTPUT';
COMMENT ON COLUMN cdss_safety_red_line.description IS '描述';
COMMENT ON COLUMN cdss_safety_red_line.condition_expression IS '触发条件表达式';
COMMENT ON COLUMN cdss_safety_red_line.blocking_action IS '阻断策略: WARN/BLOCK/ESCALATE/REQUIRE_DUAL_CONFIRM';
COMMENT ON COLUMN cdss_safety_red_line.severity IS '严重度: HIGH/CRITICAL';
COMMENT ON COLUMN cdss_safety_red_line.applicable_scenarios IS '适用场景 JSON';
COMMENT ON COLUMN cdss_safety_red_line.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN cdss_safety_red_line.created_by IS '创建人';
COMMENT ON COLUMN cdss_safety_red_line.created_time IS '创建时间';
COMMENT ON COLUMN cdss_safety_red_line.updated_by IS '更新人';
COMMENT ON COLUMN cdss_safety_red_line.updated_time IS '更新时间';

-- 红线扫描结果
CREATE TABLE cdss_red_line_scan_result (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  scan_code VARCHAR2(64) NOT NULL,
  scan_type VARCHAR2(16),
  red_line_code VARCHAR2(64) NOT NULL,
  red_line_name VARCHAR2(200),
  category VARCHAR2(32),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  trigger_context CLOB,
  violation_detail VARCHAR2(2000),
  severity VARCHAR2(16),
  blocking_action VARCHAR2(32),
  status VARCHAR2(16) DEFAULT 'DETECTED' NOT NULL,
  overridden_by VARCHAR2(64),
  override_reason VARCHAR2(2000),
  resolved_by VARCHAR2(64),
  resolution_note VARCHAR2(2000),
  resolved_time TIMESTAMP,
  scan_by VARCHAR2(64),
  scan_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_cdss_red_line_scan_result UNIQUE (tenant_id, scan_code)
);

COMMENT ON TABLE cdss_red_line_scan_result IS '红线扫描结果';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_code IS '扫描编码';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_type IS '扫描类型: MANUAL/SCHEDULED/REALTIME';
COMMENT ON COLUMN cdss_red_line_scan_result.red_line_code IS '红线编码';
COMMENT ON COLUMN cdss_red_line_scan_result.red_line_name IS '红线名称';
COMMENT ON COLUMN cdss_red_line_scan_result.category IS '分类';
COMMENT ON COLUMN cdss_red_line_scan_result.patient_id IS '患者ID';
COMMENT ON COLUMN cdss_red_line_scan_result.encounter_id IS '就诊ID';
COMMENT ON COLUMN cdss_red_line_scan_result.trigger_context IS '触发上下文 JSON';
COMMENT ON COLUMN cdss_red_line_scan_result.violation_detail IS '违反详情';
COMMENT ON COLUMN cdss_red_line_scan_result.severity IS '严重度: HIGH/CRITICAL';
COMMENT ON COLUMN cdss_red_line_scan_result.blocking_action IS '阻断策略';
COMMENT ON COLUMN cdss_red_line_scan_result.status IS '状态: DETECTED/BLOCKED/OVERRIDDEN/RESOLVED';
COMMENT ON COLUMN cdss_red_line_scan_result.overridden_by IS '覆盖人';
COMMENT ON COLUMN cdss_red_line_scan_result.override_reason IS '覆盖原因';
COMMENT ON COLUMN cdss_red_line_scan_result.resolved_by IS '解决人';
COMMENT ON COLUMN cdss_red_line_scan_result.resolution_note IS '解决备注';
COMMENT ON COLUMN cdss_red_line_scan_result.resolved_time IS '解决时间';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_by IS '扫描人';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_time IS '扫描时间';
COMMENT ON COLUMN cdss_red_line_scan_result.created_time IS '创建时间';

CREATE INDEX idx_red_line_scan_patient ON cdss_red_line_scan_result (tenant_id, patient_id, status);
CREATE INDEX idx_red_line_scan_severity ON cdss_red_line_scan_result (tenant_id, severity, status);
