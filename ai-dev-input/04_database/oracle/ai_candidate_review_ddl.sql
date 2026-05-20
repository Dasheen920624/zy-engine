-- FE-AI-001 AI候选配置审核台 DDL (Oracle)

CREATE TABLE ai_candidate_review (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  candidate_code VARCHAR2(64) NOT NULL,
  candidate_type VARCHAR2(32) NOT NULL,
  candidate_name VARCHAR2(200),
  source_code VARCHAR2(64),
  source_name VARCHAR2(200),
  model_provider VARCHAR2(64),
  model_name VARCHAR2(128),
  confidence NUMBER(5,4),
  candidate_content VARCHAR2(4000),
  review_status VARCHAR2(16) DEFAULT 'PENDING' NOT NULL,
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  modified_content VARCHAR2(4000),
  quality_findings VARCHAR2(2000),
  priority VARCHAR2(16) DEFAULT 'MEDIUM',
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
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

CREATE INDEX idx_ai_candidate_review_tenant ON ai_candidate_review (tenant_id, candidate_type, review_status);
CREATE INDEX idx_ai_candidate_review_status ON ai_candidate_review (tenant_id, review_status, priority);
