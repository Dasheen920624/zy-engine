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
COMMENT ON TABLE org_unit IS '组织目录：集团/医院/院区/站点/科室五段式组织树';
COMMENT ON COLUMN org_unit.tenant_id IS '租户编码（顶层隔离）';
COMMENT ON COLUMN org_unit.level_code IS '组织层级：GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT';
COMMENT ON COLUMN org_unit.org_code IS '组织编码（在 level 范围内唯一）';
COMMENT ON COLUMN org_unit.parent_level_code IS '父级层级';
COMMENT ON COLUMN org_unit.parent_org_code IS '父级编码';

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
COMMENT ON TABLE pe_pathway_def IS '路径定义主表';
COMMENT ON COLUMN pe_pathway_def.pathway_code IS '路径编码（如 AMI_STEMI）';
COMMENT ON COLUMN pe_pathway_def.specialty_code IS '专科编码';
COMMENT ON COLUMN pe_pathway_def.disease_code IS '主诊断编码';

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
COMMENT ON TABLE pe_pathway_version IS '路径版本与配置 JSON';
COMMENT ON COLUMN pe_pathway_version.config_json IS '路径完整配置 JSON（节点 / 任务 / 流转）';

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
COMMENT ON TABLE pe_patient_instance IS '患者路径运行实例';

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
COMMENT ON TABLE pe_patient_node_state IS '患者节点运行状态';

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
COMMENT ON TABLE pe_patient_task_state IS '患者任务运行状态';

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
COMMENT ON TABLE pe_variation_record IS '路径变异记录（带组织上下文）';
COMMENT ON COLUMN pe_variation_record.org_source IS '组织来源：HEADER/QUERY/BODY/DEFAULT/NONE';

CREATE TABLE pe_recommendation_record (
  id BIGINT PRIMARY KEY,
  recommendation_id VARCHAR(128) NOT NULL,
  patient_id VARCHAR(64) NOT NULL,
  encounter_id VARCHAR(64) NOT NULL,
  scenario VARCHAR(64) NOT NULL,
  target_code VARCHAR(128) NOT NULL,
  target_name VARCHAR(200),
  score DECIMAL(8,2),
  confidence VARCHAR(32),
  action_level VARCHAR(32),
  card_json CLOB,
  trace_id VARCHAR(128),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_pe_recommendation UNIQUE (recommendation_id)
);
COMMENT ON TABLE pe_recommendation_record IS '推荐记录（AI 推荐结果持久化）';
COMMENT ON COLUMN pe_recommendation_record.recommendation_id IS '推荐ID（全局唯一）';
COMMENT ON COLUMN pe_recommendation_record.scenario IS '推荐场景';
COMMENT ON COLUMN pe_recommendation_record.card_json IS '推荐卡片完整 JSON';
COMMENT ON COLUMN pe_recommendation_record.trace_id IS '关联的追踪ID';

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
COMMENT ON TABLE re_rule_def IS '规则定义（按组织 + 版本独立）';
COMMENT ON COLUMN re_rule_def.rule_json IS '规则 DSL 与元数据 JSON（含 reference_* 来源字段）';

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
COMMENT ON TABLE re_rule_exec_log IS '规则执行日志（含 traceId 与组织上下文）';

CREATE TABLE tm_standard_concept (
  id BIGINT PRIMARY KEY,
  concept_code VARCHAR(128) NOT NULL,
  concept_name VARCHAR(200) NOT NULL,
  concept_type VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
);
COMMENT ON TABLE tm_standard_concept IS '标准概念主表';

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
COMMENT ON TABLE tm_concept_mapping IS '院内字典到标准概念映射';

CREATE TABLE adp_adapter_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR(64) NOT NULL,
  adapter_name VARCHAR(200) NOT NULL,
  adapter_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  config_json CLOB NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_adapter_def UNIQUE (tenant_id, hospital_code, adapter_code)
);
COMMENT ON TABLE adp_adapter_def IS '第三方适配器定义';

CREATE TABLE adp_query_def (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL DEFAULT 'default',
  hospital_code VARCHAR(64) NOT NULL DEFAULT 'DEFAULT_HOSPITAL',
  adapter_code VARCHAR(64) NOT NULL,
  query_code VARCHAR(64) NOT NULL,
  query_name VARCHAR(200) NOT NULL,
  query_config CLOB NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_adp_query_def UNIQUE (tenant_id, hospital_code, adapter_code, query_code)
);
COMMENT ON TABLE adp_query_def IS '适配器查询定义';

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
COMMENT ON TABLE ge_graph_version IS '图谱版本';

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
COMMENT ON TABLE engine_audit_log IS '引擎审计日志（与 traceId 串联）';

