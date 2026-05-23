-- TEST-005 全功能验收中心和证据归档 DDL (DM 达梦)

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
  steps CLOB,
  expected_result VARCHAR(2000),
  priority VARCHAR(16),
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_qa_acceptance_test_case UNIQUE (tenant_id, case_code)
);

COMMENT ON TABLE qa_acceptance_test_case IS '验收测试用例';
COMMENT ON COLUMN qa_acceptance_test_case.case_code IS '用例编码';
COMMENT ON COLUMN qa_acceptance_test_case.case_name IS '用例名称';
COMMENT ON COLUMN qa_acceptance_test_case.feature_code IS '功能编码';
COMMENT ON COLUMN qa_acceptance_test_case.feature_name IS '功能名称';
COMMENT ON COLUMN qa_acceptance_test_case.category IS '分类: FUNCTIONAL/SECURITY/PERFORMANCE/COMPLIANCE/AI_SAFETY';
COMMENT ON COLUMN qa_acceptance_test_case.description IS '描述';
COMMENT ON COLUMN qa_acceptance_test_case.preconditions IS '前置条件';
COMMENT ON COLUMN qa_acceptance_test_case.steps IS '测试步骤JSON';
COMMENT ON COLUMN qa_acceptance_test_case.expected_result IS '期望结果';
COMMENT ON COLUMN qa_acceptance_test_case.priority IS '优先级: HIGH/MEDIUM/LOW';
COMMENT ON COLUMN qa_acceptance_test_case.status IS '状态: DRAFT/ACTIVE/DISABLED';
COMMENT ON COLUMN qa_acceptance_test_case.created_by IS '创建人';
COMMENT ON COLUMN qa_acceptance_test_case.created_time IS '创建时间';
COMMENT ON COLUMN qa_acceptance_test_case.updated_by IS '更新人';
COMMENT ON COLUMN qa_acceptance_test_case.updated_time IS '更新时间';

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
  actual_result CLOB,
  deviation VARCHAR(2000),
  evidence_refs CLOB,
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

COMMENT ON TABLE qa_acceptance_test_result IS '验收测试结果';
COMMENT ON COLUMN qa_acceptance_test_result.result_code IS '结果编码';
COMMENT ON COLUMN qa_acceptance_test_result.test_case_id IS '测试用例ID';
COMMENT ON COLUMN qa_acceptance_test_result.case_code IS '用例编码';
COMMENT ON COLUMN qa_acceptance_test_result.case_name IS '用例名称';
COMMENT ON COLUMN qa_acceptance_test_result.feature_code IS '功能编码';
COMMENT ON COLUMN qa_acceptance_test_result.category IS '分类';
COMMENT ON COLUMN qa_acceptance_test_result.verdict IS '判定: PASS/FAIL/BLOCKED/SKIP';
COMMENT ON COLUMN qa_acceptance_test_result.actual_result IS '实际结果';
COMMENT ON COLUMN qa_acceptance_test_result.deviation IS '偏差描述';
COMMENT ON COLUMN qa_acceptance_test_result.evidence_refs IS '证据引用JSON';
COMMENT ON COLUMN qa_acceptance_test_result.environment IS '测试环境';
COMMENT ON COLUMN qa_acceptance_test_result.executed_by IS '执行人';
COMMENT ON COLUMN qa_acceptance_test_result.executed_time IS '执行时间';
COMMENT ON COLUMN qa_acceptance_test_result.reviewed_by IS '审核人';
COMMENT ON COLUMN qa_acceptance_test_result.reviewed_time IS '审核时间';
COMMENT ON COLUMN qa_acceptance_test_result.review_note IS '审核备注';
COMMENT ON COLUMN qa_acceptance_test_result.status IS '状态: EXECUTED/REVIEWED/ACCEPTED/REJECTED';
COMMENT ON COLUMN qa_acceptance_test_result.created_time IS '创建时间';

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

COMMENT ON TABLE qa_acceptance_evidence IS '验收证据';
COMMENT ON COLUMN qa_acceptance_evidence.evidence_code IS '证据编码';
COMMENT ON COLUMN qa_acceptance_evidence.result_code IS '关联测试结果编码';
COMMENT ON COLUMN qa_acceptance_evidence.case_code IS '用例编码';
COMMENT ON COLUMN qa_acceptance_evidence.evidence_type IS '证据类型: SCREENSHOT/LOG/REPORT/VIDEO/CONFIG_DUMP';
COMMENT ON COLUMN qa_acceptance_evidence.description IS '描述';
COMMENT ON COLUMN qa_acceptance_evidence.file_path IS '文件路径';
COMMENT ON COLUMN qa_acceptance_evidence.file_hash IS '文件哈希';
COMMENT ON COLUMN qa_acceptance_evidence.file_size IS '文件大小(字节)';
COMMENT ON COLUMN qa_acceptance_evidence.mime_type IS 'MIME类型';
COMMENT ON COLUMN qa_acceptance_evidence.captured_by IS '采集人';
COMMENT ON COLUMN qa_acceptance_evidence.captured_time IS '采集时间';
COMMENT ON COLUMN qa_acceptance_evidence.created_time IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_qa_result_case_verdict ON qa_acceptance_test_result (tenant_id, case_code, verdict);
CREATE INDEX IF NOT EXISTS idx_qa_result_feature_status ON qa_acceptance_test_result (tenant_id, feature_code, status);
CREATE INDEX IF NOT EXISTS idx_qa_evidence_result ON qa_acceptance_evidence (tenant_id, result_code);
