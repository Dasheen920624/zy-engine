-- RISK-001 临床安全 DDL (DM 达梦)

-- 危险日志
CREATE TABLE IF NOT EXISTS clinical_hazard_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  hazard_code VARCHAR(64) NOT NULL,
  hazard_name VARCHAR(200) NOT NULL,
  hazard_category VARCHAR(32) NOT NULL,
  hazard_description VARCHAR(2000),
  affected_process VARCHAR(32),
  likelihood VARCHAR(32),
  severity VARCHAR(32),
  risk_level VARCHAR(16),
  control_measures VARCHAR(2000),
  residual_risk VARCHAR(16),
  status VARCHAR(16) DEFAULT 'IDENTIFIED' NOT NULL,
  accepted_by VARCHAR(64),
  accepted_time TIMESTAMP,
  acceptance_note VARCHAR(2000),
  blocking_strategy VARCHAR(32),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_hazard_log UNIQUE (tenant_id, hazard_code)
);

COMMENT ON TABLE clinical_hazard_log IS '危险日志';
COMMENT ON COLUMN clinical_hazard_log.hazard_code IS '危险编码';
COMMENT ON COLUMN clinical_hazard_log.hazard_name IS '危险名称';
COMMENT ON COLUMN clinical_hazard_log.hazard_category IS '危险分类';
COMMENT ON COLUMN clinical_hazard_log.hazard_description IS '危险描述';
COMMENT ON COLUMN clinical_hazard_log.affected_process IS '受影响流程';
COMMENT ON COLUMN clinical_hazard_log.likelihood IS '可能性';
COMMENT ON COLUMN clinical_hazard_log.severity IS '严重度';
COMMENT ON COLUMN clinical_hazard_log.risk_level IS '风险等级';
COMMENT ON COLUMN clinical_hazard_log.control_measures IS '控制措施';
COMMENT ON COLUMN clinical_hazard_log.residual_risk IS '残余风险';
COMMENT ON COLUMN clinical_hazard_log.status IS '状态: IDENTIFIED/ANALYSED/CONTROLLED/ACCEPTED/CLOSED';
COMMENT ON COLUMN clinical_hazard_log.accepted_by IS '接受人';
COMMENT ON COLUMN clinical_hazard_log.accepted_time IS '接受时间';
COMMENT ON COLUMN clinical_hazard_log.acceptance_note IS '接受备注';
COMMENT ON COLUMN clinical_hazard_log.blocking_strategy IS '阻断策略';
COMMENT ON COLUMN clinical_hazard_log.created_by IS '创建人';
COMMENT ON COLUMN clinical_hazard_log.created_time IS '创建时间';
COMMENT ON COLUMN clinical_hazard_log.updated_by IS '更新人';
COMMENT ON COLUMN clinical_hazard_log.updated_time IS '更新时间';

-- 安全案例
CREATE TABLE IF NOT EXISTS clinical_safety_case (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  case_code VARCHAR(64) NOT NULL,
  case_name VARCHAR(200) NOT NULL,
  case_type VARCHAR(32),
  scope VARCHAR(2000),
  goal VARCHAR(2000),
  argument CLOB,
  evidence_refs CLOB,
  status VARCHAR(16) DEFAULT 'DRAFT' NOT NULL,
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR(2000),
  version VARCHAR(32) DEFAULT '1.0.0',
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_safety_case UNIQUE (tenant_id, case_code, version)
);

COMMENT ON TABLE clinical_safety_case IS '安全案例';
COMMENT ON COLUMN clinical_safety_case.case_code IS '案例编码';
COMMENT ON COLUMN clinical_safety_case.case_name IS '案例名称';
COMMENT ON COLUMN clinical_safety_case.case_type IS '案例类型';
COMMENT ON COLUMN clinical_safety_case.scope IS '范围';
COMMENT ON COLUMN clinical_safety_case.goal IS '目标';
COMMENT ON COLUMN clinical_safety_case.argument IS '论证';
COMMENT ON COLUMN clinical_safety_case.evidence_refs IS '证据引用';
COMMENT ON COLUMN clinical_safety_case.status IS '状态: DRAFT/REVIEWED/APPROVED/ARCHIVED';
COMMENT ON COLUMN clinical_safety_case.reviewed_by IS '审核人';
COMMENT ON COLUMN clinical_safety_case.reviewed_time IS '审核时间';
COMMENT ON COLUMN clinical_safety_case.review_note IS '审核备注';
COMMENT ON COLUMN clinical_safety_case.version IS '版本';
COMMENT ON COLUMN clinical_safety_case.created_by IS '创建人';
COMMENT ON COLUMN clinical_safety_case.created_time IS '创建时间';
COMMENT ON COLUMN clinical_safety_case.updated_by IS '更新人';
COMMENT ON COLUMN clinical_safety_case.updated_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_clinical_hazard_tenant ON clinical_hazard_log (tenant_id, hazard_category, risk_level);
CREATE INDEX IF NOT EXISTS idx_clinical_hazard_status ON clinical_hazard_log (tenant_id, status, risk_level);
CREATE INDEX IF NOT EXISTS idx_clinical_safety_case_tenant ON clinical_safety_case (tenant_id, case_type, status);