CREATE TABLE src_document (
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
  metadata_json CLOB,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_document UNIQUE (tenant_id, document_code)
);
COMMENT ON TABLE src_document IS '来源追溯-来源文档主表';

CREATE TABLE src_citation (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  citation_code VARCHAR(128) NOT NULL,
  document_code VARCHAR(128) NOT NULL,
  section_code VARCHAR(128),
  clause_no VARCHAR(128),
  page_no VARCHAR(64),
  excerpt_text CLOB,
  summary_text VARCHAR(1000),
  evidence_level VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_citation UNIQUE (tenant_id, citation_code)
);
COMMENT ON TABLE src_citation IS '来源追溯-文献引用片段表';

CREATE TABLE src_asset_binding (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  asset_type VARCHAR(64) NOT NULL,
  asset_code VARCHAR(128) NOT NULL,
  asset_version VARCHAR(64),
  citation_code VARCHAR(128) NOT NULL,
  binding_role VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_asset_binding UNIQUE (tenant_id, asset_type, asset_code, asset_version, citation_code, binding_role)
);
COMMENT ON TABLE src_asset_binding IS '来源追溯-业务资产与引用绑定表';

CREATE TABLE src_review_record (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  review_code VARCHAR(128) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_code VARCHAR(128) NOT NULL,
  target_version VARCHAR(64),
  missing_count INT DEFAULT 0 NOT NULL,
  expired_count INT DEFAULT 0 NOT NULL,
  unreviewed_count INT DEFAULT 0 NOT NULL,
  review_result VARCHAR(32) NOT NULL,
  review_message VARCHAR(1000),
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  detail_json CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_review_record UNIQUE (tenant_id, review_code)
);
COMMENT ON TABLE src_review_record IS '来源追溯-发布前来源审核记录表';

