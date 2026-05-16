-- Dameng core DDL for specialty pathway engines
-- 达梦语法整体接近Oracle，本脚本避免使用复杂数据库特性。

CREATE TABLE org_unit (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  level_code VARCHAR(32) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  org_name VARCHAR(200) NOT NULL,
  parent_level_code VARCHAR(32),
  parent_org_code VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  display_order INT DEFAULT 0 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_org_unit UNIQUE (tenant_id, level_code, org_code)
);

CREATE TABLE pe_pathway_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  pathway_code VARCHAR(64) NOT NULL,
  pathway_name VARCHAR(200) NOT NULL,
  specialty_code VARCHAR(64),
  disease_code VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_pathway_def UNIQUE (tenant_id, org_code, pathway_code)
);

CREATE TABLE pe_pathway_version (
  id BIGINT PRIMARY KEY,
  pathway_code VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  config_json CLOB NOT NULL,
  effective_time TIMESTAMP,
  retired_time TIMESTAMP,
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_pe_pathway_version UNIQUE (pathway_code, version_no)
);

CREATE TABLE pe_patient_instance (
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
  lock_version INT DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)
);

CREATE TABLE pe_patient_node_state (
  id BIGINT PRIMARY KEY,
  instance_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  node_name VARCHAR(200),
  status VARCHAR(32) NOT NULL,
  enter_time TIMESTAMP,
  complete_time TIMESTAMP,
  timeout_flag INT DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE pe_patient_task_state (
  id BIGINT PRIMARY KEY,
  instance_id BIGINT NOT NULL,
  node_code VARCHAR(64) NOT NULL,
  task_code VARCHAR(64) NOT NULL,
  task_name VARCHAR(200),
  task_type VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  result_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

CREATE TABLE pe_variation_record (
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE re_rule_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(200) NOT NULL,
  rule_type VARCHAR(64) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  severity VARCHAR(32),
  rule_json CLOB NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  CONSTRAINT uk_re_rule_def UNIQUE (tenant_id, org_code, rule_code, version_no)
);

CREATE TABLE re_rule_exec_log (
  id BIGINT PRIMARY KEY,
  trace_id VARCHAR(128) NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_version VARCHAR(32),
  patient_id VARCHAR(64),
  encounter_id VARCHAR(64),
  event_id VARCHAR(128),
  hit_flag INT NOT NULL,
  severity VARCHAR(32),
  message VARCHAR(1000),
  input_snapshot CLOB,
  output_snapshot CLOB,
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE tm_standard_concept (
  id BIGINT PRIMARY KEY,
  concept_code VARCHAR(128) NOT NULL,
  concept_name VARCHAR(200) NOT NULL,
  concept_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
);

CREATE TABLE tm_concept_mapping (
  id BIGINT PRIMARY KEY,
  source_system VARCHAR(64) NOT NULL,
  source_code VARCHAR(128) NOT NULL,
  source_name VARCHAR(200),
  concept_type VARCHAR(64) NOT NULL,
  standard_code VARCHAR(128) NOT NULL,
  mapping_status VARCHAR(32) NOT NULL,
  confidence DECIMAL(5,2),
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_concept_mapping UNIQUE (source_system, source_code, concept_type)
);

CREATE TABLE adp_adapter_def (
  id BIGINT PRIMARY KEY,
  adapter_code VARCHAR(64) NOT NULL,
  adapter_name VARCHAR(200) NOT NULL,
  adapter_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  config_json CLOB NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_adapter_def UNIQUE (adapter_code)
);

CREATE TABLE adp_query_def (
  id BIGINT PRIMARY KEY,
  adapter_code VARCHAR(64) NOT NULL,
  query_code VARCHAR(64) NOT NULL,
  query_name VARCHAR(200) NOT NULL,
  query_config CLOB NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_query_def UNIQUE (adapter_code, query_code)
);

CREATE TABLE ge_graph_version (
  id BIGINT PRIMARY KEY,
  graph_version VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  description VARCHAR(1000),
  published_by VARCHAR(64),
  published_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_ge_graph_version UNIQUE (graph_version)
);

CREATE TABLE engine_audit_log (
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
  detail_json CLOB,
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
