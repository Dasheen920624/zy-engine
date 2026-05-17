-- PostgreSQL / KingbaseES (PG 兼容模式) 核心 DDL（含中文备注）
-- 适用 PostgreSQL 14/15/16 与 KingbaseES V8
-- 设计原则：与 db/oracle 等价；ID 应用层生成；JSON 用 TEXT；时间 TIMESTAMP；布尔 SMALLINT 0/1
-- 字符集：库 ENCODING=UTF8，LC_COLLATE=zh_CN.UTF-8 或 C.UTF-8

SET client_encoding = 'UTF8';

-- ============================================================================
-- 组织目录
-- ============================================================================
CREATE TABLE IF NOT EXISTS org_unit (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  level_code VARCHAR(32) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  org_name VARCHAR(200) NOT NULL,
  parent_level_code VARCHAR(32),
  parent_org_code VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_org_unit UNIQUE (tenant_id, level_code, org_code)
);
COMMENT ON TABLE  org_unit IS '组织目录：集团/医院/院区/站点/科室五段式组织树';
COMMENT ON COLUMN org_unit.tenant_id IS '租户编码（顶层隔离）';
COMMENT ON COLUMN org_unit.level_code IS '组织层级：GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT';
COMMENT ON COLUMN org_unit.org_code IS '组织编码（在 level 范围内唯一）';
COMMENT ON COLUMN org_unit.parent_level_code IS '父级层级';
COMMENT ON COLUMN org_unit.parent_org_code IS '父级编码';

-- ============================================================================
-- 路径引擎
-- ============================================================================
CREATE TABLE IF NOT EXISTS pe_pathway_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  pathway_code VARCHAR(64) NOT NULL,
  pathway_name VARCHAR(200) NOT NULL,
  specialty_code VARCHAR(64),
  disease_code VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_pathway_def UNIQUE (tenant_id, org_code, pathway_code)
);
COMMENT ON TABLE  pe_pathway_def IS '路径定义主表';
COMMENT ON COLUMN pe_pathway_def.pathway_code IS '路径编码（如 AMI_STEMI）';

CREATE TABLE IF NOT EXISTS pe_pathway_version (
  id BIGINT PRIMARY KEY,
  pathway_code VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  config_json TEXT NOT NULL,
  effective_time TIMESTAMP,
  retired_time TIMESTAMP,
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_pe_pathway_version UNIQUE (pathway_code, version_no)
);
COMMENT ON TABLE  pe_pathway_version IS '路径版本与配置 JSON';
COMMENT ON COLUMN pe_pathway_version.config_json IS '路径完整配置 JSON（节点 / 任务 / 流转）';

CREATE TABLE IF NOT EXISTS pe_patient_instance (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  patient_id VARCHAR(64) NOT NULL,
  encounter_id VARCHAR(64) NOT NULL,
  pathway_code VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  current_node_code VARCHAR(64),
  admitted_by VARCHAR(64),
  admission_time TIMESTAMP,
  exit_time TIMESTAMP,
  exit_reason VARCHAR(1000),
  lock_version INT NOT NULL DEFAULT 0,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)
);
COMMENT ON TABLE pe_patient_instance IS '患者路径运行实例';

