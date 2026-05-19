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
COMMENT ON TABLE mpi_patient_identity IS UNISTR('\u60A3\u8005\u6807\u8BC6\u6620\u5C04\u8868');
COMMENT ON COLUMN mpi_patient_identity.id IS UNISTR('\u4E3B\u952E');
COMMENT ON COLUMN mpi_patient_identity.tenant_id IS UNISTR('\u79DF\u6237ID');
COMMENT ON COLUMN mpi_patient_identity.platform_patient_id IS UNISTR('\u5E73\u53F0\u5185\u90E8\u60A3\u8005ID');
COMMENT ON COLUMN mpi_patient_identity.identity_type IS UNISTR('\u6807\u8BC6\u7C7B\u578B');
COMMENT ON COLUMN mpi_patient_identity.external_id IS UNISTR('\u5916\u90E8\u6807\u8BC6\u503C');
COMMENT ON COLUMN mpi_patient_identity.id_hash IS UNISTR('\u8131\u654Fhash');
COMMENT ON COLUMN mpi_patient_identity.source_system IS UNISTR('\u6765\u6E90\u7CFB\u7EDF');
COMMENT ON COLUMN mpi_patient_identity.status IS UNISTR('\u72B6\u6001');
COMMENT ON COLUMN mpi_patient_identity.confidence IS UNISTR('\u7F6E\u4FE1\u5EA6');
COMMENT ON COLUMN mpi_patient_identity.manually_verified IS UNISTR('\u662F\u5426\u4EBA\u5DE5\u786E\u8BA4');
COMMENT ON COLUMN mpi_patient_identity.verified_by IS UNISTR('\u9A8C\u8BC1\u4EBA');
COMMENT ON COLUMN mpi_patient_identity.verified_time IS UNISTR('\u9A8C\u8BC1\u65F6\u95F4');
COMMENT ON COLUMN mpi_patient_identity.merged_to_id IS UNISTR('\u5408\u5E76\u76EE\u6807ID');
COMMENT ON COLUMN mpi_patient_identity.remarks IS UNISTR('\u5907\u6CE8');
COMMENT ON COLUMN mpi_patient_identity.created_time IS UNISTR('\u521B\u5EFA\u65F6\u95F4');
COMMENT ON COLUMN mpi_patient_identity.updated_time IS UNISTR('\u66F4\u65B0\u65F6\u95F4');

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

COMMENT ON TABLE mpi_visit_identity IS UNISTR('\u5C31\u8BCA\u6807\u8BC6\u6620\u5C04\u8868');
COMMENT ON COLUMN mpi_visit_identity.id IS UNISTR('\u4E3B\u952E');
COMMENT ON COLUMN mpi_visit_identity.tenant_id IS UNISTR('\u79DF\u6237ID');
COMMENT ON COLUMN mpi_visit_identity.platform_visit_id IS UNISTR('\u5E73\u53F0\u5C31\u8BCAID');
COMMENT ON COLUMN mpi_visit_identity.platform_patient_id IS UNISTR('\u5E73\u53F0\u60A3\u8005ID');
COMMENT ON COLUMN mpi_visit_identity.visit_type IS UNISTR('\u5C31\u8BCA\u7C7B\u578B');
COMMENT ON COLUMN mpi_visit_identity.identity_type IS UNISTR('\u6807\u8BC6\u7C7B\u578B');
COMMENT ON COLUMN mpi_visit_identity.external_id IS UNISTR('\u5916\u90E8\u6807\u8BC6\u503C');
COMMENT ON COLUMN mpi_visit_identity.id_hash IS UNISTR('\u8131\u654Fhash');
COMMENT ON COLUMN mpi_visit_identity.source_system IS UNISTR('\u6765\u6E90\u7CFB\u7EDF');
COMMENT ON COLUMN mpi_visit_identity.visit_date IS UNISTR('\u5C31\u8BCA\u65E5\u671F');
COMMENT ON COLUMN mpi_visit_identity.department_code IS UNISTR('\u5C31\u8BCA\u79D1\u5BA4');
COMMENT ON COLUMN mpi_visit_identity.status IS UNISTR('\u72B6\u6001');
COMMENT ON COLUMN mpi_visit_identity.remarks IS UNISTR('\u5907\u6CE8');
COMMENT ON COLUMN mpi_visit_identity.created_time IS UNISTR('\u521B\u5EFA\u65F6\u95F4');
COMMENT ON COLUMN mpi_visit_identity.updated_time IS UNISTR('\u66F4\u65B0\u65F6\u95F4');

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

COMMENT ON TABLE mpi_identity_conflict IS UNISTR('\u6807\u8BC6\u51B2\u7A81\u5904\u7406\u8868');
COMMENT ON COLUMN mpi_identity_conflict.id IS UNISTR('\u4E3B\u952E');
COMMENT ON COLUMN mpi_identity_conflict.tenant_id IS UNISTR('\u79DF\u6237ID');
COMMENT ON COLUMN mpi_identity_conflict.conflict_type IS UNISTR('\u51B2\u7A81\u7C7B\u578B');
COMMENT ON COLUMN mpi_identity_conflict.severity IS UNISTR('\u4E25\u91CD\u7EA7\u522B');
COMMENT ON COLUMN mpi_identity_conflict.patient_identity_ids IS UNISTR('\u6D89\u53CA\u60A3\u8005\u6807\u8BC6ID');
COMMENT ON COLUMN mpi_identity_conflict.visit_identity_ids IS UNISTR('\u6D89\u53CA\u5C31\u8BCA\u6807\u8BC6ID');
COMMENT ON COLUMN mpi_identity_conflict.conflict_description IS UNISTR('\u51B2\u7A81\u63CF\u8FF0');
COMMENT ON COLUMN mpi_identity_conflict.status IS UNISTR('\u5904\u7406\u72B6\u6001');
COMMENT ON COLUMN mpi_identity_conflict.resolution_type IS UNISTR('\u5904\u7406\u65B9\u5F0F');
COMMENT ON COLUMN mpi_identity_conflict.resolution_notes IS UNISTR('\u5904\u7406\u7ED3\u679C\u8BF4\u660E');
COMMENT ON COLUMN mpi_identity_conflict.resolved_by IS UNISTR('\u5904\u7406\u4EBA');
COMMENT ON COLUMN mpi_identity_conflict.resolved_time IS UNISTR('\u5904\u7406\u65F6\u95F4');
COMMENT ON COLUMN mpi_identity_conflict.target_patient_identity_id IS UNISTR('\u76EE\u6807\u60A3\u8005\u6807\u8BC6ID');
COMMENT ON COLUMN mpi_identity_conflict.created_time IS UNISTR('\u521B\u5EFA\u65F6\u95F4');
COMMENT ON COLUMN mpi_identity_conflict.updated_time IS UNISTR('\u66F4\u65B0\u65F6\u95F4');
