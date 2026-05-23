-- H2 local file database DDL for AI/offline development.
-- Oracle/DM DDL remains the production authority. This file mirrors the core
-- tables needed by the current persistence provider.

CREATE TABLE IF NOT EXISTS org_unit (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  level_code VARCHAR2(32) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  org_name VARCHAR2(200) NOT NULL,
  parent_level_code VARCHAR2(32),
  parent_org_code VARCHAR2(64),
  status VARCHAR2(32) NOT NULL,
  display_order NUMBER(10) DEFAULT 0 NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_org_unit UNIQUE (tenant_id, level_code, org_code)
);

CREATE TABLE IF NOT EXISTS pe_pathway_def (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  pathway_code VARCHAR2(64) NOT NULL,
  pathway_name VARCHAR2(200) NOT NULL,
  specialty_code VARCHAR2(64),
  disease_code VARCHAR2(64),
  status VARCHAR2(32) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_pathway_def UNIQUE (tenant_id, org_code, pathway_code)
);

CREATE TABLE IF NOT EXISTS pe_pathway_version (
  id NUMBER(20) PRIMARY KEY,
  pathway_code VARCHAR2(64) NOT NULL,
  version_no VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  config_json CLOB NOT NULL,
  effective_time TIMESTAMP,
  retired_time TIMESTAMP,
  approved_by VARCHAR2(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_pe_pathway_version UNIQUE (pathway_code, version_no)
);

CREATE TABLE IF NOT EXISTS pe_patient_instance (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  patient_id VARCHAR2(64) NOT NULL,
  encounter_id VARCHAR2(64) NOT NULL,
  pathway_code VARCHAR2(64) NOT NULL,
  version_no VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  current_node_code VARCHAR2(64),
  admitted_by VARCHAR2(64),
  admission_time TIMESTAMP,
  exit_time TIMESTAMP,
  exit_reason VARCHAR2(1000),
  lock_version NUMBER(10) DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)
);

