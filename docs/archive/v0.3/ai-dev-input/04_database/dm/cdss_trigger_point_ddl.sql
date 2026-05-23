-- CDSS 触发点配置表：定义院内业务触发点和接入策略
CREATE TABLE cdss_trigger_point (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  trigger_code VARCHAR2(64) NOT NULL,
  trigger_name VARCHAR2(200) NOT NULL,
  trigger_type VARCHAR2(32) NOT NULL,
  business_scenario VARCHAR2(32) NOT NULL,
  access_strategy VARCHAR2(32) NOT NULL,
  adapter_code VARCHAR2(64),
  endpoint_url VARCHAR2(500),
  rule_codes VARCHAR2(1000),
  pathway_codes VARCHAR2(1000),
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  risk_level VARCHAR2(16) DEFAULT 'LOW',
  timeout_ms NUMBER(10) DEFAULT 5000 NOT NULL,
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_trigger_point UNIQUE (tenant_id, trigger_code)
);

COMMENT ON TABLE cdss_trigger_point IS 'CDSS触发点配置表';
COMMENT ON COLUMN cdss_trigger_point.trigger_code IS '触发点编码';
COMMENT ON COLUMN cdss_trigger_point.trigger_name IS '触发点名称';
COMMENT ON COLUMN cdss_trigger_point.trigger_type IS '触发类型: ORDER/EMR/EXAM/PATHWAY/INSURANCE';
COMMENT ON COLUMN cdss_trigger_point.business_scenario IS '业务场景: PRESCRIBE/ADMISSION/DIAGNOSIS/EXAM/PATHWAY_ADMIT/SETTLEMENT';
COMMENT ON COLUMN cdss_trigger_point.access_strategy IS '接入策略: API/IFRAME/CDS_HOOKS/MESSAGE';
COMMENT ON COLUMN cdss_trigger_point.adapter_code IS '适配器编码';
COMMENT ON COLUMN cdss_trigger_point.endpoint_url IS '端点URL';
COMMENT ON COLUMN cdss_trigger_point.rule_codes IS '关联规则编码';
COMMENT ON COLUMN cdss_trigger_point.pathway_codes IS '关联路径编码';
COMMENT ON COLUMN cdss_trigger_point.priority IS '优先级';
COMMENT ON COLUMN cdss_trigger_point.risk_level IS '风险等级: LOW/MEDIUM/HIGH';
COMMENT ON COLUMN cdss_trigger_point.timeout_ms IS '超时时间(毫秒)';
COMMENT ON COLUMN cdss_trigger_point.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN cdss_trigger_point.description IS '描述';
