-- FE-AI-001 AI候选配置审核台 DDL (DM 达梦)

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

COMMENT ON TABLE ai_candidate_review IS 'AI候选配置审核表';
COMMENT ON COLUMN ai_candidate_review.id IS '主键ID';
COMMENT ON COLUMN ai_candidate_review.tenant_id IS '租户ID';
COMMENT ON COLUMN ai_candidate_review.candidate_code IS '候选编码';
COMMENT ON COLUMN ai_candidate_review.candidate_type IS '候选类型';
COMMENT ON COLUMN ai_candidate_review.candidate_name IS '候选名称';
COMMENT ON COLUMN ai_candidate_review.source_code IS '来源编码';
COMMENT ON COLUMN ai_candidate_review.source_name IS '来源名称';
COMMENT ON COLUMN ai_candidate_review.model_provider IS '模型供应商';
COMMENT ON COLUMN ai_candidate_review.model_name IS '模型名称';
COMMENT ON COLUMN ai_candidate_review.confidence IS '置信度';
COMMENT ON COLUMN ai_candidate_review.candidate_content IS '候选内容';
COMMENT ON COLUMN ai_candidate_review.review_status IS '审核状态: PENDING/APPROVED/REJECTED/MODIFIED';
COMMENT ON COLUMN ai_candidate_review.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_candidate_review.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_candidate_review.review_note IS '审核备注';
COMMENT ON COLUMN ai_candidate_review.modified_content IS '修改后内容';
COMMENT ON COLUMN ai_candidate_review.quality_findings IS '质检发现';
COMMENT ON COLUMN ai_candidate_review.priority IS '优先级: HIGH/MEDIUM/LOW';
COMMENT ON COLUMN ai_candidate_review.created_by IS '创建人';
COMMENT ON COLUMN ai_candidate_review.created_time IS '创建时间';
COMMENT ON COLUMN ai_candidate_review.updated_by IS '更新人';
COMMENT ON COLUMN ai_candidate_review.updated_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_ai_candidate_review_tenant ON ai_candidate_review (tenant_id, candidate_type, review_status);
CREATE INDEX IF NOT EXISTS idx_ai_candidate_review_status ON ai_candidate_review (tenant_id, review_status, priority);
