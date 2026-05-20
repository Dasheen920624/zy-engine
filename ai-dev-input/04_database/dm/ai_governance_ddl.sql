-- AI-GOV-001 AI治理 DDL (DM 达梦)

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

COMMENT ON TABLE ai_model_registry IS '模型注册表';
COMMENT ON COLUMN ai_model_registry.model_code IS '模型编码';
COMMENT ON COLUMN ai_model_registry.model_name IS '模型名称';
COMMENT ON COLUMN ai_model_registry.model_provider IS '模型供应商';
COMMENT ON COLUMN ai_model_registry.model_version IS '模型版本';
COMMENT ON COLUMN ai_model_registry.model_type IS '模型类型';
COMMENT ON COLUMN ai_model_registry.endpoint_url IS '服务端点URL';
COMMENT ON COLUMN ai_model_registry.api_key_ref IS 'API密钥引用';
COMMENT ON COLUMN ai_model_registry.timeout_ms IS '超时时间(毫秒)';
COMMENT ON COLUMN ai_model_registry.max_tokens IS '最大Token数';
COMMENT ON COLUMN ai_model_registry.temperature IS '温度参数';
COMMENT ON COLUMN ai_model_registry.status IS '状态: REGISTERED/ACTIVE/DEPRECATED/RETIRED';
COMMENT ON COLUMN ai_model_registry.review_status IS '审核状态: PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN ai_model_registry.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_model_registry.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_model_registry.review_note IS '审核备注';
COMMENT ON COLUMN ai_model_registry.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN ai_model_registry.description IS '描述';
COMMENT ON COLUMN ai_model_registry.created_by IS '创建人';
COMMENT ON COLUMN ai_model_registry.created_time IS '创建时间';
COMMENT ON COLUMN ai_model_registry.updated_by IS '更新人';
COMMENT ON COLUMN ai_model_registry.updated_time IS '更新时间';

-- 提示词模板表
CREATE TABLE IF NOT EXISTS ai_prompt_template (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(200) NOT NULL,
  template_type VARCHAR(32),
  model_type VARCHAR(32),
  content CLOB,
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

COMMENT ON TABLE ai_prompt_template IS '提示词模板表';
COMMENT ON COLUMN ai_prompt_template.template_code IS '模板编码';
COMMENT ON COLUMN ai_prompt_template.template_name IS '模板名称';
COMMENT ON COLUMN ai_prompt_template.template_type IS '模板类型';
COMMENT ON COLUMN ai_prompt_template.model_type IS '适用模型类型';
COMMENT ON COLUMN ai_prompt_template.content IS '模板内容';
COMMENT ON COLUMN ai_prompt_template.version IS '模板版本';
COMMENT ON COLUMN ai_prompt_template.variables IS '变量定义';
COMMENT ON COLUMN ai_prompt_template.hash IS '内容哈希';
COMMENT ON COLUMN ai_prompt_template.status IS '状态: DRAFT/PUBLISHED/ARCHIVED';
COMMENT ON COLUMN ai_prompt_template.review_status IS '审核状态: PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN ai_prompt_template.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_prompt_template.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_prompt_template.review_note IS '审核备注';
COMMENT ON COLUMN ai_prompt_template.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN ai_prompt_template.description IS '描述';
COMMENT ON COLUMN ai_prompt_template.created_by IS '创建人';
COMMENT ON COLUMN ai_prompt_template.created_time IS '创建时间';
COMMENT ON COLUMN ai_prompt_template.updated_by IS '更新人';
COMMENT ON COLUMN ai_prompt_template.updated_time IS '更新时间';

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
  detail_json CLOB,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  completed_time TIMESTAMP,
  CONSTRAINT uk_ai_model_eval_task UNIQUE (tenant_id, task_code)
);

COMMENT ON TABLE ai_model_eval_task IS '模型评测任务表';
COMMENT ON COLUMN ai_model_eval_task.task_code IS '任务编码';
COMMENT ON COLUMN ai_model_eval_task.task_name IS '任务名称';
COMMENT ON COLUMN ai_model_eval_task.model_code IS '评测模型编码';
COMMENT ON COLUMN ai_model_eval_task.model_version IS '评测模型版本';
COMMENT ON COLUMN ai_model_eval_task.prompt_template_code IS '提示词模板编码';
COMMENT ON COLUMN ai_model_eval_task.prompt_version IS '提示词模板版本';
COMMENT ON COLUMN ai_model_eval_task.benchmark_code IS '基准测试编码';
COMMENT ON COLUMN ai_model_eval_task.benchmark_name IS '基准测试名称';
COMMENT ON COLUMN ai_model_eval_task.sample_size IS '样本数量';
COMMENT ON COLUMN ai_model_eval_task.status IS '状态: PENDING/RUNNING/COMPLETED/FAILED';
COMMENT ON COLUMN ai_model_eval_task.accuracy_score IS '准确率评分';
COMMENT ON COLUMN ai_model_eval_task.latency_ms IS '平均延迟(毫秒)';
COMMENT ON COLUMN ai_model_eval_task.pass_rate IS '通过率';
COMMENT ON COLUMN ai_model_eval_task.result_summary IS '结果摘要';
COMMENT ON COLUMN ai_model_eval_task.detail_json IS '详情JSON';
COMMENT ON COLUMN ai_model_eval_task.created_by IS '创建人';
COMMENT ON COLUMN ai_model_eval_task.created_time IS '创建时间';
COMMENT ON COLUMN ai_model_eval_task.completed_time IS '完成时间';

CREATE INDEX IF NOT EXISTS idx_ai_model_registry_tenant ON ai_model_registry (tenant_id, model_type, status);
CREATE INDEX IF NOT EXISTS idx_ai_prompt_template_tenant ON ai_prompt_template (tenant_id, template_type, status);
CREATE INDEX IF NOT EXISTS idx_ai_model_eval_task_model ON ai_model_eval_task (tenant_id, model_code, status);
