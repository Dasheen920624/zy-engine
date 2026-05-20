-- RISK-001 临床安全 DDL (Oracle)

-- 危险日志
CREATE TABLE clinical_hazard_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  hazard_code VARCHAR2(64) NOT NULL,
  hazard_name VARCHAR2(200) NOT NULL,
  hazard_category VARCHAR2(32) NOT NULL,
  hazard_description VARCHAR2(2000),
  affected_process VARCHAR2(32),
  likelihood VARCHAR2(32),
  severity VARCHAR2(32),
  risk_level VARCHAR2(16),
  control_measures VARCHAR2(2000),
  residual_risk VARCHAR2(16),
  status VARCHAR2(16) DEFAULT 'IDENTIFIED' NOT NULL,
  accepted_by VARCHAR2(64),
  accepted_time TIMESTAMP,
  acceptance_note VARCHAR2(2000),
  blocking_strategy VARCHAR2(32),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
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
CREATE TABLE clinical_safety_case (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  case_code VARCHAR2(64) NOT NULL,
  case_name VARCHAR2(200) NOT NULL,
  case_type VARCHAR2(32),
  scope VARCHAR2(2000),
  goal VARCHAR2(2000),
  argument CLOB,
  evidence_refs CLOB,
  status VARCHAR2(16) DEFAULT 'DRAFT' NOT NULL,
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  version VARCHAR2(32) DEFAULT '1.0.0',
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
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

CREATE INDEX idx_clinical_hazard_tenant ON clinical_hazard_log (tenant_id, hazard_category, risk_level);
CREATE INDEX idx_clinical_hazard_status ON clinical_hazard_log (tenant_id, status, risk_level);
CREATE INDEX idx_clinical_safety_case_tenant ON clinical_safety_case (tenant_id, case_type, status);
