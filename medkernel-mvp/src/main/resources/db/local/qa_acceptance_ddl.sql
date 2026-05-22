-- TEST-005 全功能验收中心和证据归档 DDL (H2 本地开发库)

-- 验收测试用例
CREATE TABLE IF NOT EXISTS qa_acceptance_test_case (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  case_code VARCHAR(64) NOT NULL,
  case_name VARCHAR(200) NOT NULL,
  feature_code VARCHAR(64),
  feature_name VARCHAR(200),
  category VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  preconditions VARCHAR(2000),
  steps VARCHAR(4000),
  expected_result VARCHAR(2000),
  priority VARCHAR(16),
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_qa_acceptance_test_case UNIQUE (tenant_id, case_code)
);

-- 验收测试结果
CREATE TABLE IF NOT EXISTS qa_acceptance_test_result (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  result_code VARCHAR(64) NOT NULL,
  test_case_id BIGINT,
  case_code VARCHAR(64),
  case_name VARCHAR(200),
  feature_code VARCHAR(64),
  category VARCHAR(32),
  verdict VARCHAR(16),
  actual_result VARCHAR(4000),
  deviation VARCHAR(2000),
  evidence_refs VARCHAR(4000),
  environment VARCHAR(200),
  executed_by VARCHAR(64),
  executed_time TIMESTAMP,
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  status VARCHAR(16) DEFAULT 'EXECUTED' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_qa_acceptance_test_result UNIQUE (tenant_id, result_code)
);

-- 验收证据
CREATE TABLE IF NOT EXISTS qa_acceptance_evidence (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  evidence_code VARCHAR(64) NOT NULL,
  result_code VARCHAR(64),
  case_code VARCHAR(64),
  evidence_type VARCHAR(32),
  description VARCHAR(2000),
  file_path VARCHAR(500),
  file_hash VARCHAR(128),
  file_size BIGINT DEFAULT 0,
  mime_type VARCHAR(128),
  captured_by VARCHAR(64),
  captured_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_qa_acceptance_evidence UNIQUE (tenant_id, evidence_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_qa_result_case_verdict ON qa_acceptance_test_result (tenant_id, case_code, verdict);
CREATE INDEX IF NOT EXISTS idx_qa_result_feature_status ON qa_acceptance_test_result (tenant_id, feature_code, status);
CREATE INDEX IF NOT EXISTS idx_qa_evidence_result ON qa_acceptance_evidence (tenant_id, result_code);
