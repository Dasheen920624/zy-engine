-- INTEROP-001: 院内互联互通标准适配矩阵 DDL
-- 扩展适配器表结构，支持多协议和标准

-- 1. 扩展 adp_adapter_def 表，增加协议和标准相关字段
ALTER TABLE adp_adapter_def ADD (
  protocol VARCHAR2(32) DEFAULT 'REST' NOT NULL,
  source_system VARCHAR2(32),
  base_url VARCHAR2(500),
  auth_type VARCHAR2(32) DEFAULT 'NONE',
  timeout_ms NUMBER(10) DEFAULT 30000,
  retry_count NUMBER(3) DEFAULT 3,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 扩展 adp_query_def 表，增加标准相关字段
ALTER TABLE adp_query_def ADD (
  query_type VARCHAR2(32) DEFAULT 'READ',
  request_template CLOB,
  response_mapping CLOB,
  fhir_resource_type VARCHAR2(64),
  hl7_message_type VARCHAR2(32),
  dicom_sop_class VARCHAR2(64),
  sample_data CLOB,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 创建适配器连接配置表
CREATE TABLE adp_connection_config (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR2(64) NOT NULL,
  config_key VARCHAR2(100) NOT NULL,
  config_value CLOB,
  config_type VARCHAR2(32) DEFAULT 'STRING',
  description VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_connection_config UNIQUE (tenant_id, hospital_code, adapter_code, config_key)
);

-- 4. 创建适配器认证凭据表（加密存储）
CREATE TABLE adp_credential (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR2(64) NOT NULL,
  credential_type VARCHAR2(32) NOT NULL, -- BASIC/OAUTH2/CERT/APIKEY
  credential_key VARCHAR2(100) NOT NULL,
  credential_value CLOB, -- 加密存储
  expires_at TIMESTAMP,
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_credential UNIQUE (tenant_id, hospital_code, adapter_code, credential_type, credential_key)
);

-- 5. 创建适配器调用日志表
CREATE TABLE adp_call_log (
  id NUMBER(20) PRIMARY KEY,
  trace_id VARCHAR2(128),
  tenant_id VARCHAR2(64) NOT NULL,
  hospital_code VARCHAR2(64) NOT NULL,
  adapter_code VARCHAR2(64) NOT NULL,
  query_code VARCHAR2(64) NOT NULL,
  request_params CLOB,
  response_data CLOB,
  status VARCHAR2(32) NOT NULL, -- SUCCESS/FAILED/TIMEOUT
  error_code VARCHAR2(64),
  error_message VARCHAR2(1000),
  elapsed_ms NUMBER(10),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  operator_id VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. 创建标准映射表
CREATE TABLE adp_standard_mapping (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR2(64) NOT NULL,
  source_field VARCHAR2(100) NOT NULL,
  target_standard VARCHAR2(32) NOT NULL, -- HL7/FHIR/CDA/DICOM
  target_field VARCHAR2(100) NOT NULL,
  mapping_rule CLOB, -- 映射规则（JSON或表达式）
  transform_function VARCHAR2(200), -- 转换函数
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_standard_mapping UNIQUE (tenant_id, hospital_code, adapter_code, source_field, target_standard)
);

-- 7. 创建CDS Hooks服务配置表
CREATE TABLE adp_cds_hooks_service (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  hook_id VARCHAR2(64) NOT NULL,
  hook_type VARCHAR2(32) NOT NULL, -- patient-view/order-select/order-sign
  service_id VARCHAR2(64) NOT NULL,
  service_title VARCHAR2(200) NOT NULL,
  description VARCHAR2(1000),
  usage_requirements VARCHAR2(1000),
  prefetch_data CLOB,
  response_template CLOB,
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_cds_hooks_service UNIQUE (tenant_id, hospital_code, hook_id, service_id)
);

-- 8. 创建SMART on FHIR应用配置表
CREATE TABLE adp_smart_app (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  app_id VARCHAR2(64) NOT NULL,
  app_name VARCHAR2(200) NOT NULL,
  app_type VARCHAR2(32) NOT NULL, -- EHR_LAUNCH/PATIENT_LAUNCH
  client_id VARCHAR2(100) NOT NULL,
  client_secret CLOB, -- 加密存储
  redirect_uri VARCHAR2(500) NOT NULL,
  scope VARCHAR2(500),
  launch_url VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_smart_app UNIQUE (tenant_id, hospital_code, app_id)
);

-- 9. 创建索引
CREATE INDEX idx_adp_call_log_trace ON adp_call_log(trace_id);
CREATE INDEX idx_adp_call_log_adapter ON adp_call_log(tenant_id, hospital_code, adapter_code, query_code);
CREATE INDEX idx_adp_call_log_time ON adp_call_log(created_time);
CREATE INDEX idx_adp_call_log_patient ON adp_call_log(patient_id);

-- 10. 添加注释
COMMENT ON TABLE adp_adapter_def IS '适配器定义表';
COMMENT ON COLUMN adp_adapter_def.protocol IS '通信协议：REST/HL7/FHIR/CDA/DICOM/SOAP/MLLP';
COMMENT ON COLUMN adp_adapter_def.source_system IS '源系统：HIS/EMR/LIS/PACS/INSURANCE/OA';
COMMENT ON COLUMN adp_adapter_def.auth_type IS '认证类型：NONE/BASIC/OAUTH2/CERT/APIKEY';

COMMENT ON TABLE adp_query_def IS '适配器查询定义表';
COMMENT ON COLUMN adp_query_def.query_type IS '查询类型：READ/WRITE/SUBSCRIBE';
COMMENT ON COLUMN adp_query_def.fhir_resource_type IS 'FHIR资源类型：Patient/Encounter/DiagnosticReport等';
COMMENT ON COLUMN adp_query_def.hl7_message_type IS 'HL7消息类型：ADT^A01/ORU^R01等';
COMMENT ON COLUMN adp_query_def.dicom_sop_class IS 'DICOM SOP类：CTImageStorage/MRImageStorage等';

COMMENT ON TABLE adp_connection_config IS '适配器连接配置表';
COMMENT ON TABLE adp_credential IS '适配器认证凭据表（加密存储）';
COMMENT ON TABLE adp_call_log IS '适配器调用日志表';
COMMENT ON TABLE adp_standard_mapping IS '标准映射表';
COMMENT ON TABLE adp_cds_hooks_service IS 'CDS Hooks服务配置表';
COMMENT ON TABLE adp_smart_app IS 'SMART on FHIR应用配置表';