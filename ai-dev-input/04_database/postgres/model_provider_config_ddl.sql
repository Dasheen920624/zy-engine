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

COMMENT ON TABLE model_provider_config IS '模型Provider配置';
COMMENT ON COLUMN model_provider_config.provider_type IS 'Provider类型: RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/DIFY/LOCAL';
COMMENT ON COLUMN model_provider_config.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN model_provider_config.priority IS '优先级（数字越大优先级越高）';
COMMENT ON COLUMN model_provider_config.degradation_target IS '降级目标Provider类型';
