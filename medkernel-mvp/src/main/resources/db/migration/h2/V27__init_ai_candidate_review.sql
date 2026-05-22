-- FE-AI-001 AI候选配置审核台 DDL (H2 本地开发库)

CREATE TABLE IF NOT EXISTS ai_candidate_review (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  candidate_code VARCHAR(64) NOT NULL,
  candidate_type VARCHAR(32) NOT NULL,
  candidate_name VARCHAR(200),
  source_code VARCHAR(64),
  source_name VARCHAR(200),
  model_provider VARCHAR(64),
  model_name VARCHAR(128),
  confidence DECIMAL(5,4),
  candidate_content VARCHAR(4000),
  review_status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  modified_content VARCHAR(4000),
  quality_findings VARCHAR(2000),
  priority VARCHAR(16) DEFAULT 'MEDIUM',
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_candidate_review UNIQUE (tenant_id, candidate_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_ai_candidate_review_tenant ON ai_candidate_review (tenant_id, candidate_type, review_status);
CREATE INDEX IF NOT EXISTS idx_ai_candidate_review_status ON ai_candidate_review (tenant_id, review_status, priority);
