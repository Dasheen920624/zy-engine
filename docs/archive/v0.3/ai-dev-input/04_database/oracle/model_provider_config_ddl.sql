-- 模型 Provider 配置表
CREATE TABLE model_provider_config (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  provider_type VARCHAR2(32) NOT NULL,
  provider_name VARCHAR2(128) NOT NULL,
  endpoint_url VARCHAR2(500),
  api_key VARCHAR2(500),
  model_name VARCHAR2(128),
  model_version VARCHAR2(32),
  timeout_ms NUMBER(10) DEFAULT 5000 NOT NULL,
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  degradation_target VARCHAR2(32),
  config_json VARCHAR2(4000),
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_model_provider_config UNIQUE (tenant_id, provider_type, provider_name)
);

COMMENT ON TABLE model_provider_config IS '模型Provider配置';
COMMENT ON COLUMN model_provider_config.provider_type IS 'Provider类型: RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/DIFY/LOCAL';
COMMENT ON COLUMN model_provider_config.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN model_provider_config.priority IS '优先级（数字越大优先级越高）';
COMMENT ON COLUMN model_provider_config.degradation_target IS '降级目标Provider类型';
