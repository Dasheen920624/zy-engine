-- Oracle DDL for MPI (Patient Master Index) module.
-- This is the production authority DDL. H2/DM/PG files mirror this structure.

-- ============================================================================
-- 患者标识映射表
-- ============================================================================

CREATE TABLE mpi_patient_identity (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  platform_patient_id VARCHAR2(64) NOT NULL,
  identity_type VARCHAR2(64) NOT NULL,
  external_id VARCHAR2(128) NOT NULL,
  id_hash VARCHAR2(64) NOT NULL,
  source_system VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  confidence NUMBER(3) DEFAULT 100,
  manually_verified NUMBER(1) DEFAULT 0,
  verified_by VARCHAR2(64),
  verified_time TIMESTAMP,
  merged_to_id NUMBER(20),
  remarks VARCHAR2(500),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_patient_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

CREATE INDEX idx_mpi_pi_platform ON mpi_patient_identity(tenant_id, platform_patient_id);
CREATE INDEX idx_mpi_pi_hash ON mpi_patient_identity(tenant_id, id_hash);
CREATE INDEX idx_mpi_pi_source ON mpi_patient_identity(tenant_id, source_system);
CREATE INDEX idx_mpi_pi_status ON mpi_patient_identity(tenant_id, status);

-- 中文注释
COMMENT ON TABLE mpi_patient_identity IS '患者标识映射表';
COMMENT ON COLUMN mpi_patient_identity.id IS '主键';
COMMENT ON COLUMN mpi_patient_identity.tenant_id IS '租户ID';
COMMENT ON COLUMN mpi_patient_identity.platform_patient_id IS '平台内部患者ID';
COMMENT ON COLUMN mpi_patient_identity.identity_type IS '标识类型';
COMMENT ON COLUMN mpi_patient_identity.external_id IS '外部标识值';
COMMENT ON COLUMN mpi_patient_identity.id_hash IS '脱敏hash';
COMMENT ON COLUMN mpi_patient_identity.source_system IS '来源系统';
COMMENT ON COLUMN mpi_patient_identity.status IS '状态';
COMMENT ON COLUMN mpi_patient_identity.confidence IS '置信度';
COMMENT ON COLUMN mpi_patient_identity.manually_verified IS '是否人工确认';
COMMENT ON COLUMN mpi_patient_identity.verified_by IS '验证人';
COMMENT ON COLUMN mpi_patient_identity.verified_time IS '验证时间';
COMMENT ON COLUMN mpi_patient_identity.merged_to_id IS '合并目标ID';
COMMENT ON COLUMN mpi_patient_identity.remarks IS '备注';
COMMENT ON COLUMN mpi_patient_identity.created_time IS '创建时间';
COMMENT ON COLUMN mpi_patient_identity.updated_time IS '更新时间';

-- ============================================================================
-- 就诊标识映射表
-- ============================================================================

CREATE TABLE mpi_visit_identity (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  platform_visit_id VARCHAR2(64) NOT NULL,
  platform_patient_id VARCHAR2(64) NOT NULL,
  visit_type VARCHAR2(32) NOT NULL,
  identity_type VARCHAR2(64) NOT NULL,
  external_id VARCHAR2(128) NOT NULL,
  id_hash VARCHAR2(64) NOT NULL,
  source_system VARCHAR2(64) NOT NULL,
  visit_date DATE,
  department_code VARCHAR2(64),
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  remarks VARCHAR2(500),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_visit_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

CREATE INDEX idx_mpi_vi_platform ON mpi_visit_identity(tenant_id, platform_visit_id);
CREATE INDEX idx_mpi_vi_patient ON mpi_visit_identity(tenant_id, platform_patient_id);
CREATE INDEX idx_mpi_vi_hash ON mpi_visit_identity(tenant_id, id_hash);
CREATE INDEX idx_mpi_vi_source ON mpi_visit_identity(tenant_id, source_system);
CREATE INDEX idx_mpi_vi_type ON mpi_visit_identity(tenant_id, visit_type);

COMMENT ON TABLE mpi_visit_identity IS '就诊标识映射表';
COMMENT ON COLUMN mpi_visit_identity.id IS '主键';
COMMENT ON COLUMN mpi_visit_identity.tenant_id IS '租户ID';
COMMENT ON COLUMN mpi_visit_identity.platform_visit_id IS '平台就诊ID';
COMMENT ON COLUMN mpi_visit_identity.platform_patient_id IS '平台患者ID';
COMMENT ON COLUMN mpi_visit_identity.visit_type IS '就诊类型';
COMMENT ON COLUMN mpi_visit_identity.identity_type IS '标识类型';
COMMENT ON COLUMN mpi_visit_identity.external_id IS '外部标识值';
COMMENT ON COLUMN mpi_visit_identity.id_hash IS '脱敏hash';
COMMENT ON COLUMN mpi_visit_identity.source_system IS '来源系统';
COMMENT ON COLUMN mpi_visit_identity.visit_date IS '就诊日期';
COMMENT ON COLUMN mpi_visit_identity.department_code IS '就诊科室';
COMMENT ON COLUMN mpi_visit_identity.status IS '状态';
COMMENT ON COLUMN mpi_visit_identity.remarks IS '备注';
COMMENT ON COLUMN mpi_visit_identity.created_time IS '创建时间';
COMMENT ON COLUMN mpi_visit_identity.updated_time IS '更新时间';

-- ============================================================================
-- 标识冲突处理表
-- ============================================================================

CREATE TABLE mpi_identity_conflict (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  conflict_type VARCHAR2(64) NOT NULL,
  severity VARCHAR2(16) DEFAULT 'MEDIUM' NOT NULL,
  patient_identity_ids VARCHAR2(1000),
  visit_identity_ids VARCHAR2(1000),
  conflict_description VARCHAR2(1000) NOT NULL,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  resolution_type VARCHAR2(32),
  resolution_notes VARCHAR2(1000),
  resolved_by VARCHAR2(64),
  resolved_time TIMESTAMP,
  target_patient_identity_id NUMBER(20),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

CREATE INDEX idx_mpi_ic_tenant ON mpi_identity_conflict(tenant_id, status);
CREATE INDEX idx_mpi_ic_type ON mpi_identity_conflict(tenant_id, conflict_type);
CREATE INDEX idx_mpi_ic_severity ON mpi_identity_conflict(tenant_id, severity);

COMMENT ON TABLE mpi_identity_conflict IS '标识冲突处理表';
COMMENT ON COLUMN mpi_identity_conflict.id IS '主键';
COMMENT ON COLUMN mpi_identity_conflict.tenant_id IS '租户ID';
COMMENT ON COLUMN mpi_identity_conflict.conflict_type IS '冲突类型';
COMMENT ON COLUMN mpi_identity_conflict.severity IS '严重级别';
COMMENT ON COLUMN mpi_identity_conflict.patient_identity_ids IS '涉及患者标识ID';
COMMENT ON COLUMN mpi_identity_conflict.visit_identity_ids IS '涉及就诊标识ID';
COMMENT ON COLUMN mpi_identity_conflict.conflict_description IS '冲突描述';
COMMENT ON COLUMN mpi_identity_conflict.status IS '处理状态';
COMMENT ON COLUMN mpi_identity_conflict.resolution_type IS '处理方式';
COMMENT ON COLUMN mpi_identity_conflict.resolution_notes IS '处理结果说明';
COMMENT ON COLUMN mpi_identity_conflict.resolved_by IS '处理人';
COMMENT ON COLUMN mpi_identity_conflict.resolved_time IS '处理时间';
COMMENT ON COLUMN mpi_identity_conflict.target_patient_identity_id IS '目标患者标识ID';
COMMENT ON COLUMN mpi_identity_conflict.created_time IS '创建时间';
COMMENT ON COLUMN mpi_identity_conflict.updated_time IS '更新时间';
