-- AI-GOV-001 AI治理 DDL (H2 本地开发库)

-- 模型注册表
CREATE TABLE IF NOT EXISTS ai_model_registry (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  model_code VARCHAR(64) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  model_provider VARCHAR(32) NOT NULL,
  model_version VARCHAR(32),
  model_type VARCHAR(32),
  endpoint_url VARCHAR(500),
  api_key_ref VARCHAR(128),
  timeout_ms INTEGER DEFAULT 5000,
  max_tokens INTEGER DEFAULT 4096,
  temperature DECIMAL(3,2) DEFAULT 0.70,
  status VARCHAR(16) DEFAULT 'REGISTERED' NOT NULL,
  review_status VARCHAR(16) DEFAULT 'PENDING',
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  enabled VARCHAR(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR(2000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_model_registry UNIQUE (tenant_id, model_code)
);

-- 提示词模板表
CREATE TABLE IF NOT EXISTS ai_prompt_template (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(200) NOT NULL,
  template_type VARCHAR(32),
  model_type VARCHAR(32),
  content VARCHAR(4000),
  version VARCHAR(32) DEFAULT '1.0.0',
  variables VARCHAR(2000),
  hash VARCHAR(128),
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  review_status VARCHAR(16) DEFAULT 'PENDING',
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  enabled VARCHAR(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR(2000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_prompt_template UNIQUE (tenant_id, template_code, version)
);

-- 模型评测任务表
CREATE TABLE IF NOT EXISTS ai_model_eval_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_name VARCHAR(200) NOT NULL,
  model_code VARCHAR(64) NOT NULL,
  model_version VARCHAR(32),
  prompt_template_code VARCHAR(64),
  prompt_version VARCHAR(32),
  benchmark_code VARCHAR(64),
  benchmark_name VARCHAR(200),
  sample_size INTEGER DEFAULT 100,
  status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
  accuracy_score DECIMAL(5,4),
  latency_ms DECIMAL(10,2),
  pass_rate DECIMAL(5,4),
  result_summary VARCHAR(2000),
  detail_json VARCHAR(4000),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  completed_time TIMESTAMP,
  CONSTRAINT uk_ai_model_eval_task UNIQUE (tenant_id, task_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_model_registry_tenant ON ai_model_registry (tenant_id, model_type, status);
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_tenant ON ai_prompt_template (tenant_id, template_type, status);
CREATE INDEX IF NOT EXISTS idx_ai_model_eval_task_model ON ai_model_eval_task (tenant_id, model_code, status);
