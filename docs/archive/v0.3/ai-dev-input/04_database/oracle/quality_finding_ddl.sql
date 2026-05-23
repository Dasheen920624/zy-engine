-- AIK-004 质量发现表 (Oracle)
-- 记录知识资产质量检测结果

CREATE TABLE quality_finding (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  finding_code VARCHAR2(64) NOT NULL,
  finding_type VARCHAR2(32) NOT NULL,
  severity VARCHAR2(16) DEFAULT 'WARNING',
  asset_type VARCHAR2(32) NOT NULL,
  asset_code VARCHAR2(128) NOT NULL,
  asset_name VARCHAR2(200),
  asset_version VARCHAR2(32),
  description VARCHAR2(2000),
  detail_json VARCHAR2(4000),
  detection_rule VARCHAR2(500),
  status VARCHAR2(16) DEFAULT 'OPEN' NOT NULL,
  resolved_by VARCHAR2(64),
  resolution_note VARCHAR2(2000),
  resolved_time TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_quality_finding UNIQUE (tenant_id, finding_code)
);

COMMENT ON TABLE quality_finding IS '质量发现表';
COMMENT ON COLUMN quality_finding.finding_code IS '发现编码';
COMMENT ON COLUMN quality_finding.finding_type IS '发现类型: MISSING_SOURCE/EXPIRED/UNCLEAR_AUTH/RULE_CONFLICT/LOW_CONFIDENCE/MULTI_CANDIDATE_CONFLICT';
COMMENT ON COLUMN quality_finding.severity IS '严重程度: INFO/WARNING/CRITICAL';
COMMENT ON COLUMN quality_finding.asset_type IS '资产类型: RULE/TERMINOLOGY_MAPPING/KNOWLEDGE_ASSET/PATHWAY';
COMMENT ON COLUMN quality_finding.asset_code IS '资产编码';
COMMENT ON COLUMN quality_finding.asset_name IS '资产名称';
COMMENT ON COLUMN quality_finding.asset_version IS '资产版本';
COMMENT ON COLUMN quality_finding.description IS '描述';
COMMENT ON COLUMN quality_finding.detail_json IS '详情JSON';
COMMENT ON COLUMN quality_finding.detection_rule IS '检测规则';
COMMENT ON COLUMN quality_finding.status IS '状态: OPEN/ACKNOWLEDGED/RESOLVED/DISMISSED';
COMMENT ON COLUMN quality_finding.resolved_by IS '处理人';
COMMENT ON COLUMN quality_finding.resolution_note IS '处理备注';
COMMENT ON COLUMN quality_finding.resolved_time IS '处理时间';
COMMENT ON COLUMN quality_finding.created_by IS '创建人';
COMMENT ON COLUMN quality_finding.created_time IS '创建时间';
COMMENT ON COLUMN quality_finding.updated_by IS '更新人';
COMMENT ON COLUMN quality_finding.updated_time IS '更新时间';

CREATE INDEX idx_quality_finding_tenant ON quality_finding (tenant_id, finding_type, status);
CREATE INDEX idx_quality_finding_asset ON quality_finding (tenant_id, asset_type, asset_code);
CREATE INDEX idx_quality_finding_severity ON quality_finding (tenant_id, severity, status);
