-- AI 知识生产任务表
CREATE TABLE ai_knowledge_job (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  job_code VARCHAR2(64) NOT NULL,
  job_name VARCHAR2(200) NOT NULL,
  job_type VARCHAR2(32) NOT NULL,
  source_code VARCHAR2(64),
  subscription_id VARCHAR2(64),
  model_provider VARCHAR2(64),
  model_name VARCHAR2(128),
  prompt_version VARCHAR2(32),
  input_hash VARCHAR2(128),
  output_hash VARCHAR2(128),
  input_summary VARCHAR2(2000),
  output_summary VARCHAR2(2000),
  evidence_ids VARCHAR2(1000),
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  review_status VARCHAR2(32) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(2000),
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  max_retries NUMBER(10) DEFAULT 3 NOT NULL,
  started_time TIMESTAMP,
  finished_time TIMESTAMP,
  duration_ms NUMBER(10),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_knowledge_job UNIQUE (tenant_id, job_code)
);

COMMENT ON TABLE ai_knowledge_job IS 'AI知识生产任务';
COMMENT ON COLUMN ai_knowledge_job.job_type IS '任务类型: EXTRACT/MAP/RULE_GENERATE/GRAPH_BUILD/QUALITY_CHECK';
COMMENT ON COLUMN ai_knowledge_job.status IS '状态: PENDING/RUNNING/SUCCESS/FAILED/RETRY/CANCELLED';
COMMENT ON COLUMN ai_knowledge_job.review_status IS '审核状态: PENDING/APPROVED/REJECTED';

-- AI 模型调用日志表
CREATE TABLE ai_model_call_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  job_id NUMBER(19),
  call_type VARCHAR2(32) NOT NULL,
  model_provider VARCHAR2(64) NOT NULL,
  model_name VARCHAR2(128) NOT NULL,
  model_version VARCHAR2(32),
  prompt_template_id VARCHAR2(64),
  prompt_version VARCHAR2(32),
  prompt_hash VARCHAR2(128),
  input_hash VARCHAR2(128),
  output_hash VARCHAR2(128),
  input_token_count NUMBER(10),
  output_token_count NUMBER(10),
  total_token_count NUMBER(10),
  call_status VARCHAR2(32) NOT NULL,
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  fallback_used VARCHAR2(32),
  fallback_provider VARCHAR2(64),
  fallback_model VARCHAR2(128),
  trace_id VARCHAR2(64),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  elapsed_ms NUMBER(10),
  called_time TIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

COMMENT ON TABLE ai_model_call_log IS 'AI模型调用日志';
COMMENT ON COLUMN ai_model_call_log.call_type IS '调用类型: RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/WORKFLOW';
COMMENT ON COLUMN ai_model_call_log.call_status IS '调用状态: SUCCESS/ERROR/FALLBACK/TIMEOUT';
