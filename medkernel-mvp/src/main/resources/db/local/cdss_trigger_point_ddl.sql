-- CDSS 触发点配置表：定义院内业务触发点和接入策略
CREATE TABLE IF NOT EXISTS cdss_trigger_point (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  trigger_code VARCHAR(64) NOT NULL,
  trigger_name VARCHAR(200) NOT NULL,
  trigger_type VARCHAR(32) NOT NULL,
  business_scenario VARCHAR(32) NOT NULL,
  access_strategy VARCHAR(32) NOT NULL,
  adapter_code VARCHAR(64),
  endpoint_url VARCHAR(500),
  rule_codes VARCHAR(1000),
  pathway_codes VARCHAR(1000),
  priority INTEGER DEFAULT 0 NOT NULL,
  risk_level VARCHAR(16) DEFAULT 'LOW',
  timeout_ms INTEGER DEFAULT 5000 NOT NULL,
  enabled VARCHAR(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR(2000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_trigger_point UNIQUE (tenant_id, trigger_code)
);
