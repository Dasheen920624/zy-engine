-- PR-FINAL-25: KingbaseES V8 兼容 PostgreSQL 8.x 协议，本目录从 db/migration/postgres/ 复制。
-- 如发现 KingbaseES 实测差异，在此独立覆盖；正常情况两套保持同步。
-- PostgreSQL DDL for MPI (Patient Master Index) module.

-- ============================================================================
-- 患者标识映射表
-- ============================================================================

CREATE TABLE IF NOT EXISTS mpi_patient_identity (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  platform_patient_id VARCHAR(64) NOT NULL,
  identity_type VARCHAR(64) NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  id_hash VARCHAR(64) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  confidence INTEGER DEFAULT 100,
  manually_verified SMALLINT DEFAULT 0,
  verified_by VARCHAR(64),
  verified_time TIMESTAMP,
  merged_to_id BIGINT,
  remarks VARCHAR(500),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_patient_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

CREATE INDEX IF NOT EXISTS idx_mpi_pi_platform ON mpi_patient_identity(tenant_id, platform_patient_id);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_hash ON mpi_patient_identity(tenant_id, id_hash);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_source ON mpi_patient_identity(tenant_id, source_system);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_status ON mpi_patient_identity(tenant_id, status);

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

CREATE TABLE IF NOT EXISTS mpi_visit_identity (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  platform_visit_id VARCHAR(64) NOT NULL,
  platform_patient_id VARCHAR(64) NOT NULL,
  visit_type VARCHAR(32) NOT NULL,
  identity_type VARCHAR(64) NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  id_hash VARCHAR(64) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  visit_date DATE,
  department_code VARCHAR(64),
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  remarks VARCHAR(500),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_visit_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

CREATE INDEX IF NOT EXISTS idx_mpi_vi_platform ON mpi_visit_identity(tenant_id, platform_visit_id);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_patient ON mpi_visit_identity(tenant_id, platform_patient_id);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_hash ON mpi_visit_identity(tenant_id, id_hash);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_source ON mpi_visit_identity(tenant_id, source_system);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_type ON mpi_visit_identity(tenant_id, visit_type);

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

CREATE TABLE IF NOT EXISTS mpi_identity_conflict (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  conflict_type VARCHAR(64) NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  patient_identity_ids VARCHAR(1000),
  visit_identity_ids VARCHAR(1000),
  conflict_description VARCHAR(1000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  resolution_type VARCHAR(32),
  resolution_notes VARCHAR(1000),
  resolved_by VARCHAR(64),
  resolved_time TIMESTAMP,
  target_patient_identity_id BIGINT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mpi_ic_tenant ON mpi_identity_conflict(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mpi_ic_type ON mpi_identity_conflict(tenant_id, conflict_type);
CREATE INDEX IF NOT EXISTS idx_mpi_ic_severity ON mpi_identity_conflict(tenant_id, severity);

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