CREATE TABLE IF NOT EXISTS pe_patient_node_state (
  id NUMBER(20) PRIMARY KEY,
  instance_id NUMBER(20) NOT NULL,
  node_code VARCHAR2(64) NOT NULL,
  node_name VARCHAR2(200),
  status VARCHAR2(32) NOT NULL,
  enter_time TIMESTAMP,
  complete_time TIMESTAMP,
  timeout_flag NUMBER(1) DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS pe_patient_task_state (
  id NUMBER(20) PRIMARY KEY,
  instance_id NUMBER(20) NOT NULL,
  node_code VARCHAR2(64) NOT NULL,
  task_code VARCHAR2(64) NOT NULL,
  task_name VARCHAR2(200),
  task_type VARCHAR2(64),
  status VARCHAR2(32) NOT NULL,
  result_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pe_variation_record (
  id NUMBER(20) PRIMARY KEY,
  instance_id NUMBER(20) NOT NULL,
  patient_id VARCHAR2(64) NOT NULL,
  encounter_id VARCHAR2(64) not null,
  node_code VARCHAR2(64),
  variation_type VARCHAR2(64) NOT NULL,
  reason VARCHAR2(1000),
  operator_id VARCHAR2(64),
  tenant_id VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  org_source VARCHAR2(32),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS pe_recommendation_record (
  id NUMBER(20) PRIMARY KEY,
  recommendation_id VARCHAR2(128) NOT NULL,
  patient_id VARCHAR2(64) NOT NULL,
  encounter_id VARCHAR2(64) NOT NULL,
  scenario VARCHAR2(64) NOT NULL,
  target_code VARCHAR2(128) NOT NULL,
  target_name VARCHAR2(200),
  score NUMBER(8,2),
  confidence VARCHAR2(32),
  action_level VARCHAR2(32),
  card_json CLOB,
  trace_id VARCHAR2(128),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_pe_recommendation UNIQUE (recommendation_id)
);

CREATE TABLE IF NOT EXISTS re_rule_def (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  rule_code VARCHAR2(64) NOT NULL,
  rule_name VARCHAR2(200) NOT NULL,
  rule_type VARCHAR2(64) NOT NULL,
  version_no VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  severity VARCHAR2(32),
  rule_json CLOB NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  approved_by VARCHAR2(64),
  approved_time TIMESTAMP,
  CONSTRAINT uk_re_rule_def UNIQUE (tenant_id, org_code, rule_code, version_no)
);

CREATE TABLE IF NOT EXISTS re_rule_exec_log (
  id NUMBER(20) PRIMARY KEY,
  trace_id VARCHAR2(128) NOT NULL,
  rule_code VARCHAR2(64) NOT NULL,
  rule_version VARCHAR2(32),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  event_id VARCHAR2(128),
  hit_flag NUMBER(1) NOT NULL,
  severity VARCHAR2(32),
  message VARCHAR2(1000),
  input_snapshot CLOB,
  output_snapshot CLOB,
  elapsed_ms NUMBER(10),
  result_status VARCHAR2(32) NOT NULL,
  error_code VARCHAR2(64),
  error_message VARCHAR2(1000),
  tenant_id VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  org_source VARCHAR2(32),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS tm_standard_concept (
  id NUMBER(20) PRIMARY KEY,
  concept_code VARCHAR2(128) NOT NULL,
  concept_name VARCHAR2(200) NOT NULL,
  concept_type VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
);

CREATE TABLE IF NOT EXISTS tm_concept_mapping (
  id NUMBER(20) PRIMARY KEY,
  source_system VARCHAR2(64) NOT NULL,
  source_code VARCHAR2(128) NOT NULL,
  source_name VARCHAR2(200),
  concept_type VARCHAR2(64) NOT NULL,
  standard_code VARCHAR2(128) NOT NULL,
  mapping_status VARCHAR2(32) NOT NULL,
  confidence NUMBER(5,2),
  approved_by VARCHAR2(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_concept_mapping UNIQUE (source_system, source_code, concept_type)
);

CREATE TABLE IF NOT EXISTS adp_adapter_def (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR2(64) NOT NULL,
  adapter_name VARCHAR2(200) NOT NULL,
  adapter_type VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  config_json CLOB NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_adapter_def UNIQUE (tenant_id, hospital_code, adapter_code)
);

CREATE TABLE IF NOT EXISTS adp_query_def (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR2(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR2(64) NOT NULL,
  query_code VARCHAR2(64) NOT NULL,
  query_name VARCHAR2(200) NOT NULL,
  query_config CLOB NOT NULL,
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_query_def UNIQUE (tenant_id, hospital_code, adapter_code, query_code)
);

CREATE TABLE IF NOT EXISTS ge_graph_version (
  id NUMBER(20) PRIMARY KEY,
  graph_version VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  description VARCHAR2(1000),
  published_by VARCHAR2(64),
  published_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_ge_graph_version UNIQUE (graph_version)
);

CREATE TABLE IF NOT EXISTS engine_audit_log (
  id NUMBER(20) PRIMARY KEY,
  trace_id VARCHAR2(128),
  engine_type VARCHAR2(32) NOT NULL,
  action_type VARCHAR2(64) NOT NULL,
  target_type VARCHAR2(64),
  target_code VARCHAR2(128),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  operator_id VARCHAR2(64),
  tenant_id VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  org_source VARCHAR2(32),
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS src_document (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  document_code VARCHAR2(128) NOT NULL,
  title VARCHAR2(500) NOT NULL,
  source_type VARCHAR2(64) NOT NULL,
  source_uri VARCHAR2(1000),
  publisher VARCHAR2(200),
  effective_date DATE,
  expiry_date DATE,
  review_status VARCHAR2(32) NOT NULL,
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  content_hash VARCHAR2(128),
  metadata_json CLOB,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_document UNIQUE (tenant_id, document_code)
);

CREATE TABLE IF NOT EXISTS src_citation (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  citation_code VARCHAR2(128) NOT NULL,
  document_code VARCHAR2(128) NOT NULL,
  section_code VARCHAR2(128),
  clause_no VARCHAR2(128),
  page_no VARCHAR2(64),
  excerpt_text CLOB,
  summary_text VARCHAR2(1000),
  evidence_level VARCHAR2(64),
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_citation UNIQUE (tenant_id, citation_code)
);

CREATE TABLE IF NOT EXISTS src_asset_binding (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  asset_type VARCHAR2(64) NOT NULL,
  asset_code VARCHAR2(128) NOT NULL,
  asset_version VARCHAR2(64),
  citation_code VARCHAR2(128) NOT NULL,
  binding_role VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_asset_binding UNIQUE (tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)
);

CREATE TABLE IF NOT EXISTS src_review_record (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  review_code VARCHAR2(128) NOT NULL,
  target_type VARCHAR2(64) NOT NULL,
  target_code VARCHAR2(128) NOT NULL,
  target_version VARCHAR2(64),
  missing_count NUMBER(10) DEFAULT 0 NOT NULL,
  expired_count NUMBER(10) DEFAULT 0 NOT NULL,
  unreviewed_count NUMBER(10) DEFAULT 0 NOT NULL,
  review_result VARCHAR2(32) NOT NULL,
  review_message VARCHAR2(1000),
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_review_record UNIQUE (tenant_id, review_code)
);

CREATE TABLE IF NOT EXISTS src_runtime_evidence (
  id NUMBER(20) PRIMARY KEY,
  trace_id VARCHAR2(128) NOT NULL,
  tenant_id VARCHAR2(64),
  engine_type VARCHAR2(32) NOT NULL,
  action_type VARCHAR2(64) NOT NULL,
  target_type VARCHAR2(64) NOT NULL,
  target_code VARCHAR2(128) NOT NULL,
  target_version VARCHAR2(64),
  citation_code VARCHAR2(128),
  evidence_summary VARCHAR2(1000),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pe_instance_patient ON pe_patient_instance(patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_org_parent ON org_unit(tenant_id, parent_level_code, parent_org_code);
CREATE INDEX IF NOT EXISTS idx_pe_node_instance ON pe_patient_node_state(instance_id, node_code);
CREATE INDEX IF NOT EXISTS idx_pe_task_instance ON pe_patient_task_state(instance_id, node_code);
CREATE INDEX IF NOT EXISTS idx_pe_variation_org ON pe_variation_record(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_re_log_trace ON re_rule_exec_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_re_log_patient ON re_rule_exec_log(patient_id, encounter_id);
CREATE INDEX IF NOT EXISTS idx_re_log_org ON re_rule_exec_log(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_audit_trace ON engine_audit_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_audit_org ON engine_audit_log(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_src_doc_review ON src_document(tenant_id, review_status, expiry_date);
CREATE INDEX IF NOT EXISTS idx_src_citation_doc ON src_citation(tenant_id, document_code);
CREATE INDEX IF NOT EXISTS idx_src_binding_asset ON src_asset_binding(tenant_id, asset_type, asset_code, asset_version);
CREATE INDEX IF NOT EXISTS idx_src_review_target ON src_review_record(tenant_id, target_type, target_code, target_version);
CREATE INDEX IF NOT EXISTS idx_src_runtime_trace ON src_runtime_evidence(trace_id, engine_type);

-- ============================================================================
-- 配置包
-- ============================================================================
CREATE TABLE IF NOT EXISTS cfg_config_package (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  package_code VARCHAR2(128) NOT NULL,
  package_version VARCHAR2(64) NOT NULL,
  asset_type VARCHAR2(64) NOT NULL,
  scope_level VARCHAR2(32) NOT NULL,
  scope_code VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  base_version VARCHAR2(64),
  target_version VARCHAR2(64),
  content_hash VARCHAR2(128),
  declared_content_hash VARCHAR2(128),
  manifest_json CLOB,
  diff_json CLOB,
  full_snapshot_json CLOB NOT NULL,
  created_by VARCHAR2(64),
  reviewed_by VARCHAR2(64),
  approved_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  reviewed_time TIMESTAMP,
  published_time TIMESTAMP,
  CONSTRAINT uk_cfg_config_package UNIQUE (tenant_id, package_code, package_version, asset_type, scope_level, scope_code)
);

CREATE INDEX IF NOT EXISTS idx_cfg_pkg_tenant ON cfg_config_package(tenant_id, asset_type, status);
CREATE INDEX IF NOT EXISTS idx_cfg_pkg_code ON cfg_config_package(tenant_id, package_code, package_version);

-- ============================================================================
-- 术语治理队列
-- ============================================================================
CREATE TABLE IF NOT EXISTS tm_unmapped_queue (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  queue_id VARCHAR2(64) NOT NULL,
  source_system VARCHAR2(64) NOT NULL,
  source_code VARCHAR2(128) NOT NULL,
  source_name VARCHAR2(200),
  concept_type VARCHAR2(64) NOT NULL,
  governance_status VARCHAR2(32) NOT NULL,
  proposed_standard_code VARCHAR2(128),
  proposed_standard_name VARCHAR2(200),
  proposed_confidence NUMBER(5,4) DEFAULT 0,
  proposed_mapping_source VARCHAR2(64),
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(1000),
  occurrence_count NUMBER(10) DEFAULT 1 NOT NULL,
  last_occurrence_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_tm_unmapped_queue UNIQUE (tenant_id, source_system, source_code, concept_type, governance_status)
);

CREATE INDEX IF NOT EXISTS idx_tm_queue_status ON tm_unmapped_queue(tenant_id, governance_status, last_occurrence_time);
CREATE INDEX IF NOT EXISTS idx_tm_queue_system ON tm_unmapped_queue(tenant_id, source_system, concept_type);

-- ============================================================================
-- 统一待办和审批工作流
-- ============================================================================
CREATE TABLE IF NOT EXISTS wf_todo_task (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  task_code VARCHAR2(128) NOT NULL,
  business_type VARCHAR2(64) NOT NULL,
  business_code VARCHAR2(128) NOT NULL,
  business_version VARCHAR2(64),
  title VARCHAR2(500) NOT NULL,
  description VARCHAR2(2000),
  priority VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  assigned_type VARCHAR2(32) NOT NULL,
  assigned_to VARCHAR2(128),
  created_by VARCHAR2(64) NOT NULL,
  due_time TIMESTAMP,
  completed_by VARCHAR2(64),
  completed_time TIMESTAMP,
  completed_comment VARCHAR2(1000),
  cancelled_by VARCHAR2(64),
  cancelled_time TIMESTAMP,
  cancel_reason VARCHAR2(1000),
  tenant_code VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  metadata_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_todo_task UNIQUE (tenant_id, task_code)
);

CREATE TABLE IF NOT EXISTS wf_approval_action (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  task_id NUMBER(20) NOT NULL,
  task_code VARCHAR2(128) NOT NULL,
  action_type VARCHAR2(32) NOT NULL,
  action_result VARCHAR2(32) NOT NULL,
  operator_id VARCHAR2(64) NOT NULL,
  operator_name VARCHAR2(100),
  comment VARCHAR2(2000),
  delegate_to VARCHAR2(64),
  delegate_to_name VARCHAR2(100),
  tenant_code VARCHAR2(64),
  group_code VARCHAR2(64),
  hospital_code VARCHAR2(64),
  campus_code VARCHAR2(64),
  site_code VARCHAR2(64),
  department_code VARCHAR2(64),
  scope_level VARCHAR2(32),
  scope_code VARCHAR2(64),
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_wf_action_task FOREIGN KEY (task_id) REFERENCES wf_todo_task(id)
);

CREATE TABLE IF NOT EXISTS wf_approval_rule (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  rule_code VARCHAR2(128) NOT NULL,
  rule_name VARCHAR2(200) NOT NULL,
  business_type VARCHAR2(64) NOT NULL,
  approval_type VARCHAR2(32) NOT NULL,
  approver_type VARCHAR2(32) NOT NULL,
  approver_value VARCHAR2(200) NOT NULL,
  timeout_hours NUMBER(10),
  timeout_action VARCHAR2(32),
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  status VARCHAR2(32) NOT NULL,
  description VARCHAR2(500),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_wf_approval_rule UNIQUE (tenant_id, rule_code)
);

CREATE INDEX IF NOT EXISTS idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX IF NOT EXISTS idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX IF NOT EXISTS idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX IF NOT EXISTS idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX IF NOT EXISTS idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX IF NOT EXISTS idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);

-- ============================================================================
-- Dify 工作流模板
-- ============================================================================
CREATE TABLE IF NOT EXISTS src_dify_template (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  workflow_code VARCHAR2(128) NOT NULL,
  workflow_version VARCHAR2(64) NOT NULL,
  workflow_name VARCHAR2(256),
  description VARCHAR2(1000),
  dify_app_code VARCHAR2(128),
  timeout_ms NUMBER(10),
  retry_count NUMBER(10),
  template_json CLOB,
  reference_document_code VARCHAR2(128),
  reference_binding_type VARCHAR2(64),
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_dify_template UNIQUE (workflow_code, workflow_version)
);

CREATE INDEX IF NOT EXISTS idx_dify_tpl_code ON src_dify_template(tenant_id, workflow_code);
