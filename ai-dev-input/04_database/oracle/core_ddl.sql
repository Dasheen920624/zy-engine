-- Oracle core DDL for specialty pathway engines
-- Naming convention:
-- pe_* pathway engine
-- re_* rule engine
-- ge_* graph engine metadata
-- tm_* terminology service
-- adp_* adapter hub
-- common large JSON configs are stored in CLOB, query fields are structured.

CREATE TABLE org_unit (
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

CREATE TABLE pe_pathway_def (
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

CREATE TABLE pe_pathway_version (
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

CREATE TABLE pe_patient_instance (
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

CREATE TABLE pe_patient_node_state (
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

CREATE TABLE pe_patient_task_state (
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

CREATE TABLE pe_variation_record (
  id NUMBER(20) PRIMARY KEY,
  instance_id NUMBER(20) NOT NULL,
  patient_id VARCHAR2(64) NOT NULL,
  encounter_id VARCHAR2(64) NOT NULL,
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

CREATE TABLE re_rule_def (
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

CREATE TABLE re_rule_exec_log (
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

CREATE TABLE tm_standard_concept (
  id NUMBER(20) PRIMARY KEY,
  concept_code VARCHAR2(128) NOT NULL,
  concept_name VARCHAR2(200) NOT NULL,
  concept_type VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
);

CREATE TABLE tm_concept_mapping (
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

CREATE TABLE adp_adapter_def (
  id NUMBER(20) PRIMARY KEY,
  adapter_code VARCHAR2(64) NOT NULL,
  adapter_name VARCHAR2(200) NOT NULL,
  adapter_type VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  config_json CLOB NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_adapter_def UNIQUE (adapter_code)
);

CREATE TABLE adp_query_def (
  id NUMBER(20) PRIMARY KEY,
  adapter_code VARCHAR2(64) NOT NULL,
  query_code VARCHAR2(64) NOT NULL,
  query_name VARCHAR2(200) NOT NULL,
  query_config CLOB NOT NULL,
  status VARCHAR2(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_query_def UNIQUE (adapter_code, query_code)
);

CREATE TABLE ge_graph_version (
  id NUMBER(20) PRIMARY KEY,
  graph_version VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  description VARCHAR2(1000),
  published_by VARCHAR2(64),
  published_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_ge_graph_version UNIQUE (graph_version)
);

CREATE TABLE engine_audit_log (
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

CREATE TABLE src_document (
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_document UNIQUE (tenant_id, document_code)
);

CREATE TABLE src_citation (
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

CREATE TABLE src_asset_binding (
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

CREATE TABLE src_review_record (
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

CREATE TABLE src_runtime_evidence (
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

CREATE INDEX idx_pe_instance_patient ON pe_patient_instance(patient_id, encounter_id);
CREATE INDEX idx_org_parent ON org_unit(tenant_id, parent_level_code, parent_org_code);
CREATE INDEX idx_pe_node_instance ON pe_patient_node_state(instance_id, node_code);
CREATE INDEX idx_pe_variation_org ON pe_variation_record(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX idx_re_log_trace ON re_rule_exec_log(trace_id);
CREATE INDEX idx_re_log_patient ON re_rule_exec_log(patient_id, encounter_id);
CREATE INDEX idx_re_log_org ON re_rule_exec_log(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX idx_audit_trace ON engine_audit_log(trace_id);
CREATE INDEX idx_audit_org ON engine_audit_log(tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX idx_src_doc_review ON src_document(tenant_id, review_status, expiry_date);
CREATE INDEX idx_src_citation_doc ON src_citation(tenant_id, document_code);
CREATE INDEX idx_src_binding_asset ON src_asset_binding(tenant_id, asset_type, asset_code, asset_version);
CREATE INDEX idx_src_review_target ON src_review_record(tenant_id, target_type, target_code, target_version);
CREATE INDEX idx_src_runtime_trace ON src_runtime_evidence(trace_id, engine_type);
