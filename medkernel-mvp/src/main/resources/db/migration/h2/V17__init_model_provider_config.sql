-- 模型 Provider 配置表：存储模型 Provider 的连接与调度配置
CREATE TABLE IF NOT EXISTS model_provider_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  provider_name VARCHAR(128) NOT NULL,
  endpoint_url VARCHAR(500),
  api_key VARCHAR(500),
  model_name VARCHAR(128),
  model_version VARCHAR(32),
  timeout_ms INTEGER DEFAULT 5000 NOT NULL,
  retry_count INTEGER DEFAULT 0 NOT NULL,
  priority INTEGER DEFAULT 0 NOT NULL,
  enabled VARCHAR(5) DEFAULT 'TRUE' NOT NULL,
  degradation_target VARCHAR(32),
  config_json VARCHAR(4000),
  description VARCHAR(2000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_model_provider_config UNIQUE (tenant_id, provider_type, provider_name)
);