CREATE TABLE IF NOT EXISTS pe_patient_node_state (
  id BIGINT PRIMARY KEY,
  instance_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(200),
  status VARCHAR(32) NOT NULL,
  enter_time TIMESTAMP,
  complete_time TIMESTAMP,
  timeout_flag SMALLINT NOT NULL DEFAULT 0,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE pe_patient_node_state IS '患者节点运行状态';

CREATE TABLE IF NOT EXISTS pe_patient_task_state (
  id BIGINT PRIMARY KEY,
  instance_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_name VARCHAR(200),
  task_type VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  result_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP
);
COMMENT ON TABLE pe_patient_task_state IS '患者任务运行状态';

CREATE TABLE IF NOT EXISTS pe_variation_record (
  id BIGINT PRIMARY KEY,
  instance_id BIGINT NOT NULL,
  patient_id VARCHAR(64) NOT NULL,
  encounter_id VARCHAR(64) NOT NULL,
  node_code VARCHAR(64),
  variation_type VARCHAR(64) NOT NULL,
  reason VARCHAR(1000),
  operator_id VARCHAR(64),
  tenant_id VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  org_source VARCHAR(32),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE  pe_variation_record IS '路径变异记录（带组织上下文）';
COMMENT ON COLUMN pe_variation_record.org_source IS '组织来源：HEADER/QUERY/BODY/DEFAULT/NONE';

-- ============================================================================
-- 规则引擎
-- ============================================================================
CREATE TABLE IF NOT EXISTS re_rule_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  severity VARCHAR(32),
  rule_json TEXT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  CONSTRAINT uk_re_rule_def UNIQUE (tenant_id, org_code, rule_code, version_no)
);
COMMENT ON TABLE  re_rule_def IS '规则定义（按组织 + 版本独立）';
COMMENT ON COLUMN re_rule_def.rule_json IS '规则 DSL 与元数据 JSON（含 reference_* 来源字段）';

CREATE TABLE IF NOT EXISTS re_rule_exec_log (
  id BIGINT PRIMARY KEY,
  trace_id VARCHAR(128) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_version VARCHAR(32),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  event_id VARCHAR(128),
  hit_flag SMALLINT NOT NULL,
  severity VARCHAR(32),
  message VARCHAR(1000),
  input_snapshot TEXT,
  output_snapshot TEXT,
  elapsed_ms INT,
  result_status VARCHAR(32) NOT NULL,
  error_code VARCHAR(64),
  error_message VARCHAR(1000),
  tenant_id VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  org_source VARCHAR(32),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE re_rule_exec_log IS '规则执行日志（含 traceId 与组织上下文）';

-- ============================================================================
-- 标准化中心
-- ============================================================================
CREATE TABLE IF NOT EXISTS tm_standard_concept (
  id BIGINT PRIMARY KEY,
  concept_code VARCHAR(128) NOT NULL,
  concept_name VARCHAR(200) NOT NULL,
  concept_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
);
COMMENT ON TABLE tm_standard_concept IS '标准概念主表';

CREATE TABLE IF NOT EXISTS tm_concept_mapping (
  id BIGINT PRIMARY KEY,
  source_system VARCHAR(64) NOT NULL,
  source_code VARCHAR(128) NOT NULL,
  source_name VARCHAR(200),
  concept_type VARCHAR(64) NOT NULL,
  standard_code VARCHAR(128) NOT NULL,
  mapping_status VARCHAR(32) NOT NULL,
  confidence NUMERIC(5, 2),
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tm_concept_mapping UNIQUE (source_system, source_code, concept_type)
);
COMMENT ON TABLE tm_concept_mapping IS '院内字典到标准概念映射';

-- ============================================================================
-- 适配器
-- ============================================================================
CREATE TABLE IF NOT EXISTS adp_adapter_def (
  id BIGINT PRIMARY KEY,
  adapter_code VARCHAR(64) NOT NULL,
  adapter_name VARCHAR(200) NOT NULL,
  adapter_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  config_json TEXT NOT NULL,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_adapter_def UNIQUE (adapter_code)
);
COMMENT ON TABLE adp_adapter_def IS '第三方适配器定义';

CREATE TABLE IF NOT EXISTS adp_query_def (
  id BIGINT PRIMARY KEY,
  adapter_code VARCHAR(64) NOT NULL,
  query_code VARCHAR(64) NOT NULL,
  query_name VARCHAR(200) NOT NULL,
  query_config TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_query_def UNIQUE (adapter_code, query_code)
);
COMMENT ON TABLE adp_query_def IS '适配器查询定义';

-- ============================================================================
-- 图谱
-- ============================================================================
CREATE TABLE IF NOT EXISTS ge_graph_version (
  id BIGINT PRIMARY KEY,
  graph_version VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  description VARCHAR(1000),
  published_by VARCHAR(64),
  published_time TIMESTAMP,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_ge_graph_version UNIQUE (graph_version)
);
COMMENT ON TABLE ge_graph_version IS '图谱版本';

-- ============================================================================
-- 审计
-- ============================================================================
CREATE TABLE IF NOT EXISTS engine_audit_log (
  id BIGINT PRIMARY KEY,
  trace_id VARCHAR(128),
  engine_type VARCHAR(32) NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64),
  target_code VARCHAR(128),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  operator_id VARCHAR(64),
  tenant_id VARCHAR(64),
  group_code VARCHAR(64),
  hospital_code VARCHAR(64),
  campus_code VARCHAR(64),
  site_code VARCHAR(64),
  department_code VARCHAR(64),
  scope_level VARCHAR(32),
  scope_code VARCHAR(64),
  org_source VARCHAR(32),
  detail_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE engine_audit_log IS '引擎审计日志（与 traceId 串联）';

-- ============================================================================
-- 来源追溯
-- ============================================================================
CREATE TABLE IF NOT EXISTS src_document (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  document_code VARCHAR(128) NOT NULL,
  title VARCHAR(500) NOT NULL,
  source_type VARCHAR(64) NOT NULL,
  source_uri VARCHAR(1000),
  publisher VARCHAR(200),
  effective_date DATE,
  expiry_date DATE,
  review_status VARCHAR(32) NOT NULL,
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  content_hash VARCHAR(128),
  metadata_json TEXT,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_src_document UNIQUE (tenant_id, document_code)
);
COMMENT ON TABLE src_document IS '来源追溯-来源文档主表';

CREATE TABLE IF NOT EXISTS src_citation (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  citation_code VARCHAR(128) NOT NULL,
  document_code VARCHAR(128) NOT NULL,
  section_code VARCHAR(128),
  clause_no VARCHAR(128),
  page_no VARCHAR(64),
  excerpt_text TEXT,
  summary_text VARCHAR(1000),
  evidence_level VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_src_citation UNIQUE (tenant_id, citation_code)
);
COMMENT ON TABLE src_citation IS '来源追溯-文献引用片段表';

CREATE TABLE IF NOT EXISTS src_asset_binding (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  asset_type VARCHAR(64) NOT NULL,
  asset_code VARCHAR(128) NOT NULL,
  asset_version VARCHAR(64),
  citation_code VARCHAR(128) NOT NULL,
  binding_role VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_src_asset_binding UNIQUE (tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)
);
COMMENT ON TABLE src_asset_binding IS '来源追溯-业务资产与引用绑定表';

CREATE TABLE IF NOT EXISTS src_review_record (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  review_code VARCHAR(128) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_code VARCHAR(128) NOT NULL,
  target_version VARCHAR(64),
  missing_count INT NOT NULL DEFAULT 0,
  expired_count INT NOT NULL DEFAULT 0,
  unreviewed_count INT NOT NULL DEFAULT 0,
  review_result VARCHAR(32) NOT NULL,
  review_message VARCHAR(1000),
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  detail_json TEXT,
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_src_review_record UNIQUE (tenant_id, review_code)
);
COMMENT ON TABLE src_review_record IS '来源追溯-发布前来源审核记录表';

CREATE TABLE IF NOT EXISTS src_runtime_evidence (
  id BIGINT PRIMARY KEY,
  trace_id VARCHAR(128) NOT NULL,
  tenant_id VARCHAR(64),
  engine_type VARCHAR(32) NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_code VARCHAR(128) NOT NULL,
  target_version VARCHAR(64),
  citation_code VARCHAR(128),
  evidence_summary VARCHAR(1000),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE src_runtime_evidence IS '来源追溯-运行时证据链表';

-- ============================================================================
-- 索引
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_pe_instance_patient ON pe_patient_instance (patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_org_parent ON org_unit (tenant_id, parent_level_code, parent_org_code);
CREATE INDEX IF NOT EXISTS idx_pe_node_instance ON pe_patient_node_state (instance_id, node_code);
CREATE INDEX IF NOT EXISTS idx_pe_variation_org ON pe_variation_record (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_re_log_trace ON re_rule_exec_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_re_log_patient ON re_rule_exec_log (patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_re_log_org ON re_rule_exec_log (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_audit_trace ON engine_audit_log (trace_id);
CREATE INDEX IF NOT EXISTS idx_audit_org ON engine_audit_log (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_src_doc_review ON src_document (tenant_id, review_status, expiry_date);
CREATE INDEX IF NOT EXISTS idx_src_citation_doc ON src_citation (tenant_id, document_code);
CREATE INDEX IF NOT EXISTS idx_src_binding_asset ON src_asset_binding (tenant_id, asset_type, asset_code, asset_version);
CREATE INDEX IF NOT EXISTS idx_src_review_target ON src_review_record (tenant_id, target_type, target_code, target_version);
CREATE INDEX IF NOT EXISTS idx_src_runtime_trace ON src_runtime_evidence (trace_id, engine_type);
