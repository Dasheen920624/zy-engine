-- INTEROP-001: 院内互联互通标准适配矩阵 DDL（H2本地数据库版本）
-- 扩展适配器表结构，支持多协议和标准
-- 与 ai-dev-input/04_database/local/interop_ddl.sql 同源，H2 原生类型

-- 1. 扩展 adp_adapter_def 表，增加协议和标准相关字段
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS protocol VARCHAR(32) DEFAULT 'REST';
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS source_system VARCHAR(32);
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS base_url VARCHAR(500);
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS auth_type VARCHAR(32) DEFAULT 'NONE';
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS timeout_ms INTEGER DEFAULT 30000;
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 3;
ALTER TABLE adp_adapter_def ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 2. 扩展 adp_query_def 表，增加标准相关字段
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS query_type VARCHAR(32) DEFAULT 'READ';
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS request_template CLOB;
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS response_mapping CLOB;
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS fhir_resource_type VARCHAR(64);
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS hl7_message_type VARCHAR(32);
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS dicom_sop_class VARCHAR(64);
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS sample_data CLOB;
ALTER TABLE adp_query_def ADD COLUMN IF NOT EXISTS updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 3. 创建适配器连接配置表
CREATE TABLE IF NOT EXISTS adp_connection_config (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'ZYHOSPITAL',
  adapter_code VARCHAR(64) NOT NULL,
  config_key VARCHAR(100) NOT NULL,
  config_value CLOB,
  config_type VARCHAR(32) DEFAULT 'STRING',
  description VARCHAR(500),
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_connection_config UNIQUE (tenant_id, hospital_code, adapter_code, config_key)
);

-- 4. 创建适配器认证凭据表（加密存储）
CREATE TABLE IF NOT EXISTS adp_credential (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'ZYHOSPITAL',
  adapter_code VARCHAR(64) NOT NULL,
  credential_type VARCHAR(32) NOT NULL,
  credential_key VARCHAR(100) NOT NULL,
  credential_value CLOB,
  expires_at TIMESTAMP,
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_credential UNIQUE (tenant_id, hospital_code, adapter_code, credential_type, credential_key)
);

-- 5. 创建适配器调用日志表
CREATE TABLE IF NOT EXISTS adp_call_log (
  id BIGINT PRIMARY KEY,
  trace_id VARCHAR(128),
  tenant_id VARCHAR(64) NOT NULL,
  hospital_code VARCHAR(64) NOT NULL,
  adapter_code VARCHAR(64) NOT NULL,
  query_code VARCHAR(64) NOT NULL,
  request_params CLOB,
  response_data CLOB,
  status VARCHAR(32) NOT NULL,
  error_code VARCHAR(64),
  error_message VARCHAR(1000),
  elapsed_ms INTEGER,
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  operator_id VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. 创建标准映射表
CREATE TABLE IF NOT EXISTS adp_standard_mapping (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'ZYHOSPITAL',
  adapter_code VARCHAR(64) NOT NULL,
  source_field VARCHAR(100) NOT NULL,
  target_standard VARCHAR(32) NOT NULL,
  target_field VARCHAR(100) NOT NULL,
  mapping_rule CLOB,
  transform_function VARCHAR(200),
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_standard_mapping UNIQUE (tenant_id, hospital_code, adapter_code, source_field, target_standard)
);

-- 7. 创建CDS Hooks服务配置表
CREATE TABLE IF NOT EXISTS adp_cds_hooks_service (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'ZYHOSPITAL',
  hook_id VARCHAR(64) NOT NULL,
  hook_type VARCHAR(32) NOT NULL,
  service_id VARCHAR(64) NOT NULL,
  service_title VARCHAR(200) NOT NULL,
  description VARCHAR(1000),
  usage_requirements VARCHAR(1000),
  prefetch_data CLOB,
  response_template CLOB,
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_cds_hooks_service UNIQUE (tenant_id, hospital_code, hook_id, service_id)
);

-- 8. 创建SMART on FHIR应用配置表
CREATE TABLE IF NOT EXISTS adp_smart_app (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'ZYHOSPITAL',
  app_id VARCHAR(64) NOT NULL,
  app_name VARCHAR(200) NOT NULL,
  app_type VARCHAR(32) NOT NULL,
  client_id VARCHAR(100) NOT NULL,
  client_secret CLOB,
  redirect_uri VARCHAR(500) NOT NULL,
  scope VARCHAR(500),
  launch_url VARCHAR(500),
  status VARCHAR(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_smart_app UNIQUE (tenant_id, hospital_code, app_id)
);

-- 9. 创建索引
CREATE INDEX IF NOT EXISTS idx_adp_call_log_trace ON adp_call_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_adp_call_log_adapter ON adp_call_log(tenant_id, hospital_code, adapter_code, query_code);
CREATE INDEX IF NOT EXISTS idx_adp_call_log_time ON adp_call_log(created_time);
CREATE INDEX IF NOT EXISTS idx_adp_call_log_patient ON adp_call_log(patient_id);
