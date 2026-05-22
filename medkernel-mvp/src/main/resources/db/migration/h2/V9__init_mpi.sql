-- H2 local file database DDL for MPI (Patient Master Index) module.
-- Oracle/DM DDL remains the production authority. This file mirrors the core
-- tables needed by the patient index and identifier governance module.
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

-- ============================================================================
-- 患者标识映射表
-- ============================================================================

-- 患者标识映射表：记录同一患者在不同系统中的标识映射关系
CREATE TABLE IF NOT EXISTS mpi_patient_identity (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  -- 平台内部患者ID（关联 md_patient.patient_id）
  platform_patient_id VARCHAR(64) NOT NULL,
  -- 外部系统标识类型：HIS_PATIENT_ID / EMR_PATIENT_ID / INSURANCE_ID / OUTPATIENT_ID / INPATIENT_ID / PHYSICAL_CARD_NO
  identity_type VARCHAR(64) NOT NULL,
  -- 外部系统中的患者标识值
  external_id VARCHAR(128) NOT NULL,
  -- 外部标识的脱敏 hash（SHA-256），用于安全比对
  id_hash VARCHAR(64) NOT NULL,
  -- 来源系统编码（关联 adp_adapter_def.adapter_code）
  source_system VARCHAR(64) NOT NULL,
  -- 标识状态：ACTIVE / INACTIVE / MERGED / CONFLICT
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  -- 置信度（0-100），自动匹配时的可信程度
  confidence INTEGER DEFAULT 100,
  -- 是否人工确认
  manually_verified SMALLINT DEFAULT 0,
  -- 验证人
  verified_by VARCHAR(64),
  -- 验证时间
  verified_time TIMESTAMP,
  -- 合并目标（当 status=MERGED 时，指向合并到的目标记录ID）
  merged_to_id BIGINT,
  -- 备注
  remarks VARCHAR(500),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_patient_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

-- 患者标识映射表索引
CREATE INDEX IF NOT EXISTS idx_mpi_pi_platform ON mpi_patient_identity(tenant_id, platform_patient_id);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_hash ON mpi_patient_identity(tenant_id, id_hash);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_source ON mpi_patient_identity(tenant_id, source_system);
CREATE INDEX IF NOT EXISTS idx_mpi_pi_status ON mpi_patient_identity(tenant_id, status);

-- ============================================================================
-- 就诊标识映射表
-- ============================================================================

-- 就诊标识映射表：记录同一就诊事件在不同系统中的标识映射关系
CREATE TABLE IF NOT EXISTS mpi_visit_identity (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  -- 平台内部就诊ID
  platform_visit_id VARCHAR(64) NOT NULL,
  -- 关联的平台患者ID
  platform_patient_id VARCHAR(64) NOT NULL,
  -- 就诊类型：OUTPATIENT / INPATIENT / EMERGENCY / PHYSICAL_EXAM
  visit_type VARCHAR(32) NOT NULL,
  -- 外部系统标识类型：HIS_VISIT_ID / EMR_VISIT_ID / INSURANCE_SETTLEMENT_ID / OUTPATIENT_NO / INPATIENT_NO
  identity_type VARCHAR(64) NOT NULL,
  -- 外部系统中的就诊标识值
  external_id VARCHAR(128) NOT NULL,
  -- 外部标识的脱敏 hash（SHA-256）
  id_hash VARCHAR(64) NOT NULL,
  -- 来源系统编码
  source_system VARCHAR(64) NOT NULL,
  -- 就诊日期
  visit_date DATE,
  -- 就诊科室
  department_code VARCHAR(64),
  -- 标识状态：ACTIVE / INACTIVE / MERGED
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  -- 备注
  remarks VARCHAR(500),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mpi_visit_identity UNIQUE (tenant_id, identity_type, source_system, external_id)
);

-- 就诊标识映射表索引
CREATE INDEX IF NOT EXISTS idx_mpi_vi_platform ON mpi_visit_identity(tenant_id, platform_visit_id);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_patient ON mpi_visit_identity(tenant_id, platform_patient_id);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_hash ON mpi_visit_identity(tenant_id, id_hash);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_source ON mpi_visit_identity(tenant_id, source_system);
CREATE INDEX IF NOT EXISTS idx_mpi_vi_type ON mpi_visit_identity(tenant_id, visit_type);

-- ============================================================================
-- 标识冲突处理表
-- ============================================================================

-- 标识冲突表：记录标识匹配过程中的冲突，支持人工处理
CREATE TABLE IF NOT EXISTS mpi_identity_conflict (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  -- 冲突类型：DUPLICATE_EXTERNAL / MULTIPLE_PLATFORM / HASH_MISMATCH / MANUAL_REVIEW
  conflict_type VARCHAR(64) NOT NULL,
  -- 冲突严重级别：HIGH / MEDIUM / LOW
  severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  -- 涉及的患者标识ID（JSON数组）
  patient_identity_ids VARCHAR(1000),
  -- 涉及的就诊标识ID（JSON数组）
  visit_identity_ids VARCHAR(1000),
  -- 冲突描述
  conflict_description VARCHAR(1000) NOT NULL,
  -- 处理状态：PENDING / IN_PROGRESS / RESOLVED / DISMISSED
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  -- 处理方式：MERGE / SPLIT / KEEP_BOTH / MANUAL_LINK
  resolution_type VARCHAR(32),
  -- 处理结果说明
  resolution_notes VARCHAR(1000),
  -- 处理人
  resolved_by VARCHAR(64),
  -- 处理时间
  resolved_time TIMESTAMP,
  -- 处理后的目标患者标识ID（合并时）
  target_patient_identity_id BIGINT,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 标识冲突表索引
CREATE INDEX IF NOT EXISTS idx_mpi_ic_tenant ON mpi_identity_conflict(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mpi_ic_type ON mpi_identity_conflict(tenant_id, conflict_type);
CREATE INDEX IF NOT EXISTS idx_mpi_ic_severity ON mpi_identity_conflict(tenant_id, severity);