CREATE TABLE src_runtime_evidence (
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
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
COMMENT ON TABLE src_runtime_evidence IS '来源追溯-运行时证据链表';

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

-- ============================================================================
-- 配置包
-- ============================================================================
CREATE TABLE cfg_config_package (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  package_code VARCHAR(128) NOT NULL,
  package_version VARCHAR(64) NOT NULL,
  asset_type VARCHAR(64) NOT NULL,
  scope_level VARCHAR(32) NOT NULL,
  scope_code VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  base_version VARCHAR(64),
  target_version VARCHAR(64),
  content_hash VARCHAR(128),
  declared_content_hash VARCHAR(128),
  manifest_json CLOB,
  diff_json CLOB,
  full_snapshot_json CLOB NOT NULL,
  created_by VARCHAR(64),
  reviewed_by VARCHAR(64),
  approved_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  reviewed_time TIMESTAMP,
  published_time TIMESTAMP,
  CONSTRAINT uk_cfg_config_package UNIQUE (tenant_id, package_code, package_version, asset_type, scope_level, scope_code)
);
COMMENT ON TABLE cfg_config_package IS '配置包管理-配置包主表，保存配置包元数据、版本、状态和完整快照';
COMMENT ON COLUMN cfg_config_package.id IS '主键ID，由应用层生成';
COMMENT ON COLUMN cfg_config_package.tenant_id IS '租户ID';
COMMENT ON COLUMN cfg_config_package.package_code IS '配置包编码';
COMMENT ON COLUMN cfg_config_package.package_version IS '配置包版本号';
COMMENT ON COLUMN cfg_config_package.asset_type IS '资产类型：RULE/PATHWAY/GRAPH/TERMINOLOGY/ADAPTER';
COMMENT ON COLUMN cfg_config_package.scope_level IS '组织作用域层级：GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT';
COMMENT ON COLUMN cfg_config_package.scope_code IS '组织作用域编码';
COMMENT ON COLUMN cfg_config_package.status IS '配置包状态：DRAFT/REVIEWING/PUBLISHED/REJECTED/ARCHIVED';
COMMENT ON COLUMN cfg_config_package.base_version IS '基准版本号（用于增量包）';
COMMENT ON COLUMN cfg_config_package.target_version IS '目标版本号（用于增量包）';
COMMENT ON COLUMN cfg_config_package.content_hash IS '内容SHA-256哈希值';
COMMENT ON COLUMN cfg_config_package.declared_content_hash IS '声明的内容哈希值（导入时校验）';
COMMENT ON COLUMN cfg_config_package.manifest_json IS '清单JSON，包含资产清单和依赖声明';
COMMENT ON COLUMN cfg_config_package.diff_json IS '差异JSON，增量包与基准版本的差异';
COMMENT ON COLUMN cfg_config_package.full_snapshot_json IS '完整快照JSON，配置包的完整内容';
COMMENT ON COLUMN cfg_config_package.created_by IS '创建人';
COMMENT ON COLUMN cfg_config_package.reviewed_by IS '审核人';
COMMENT ON COLUMN cfg_config_package.approved_by IS '审批人';
COMMENT ON COLUMN cfg_config_package.created_time IS '创建时间';
COMMENT ON COLUMN cfg_config_package.reviewed_time IS '审核时间';
COMMENT ON COLUMN cfg_config_package.published_time IS '发布时间';

CREATE INDEX idx_cfg_pkg_tenant ON cfg_config_package(tenant_id, asset_type, status);
CREATE INDEX idx_cfg_pkg_code ON cfg_config_package(tenant_id, package_code, package_version);

-- ============================================================================
-- 术语治理队列
-- ============================================================================
CREATE TABLE tm_unmapped_queue (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  queue_id VARCHAR(64) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  source_code VARCHAR(128) NOT NULL,
  source_name VARCHAR(200),
  concept_type VARCHAR(64) NOT NULL,
  governance_status VARCHAR(32) NOT NULL,
  proposed_standard_code VARCHAR(128),
  proposed_standard_name VARCHAR(200),
  proposed_confidence DECIMAL(5,4) DEFAULT 0,
  proposed_mapping_source VARCHAR(64),
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR(1000),
  occurrence_count INT DEFAULT 1 NOT NULL,
  last_occurrence_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_tm_unmapped_queue UNIQUE (tenant_id, source_system, source_code, concept_type, governance_status)
);
COMMENT ON TABLE tm_unmapped_queue IS '术语未映射治理队列';
COMMENT ON COLUMN tm_unmapped_queue.id IS '主键ID';
COMMENT ON COLUMN tm_unmapped_queue.tenant_id IS '租户ID';
COMMENT ON COLUMN tm_unmapped_queue.queue_id IS '队列记录ID';
COMMENT ON COLUMN tm_unmapped_queue.source_system IS '来源系统编码';
COMMENT ON COLUMN tm_unmapped_queue.source_code IS '来源术语编码';
COMMENT ON COLUMN tm_unmapped_queue.source_name IS '来源术语名称';
COMMENT ON COLUMN tm_unmapped_queue.concept_type IS '概念类型';
COMMENT ON COLUMN tm_unmapped_queue.governance_status IS '治理状态';
COMMENT ON COLUMN tm_unmapped_queue.proposed_standard_code IS '建议标准码';
COMMENT ON COLUMN tm_unmapped_queue.proposed_standard_name IS '建议标准名称';
COMMENT ON COLUMN tm_unmapped_queue.proposed_confidence IS '建议置信度';
COMMENT ON COLUMN tm_unmapped_queue.proposed_mapping_source IS '建议映射来源';
COMMENT ON COLUMN tm_unmapped_queue.reviewed_by IS '审核人';
COMMENT ON COLUMN tm_unmapped_queue.reviewed_time IS '审核时间';
COMMENT ON COLUMN tm_unmapped_queue.review_comment IS '审核备注';
COMMENT ON COLUMN tm_unmapped_queue.occurrence_count IS '出现次数';
COMMENT ON COLUMN tm_unmapped_queue.last_occurrence_time IS '最近出现时间';
COMMENT ON COLUMN tm_unmapped_queue.created_time IS '创建时间';
COMMENT ON COLUMN tm_unmapped_queue.updated_time IS '更新时间';

CREATE INDEX idx_tm_queue_status ON tm_unmapped_queue(tenant_id, governance_status, last_occurrence_time);
CREATE INDEX idx_tm_queue_system ON tm_unmapped_queue(tenant_id, source_system, concept_type);

-- ============================================================================
-- Dify 工作流模板
-- ============================================================================
CREATE TABLE src_dify_template (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  workflow_code VARCHAR(128) NOT NULL,
  workflow_version VARCHAR(64) NOT NULL,
  workflow_name VARCHAR(256),
  description VARCHAR(1000),
  dify_app_code VARCHAR(128),
  timeout_ms INT,
  retry_count INT,
  template_json CLOB,
  reference_document_code VARCHAR(128),
  reference_binding_type VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_src_dify_template UNIQUE (workflow_code, workflow_version)
);
COMMENT ON TABLE src_dify_template IS 'Dify 工作流模板表';
COMMENT ON COLUMN src_dify_template.workflow_code IS '工作流编码';
COMMENT ON COLUMN src_dify_template.workflow_version IS '工作流版本';
COMMENT ON COLUMN src_dify_template.template_json IS '模板配置JSON（含input_defaults/input_mappings/required_inputs/degraded_outputs）';

CREATE INDEX idx_dify_tpl_code ON src_dify_template(tenant_id, workflow_code);
