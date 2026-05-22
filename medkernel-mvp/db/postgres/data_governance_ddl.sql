-- PostgreSQL DDL for data governance module.
-- Production authority for PostgreSQL database.

-- ============================================================================
-- 主数据表
-- ============================================================================

-- 患者主数据表
-- PR-FINAL-23: HEALTH_DATA 字段 SM4 加密 — patient_name / id_card_no / phone / address 存密文
CREATE TABLE md_patient (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  patient_id VARCHAR(64) NOT NULL,
  patient_name VARCHAR(256) NOT NULL,
  gender VARCHAR(10),
  birth_date DATE,
  id_card_no VARCHAR(256),
  phone VARCHAR(256),
  address VARCHAR(1024),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_patient UNIQUE (tenant_id, patient_id)
);

-- 医生主数据表
CREATE TABLE md_doctor (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  doctor_id VARCHAR(64) NOT NULL,
  doctor_name VARCHAR(100) NOT NULL,
  gender VARCHAR(10),
  title VARCHAR(64),
  specialty_code VARCHAR(64),
  department_code VARCHAR(64),
  license_no VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_doctor UNIQUE (tenant_id, doctor_id)
);

-- 科室主数据表
CREATE TABLE md_department (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  dept_code VARCHAR(64) NOT NULL,
  dept_name VARCHAR(200) NOT NULL,
  dept_type VARCHAR(32),
  parent_dept_code VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_department UNIQUE (tenant_id, dept_code)
);

-- 诊断主数据表
CREATE TABLE md_diagnosis (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  diagnosis_code VARCHAR(64) NOT NULL,
  diagnosis_name VARCHAR(200) NOT NULL,
  standard_code VARCHAR(64),
  standard_system VARCHAR(32),
  category VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_diagnosis UNIQUE (tenant_id, diagnosis_code)
);

-- 医嘱主数据表
CREATE TABLE md_order (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  order_code VARCHAR(64) NOT NULL,
  order_name VARCHAR(200) NOT NULL,
  order_type VARCHAR(32),
  standard_code VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_order UNIQUE (tenant_id, order_code)
);

-- 药品主数据表
CREATE TABLE md_drug (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  drug_code VARCHAR(64) NOT NULL,
  drug_name VARCHAR(200) NOT NULL,
  generic_name VARCHAR(200),
  specification VARCHAR(200),
  manufacturer VARCHAR(200),
  approval_no VARCHAR(64),
  national_code VARCHAR(64),
  medical_insurance_code VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_drug UNIQUE (tenant_id, drug_code)
);

-- 医保主数据表
CREATE TABLE md_insurance (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  insurance_code VARCHAR(64) NOT NULL,
  insurance_name VARCHAR(200) NOT NULL,
  insurance_type VARCHAR(32),
  region_code VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_insurance UNIQUE (tenant_id, insurance_code)
);

-- 知识资产主数据表
CREATE TABLE md_knowledge_asset (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  asset_code VARCHAR(128) NOT NULL,
  asset_name VARCHAR(200) NOT NULL,
  asset_type VARCHAR(32),
  version VARCHAR(32),
  source_org VARCHAR(200),
  publish_date DATE,
  effective_date DATE,
  expiry_date DATE,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_md_knowledge_asset UNIQUE (tenant_id, asset_code)
);

-- ============================================================================
-- 数据字典表
-- ============================================================================

-- 数据字典分类表
CREATE TABLE dg_dict_category (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(200) NOT NULL,
  parent_category_code VARCHAR(64),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_dg_dict_category UNIQUE (tenant_id, category_code)
);

-- 数据字典项表
CREATE TABLE dg_dict_item (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  category_code VARCHAR(64) NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(200) NOT NULL,
  item_value VARCHAR(500),
  display_order INTEGER DEFAULT 0 NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_dg_dict_item UNIQUE (tenant_id, category_code, item_code)
);

-- ============================================================================
-- 数据质量表
-- ============================================================================

-- 数据质量规则表
CREATE TABLE dg_quality_rule (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  rule_type VARCHAR(32) NOT NULL,
  target_entity VARCHAR(64) NOT NULL,
  target_field VARCHAR(64),
  rule_expression TEXT,
  severity VARCHAR(32) DEFAULT 'WARNING' NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_dg_quality_rule UNIQUE (tenant_id, rule_code)
);

-- 数据质量检查记录表
CREATE TABLE dg_quality_check (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  check_id VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  target_entity VARCHAR(64) NOT NULL,
  target_id VARCHAR(64) NOT NULL,
  check_result VARCHAR(32) NOT NULL,
  error_message VARCHAR(1000),
  check_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_dg_quality_check UNIQUE (tenant_id, check_id)
);

-- ============================================================================
-- 索引
-- ============================================================================

-- 主数据索引
CREATE INDEX idx_md_patient_tenant ON md_patient(tenant_id, status);
CREATE INDEX idx_md_doctor_tenant ON md_doctor(tenant_id, status);
CREATE INDEX idx_md_department_tenant ON md_department(tenant_id, status);
CREATE INDEX idx_md_diagnosis_tenant ON md_diagnosis(tenant_id, status);
CREATE INDEX idx_md_order_tenant ON md_order(tenant_id, status);
CREATE INDEX idx_md_drug_tenant ON md_drug(tenant_id, status);
CREATE INDEX idx_md_insurance_tenant ON md_insurance(tenant_id, status);
CREATE INDEX idx_md_knowledge_asset_tenant ON md_knowledge_asset(tenant_id, status);

-- 数据字典索引
CREATE INDEX idx_dg_dict_category_tenant ON dg_dict_category(tenant_id, status);
CREATE INDEX idx_dg_dict_item_tenant ON dg_dict_item(tenant_id, category_code, status);

-- 数据质量索引
CREATE INDEX idx_dg_quality_rule_tenant ON dg_quality_rule(tenant_id, status);
CREATE INDEX idx_dg_quality_check_tenant ON dg_quality_check(tenant_id, rule_code, check_result);
CREATE INDEX idx_dg_quality_check_time ON dg_quality_check(tenant_id, check_time);