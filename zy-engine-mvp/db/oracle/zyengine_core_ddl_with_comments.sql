SET DEFINE OFF;
SET SERVEROUTPUT ON;
ALTER SESSION SET NLS_LENGTH_SEMANTICS=CHAR;

DECLARE
  PROCEDURE create_ignore(p_sql VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE p_sql;
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -955 THEN
        NULL;
      ELSE
        RAISE;
      END IF;
  END;

  PROCEDURE add_column_if_missing(p_table VARCHAR2, p_column VARCHAR2, p_definition VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = UPPER(p_table)
       AND column_name = UPPER(p_column);

    IF v_count = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' ADD (' || p_column || ' ' || p_definition || ')';
    END IF;
  END;

  FUNCTION constraint_signature(p_constraint VARCHAR2) RETURN VARCHAR2 IS
    v_signature VARCHAR2(4000);
  BEGIN
    SELECT LISTAGG(column_name, ',') WITHIN GROUP (ORDER BY position)
      INTO v_signature
      FROM user_cons_columns
     WHERE constraint_name = UPPER(p_constraint);
    RETURN v_signature;
  END;

  PROCEDURE drop_constraint_if_exists(p_table VARCHAR2, p_constraint VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = UPPER(p_table)
       AND constraint_name = UPPER(p_constraint);

    IF v_count > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' DROP CONSTRAINT ' || p_constraint;
    END IF;
  END;
BEGIN
  create_ignore('CREATE TABLE org_unit (
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
  )');

  create_ignore('CREATE TABLE pe_pathway_def (
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
  )');

  create_ignore('CREATE TABLE pe_pathway_version (
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
  )');

  create_ignore('CREATE TABLE pe_patient_instance (
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
  )');

  create_ignore('CREATE TABLE pe_patient_node_state (
    id NUMBER(20) PRIMARY KEY,
    instance_id NUMBER(20) NOT NULL,
    node_code VARCHAR2(64) NOT NULL,
    node_name VARCHAR2(200),
    status VARCHAR2(32) NOT NULL,
    enter_time TIMESTAMP,
    complete_time TIMESTAMP,
    timeout_flag NUMBER(1) DEFAULT 0 NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
  )');

  create_ignore('CREATE TABLE pe_patient_task_state (
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
  )');

  create_ignore('CREATE TABLE pe_variation_record (
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
  )');

  create_ignore('CREATE TABLE pe_recommendation_record (
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
  )');

  create_ignore('CREATE TABLE re_rule_def (
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
  )');

  create_ignore('CREATE TABLE re_rule_exec_log (
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
  )');

  create_ignore('CREATE TABLE tm_standard_concept (
    id NUMBER(20) PRIMARY KEY,
    concept_code VARCHAR2(128) NOT NULL,
    concept_name VARCHAR2(200) NOT NULL,
    concept_type VARCHAR2(64) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_tm_standard_concept UNIQUE (concept_code, concept_type)
  )');

  create_ignore('CREATE TABLE tm_concept_mapping (
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
  )');

  create_ignore('CREATE TABLE adp_adapter_def (
    id NUMBER(20) PRIMARY KEY,
    adapter_code VARCHAR2(64) NOT NULL,
    adapter_name VARCHAR2(200) NOT NULL,
    adapter_type VARCHAR2(32) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    config_json CLOB NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_adp_adapter_def UNIQUE (adapter_code)
  )');

  create_ignore('CREATE TABLE adp_query_def (
    id NUMBER(20) PRIMARY KEY,
    adapter_code VARCHAR2(64) NOT NULL,
    query_code VARCHAR2(64) NOT NULL,
    query_name VARCHAR2(200) NOT NULL,
    query_config CLOB NOT NULL,
    status VARCHAR2(32) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_adp_query_def UNIQUE (adapter_code, query_code)
  )');

  create_ignore('CREATE TABLE ge_graph_version (
    id NUMBER(20) PRIMARY KEY,
    graph_version VARCHAR2(64) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    description VARCHAR2(1000),
    published_by VARCHAR2(64),
    published_time TIMESTAMP,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_ge_graph_version UNIQUE (graph_version)
  )');

  create_ignore('CREATE TABLE engine_audit_log (
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
  )');

  create_ignore('CREATE TABLE src_document (
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
  )');

  create_ignore('CREATE TABLE src_citation (
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
  )');

  create_ignore('CREATE TABLE src_asset_binding (
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
  )');

  create_ignore('CREATE TABLE src_review_record (
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
  )');

  create_ignore('CREATE TABLE src_runtime_evidence (
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
  )');

  create_ignore('CREATE TABLE cfg_config_package (
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
  )');

  add_column_if_missing('pe_variation_record', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('pe_variation_record', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'org_source', 'VARCHAR2(32)');

  add_column_if_missing('re_rule_exec_log', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('re_rule_exec_log', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'org_source', 'VARCHAR2(32)');

  add_column_if_missing('engine_audit_log', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('engine_audit_log', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'org_source', 'VARCHAR2(32)');

  add_column_if_missing('src_document', 'created_by', 'VARCHAR2(64)');
  add_column_if_missing('src_document', 'updated_time', 'TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL');

  IF NVL(constraint_signature('uk_pe_active_instance'), 'NONE')
        <> 'TENANT_ID,ORG_CODE,ENCOUNTER_ID,PATHWAY_CODE,STATUS' THEN
    drop_constraint_if_exists('pe_patient_instance', 'uk_pe_active_instance');
    EXECUTE IMMEDIATE 'ALTER TABLE pe_patient_instance ADD CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)';
  END IF;

  create_ignore('CREATE INDEX idx_pe_instance_patient ON pe_patient_instance(patient_id, encounter_id)');
  create_ignore('CREATE INDEX idx_org_parent ON org_unit(tenant_id, parent_level_code, parent_org_code)');
  create_ignore('CREATE INDEX idx_pe_node_instance ON pe_patient_node_state(instance_id, node_code)');
  create_ignore('CREATE INDEX idx_pe_task_instance ON pe_patient_task_state(instance_id, node_code)');
  create_ignore('CREATE INDEX idx_pe_variation_org ON pe_variation_record(tenant_id, hospital_code, scope_level, scope_code)');
  create_ignore('CREATE INDEX idx_re_log_trace ON re_rule_exec_log(trace_id)');
  create_ignore('CREATE INDEX idx_re_log_patient ON re_rule_exec_log(patient_id, encounter_id)');
  create_ignore('CREATE INDEX idx_re_log_org ON re_rule_exec_log(tenant_id, hospital_code, scope_level, scope_code)');
  create_ignore('CREATE INDEX idx_audit_trace ON engine_audit_log(trace_id)');
  create_ignore('CREATE INDEX idx_audit_org ON engine_audit_log(tenant_id, hospital_code, scope_level, scope_code)');
  create_ignore('CREATE INDEX idx_src_doc_review ON src_document(tenant_id, review_status, expiry_date)');
  create_ignore('CREATE INDEX idx_src_citation_doc ON src_citation(tenant_id, document_code)');
  create_ignore('CREATE INDEX idx_src_binding_asset ON src_asset_binding(tenant_id, asset_type, asset_code, asset_version)');
  create_ignore('CREATE INDEX idx_src_review_target ON src_review_record(tenant_id, target_type, target_code, target_version)');
  create_ignore('CREATE INDEX idx_src_runtime_trace ON src_runtime_evidence(trace_id, engine_type)');
  create_ignore('CREATE INDEX idx_cfg_pkg_tenant ON cfg_config_package(tenant_id, package_code, status)');
  create_ignore('CREATE INDEX idx_cfg_pkg_asset ON cfg_config_package(tenant_id, asset_type, scope_level, scope_code)');
END;
/

COMMENT ON TABLE org_unit IS '组织模型-集团、医院、院区、卫生所/站点、科室目录表，不包含系统内置默认基线';
COMMENT ON COLUMN org_unit.tenant_id IS '租户ID';
COMMENT ON COLUMN org_unit.level_code IS '组织层级：GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT';
COMMENT ON COLUMN org_unit.org_code IS '组织编码';
COMMENT ON COLUMN org_unit.org_name IS '组织名称';
COMMENT ON COLUMN org_unit.parent_level_code IS '上级组织层级';
COMMENT ON COLUMN org_unit.parent_org_code IS '上级组织编码';
COMMENT ON COLUMN org_unit.status IS '组织状态：ACTIVE/DISABLED';
COMMENT ON COLUMN org_unit.display_order IS '同级显示顺序';

COMMENT ON TABLE pe_pathway_def IS '路径引擎-专病路径主定义表，保存路径编码、名称、专科和病种等基础信息';
COMMENT ON COLUMN pe_pathway_def.id IS '主键ID，由应用层生成';
COMMENT ON COLUMN pe_pathway_def.tenant_id IS '租户ID，单院部署可固定为医院编码';
COMMENT ON COLUMN pe_pathway_def.org_code IS '医疗机构编码';
COMMENT ON COLUMN pe_pathway_def.pathway_code IS '路径编码，如AMI_STEMI';
COMMENT ON COLUMN pe_pathway_def.pathway_name IS '路径名称';
COMMENT ON COLUMN pe_pathway_def.specialty_code IS '专科编码';
COMMENT ON COLUMN pe_pathway_def.disease_code IS '关联疾病编码';
COMMENT ON COLUMN pe_pathway_def.status IS '路径状态：DRAFT/PUBLISHED/DISABLED';

COMMENT ON TABLE pe_pathway_version IS '路径引擎-路径版本表，保存每个路径版本的完整配置JSON和发布审核信息';
COMMENT ON COLUMN pe_pathway_version.pathway_code IS '路径编码';
COMMENT ON COLUMN pe_pathway_version.version_no IS '版本号';
COMMENT ON COLUMN pe_pathway_version.status IS '版本状态：DRAFT/PUBLISHED/RETIRED';
COMMENT ON COLUMN pe_pathway_version.config_json IS '路径配置DSL原文，包含阶段、节点、任务、流转、绑定规则';
COMMENT ON COLUMN pe_pathway_version.effective_time IS '版本生效时间';
COMMENT ON COLUMN pe_pathway_version.retired_time IS '版本停用时间';
COMMENT ON COLUMN pe_pathway_version.approved_by IS '审核发布人';
COMMENT ON COLUMN pe_pathway_version.approved_time IS '审核发布时间';

COMMENT ON TABLE pe_patient_instance IS '路径引擎-患者路径实例表，保存患者入径后的路径状态和当前节点';
COMMENT ON COLUMN pe_patient_instance.tenant_id IS '租户ID';
COMMENT ON COLUMN pe_patient_instance.org_code IS '组织编码，兼容历史单医院口径';
COMMENT ON COLUMN pe_patient_instance.patient_id IS '患者ID';
COMMENT ON COLUMN pe_patient_instance.encounter_id IS '就诊ID';
COMMENT ON COLUMN pe_patient_instance.pathway_code IS '路径编码';
COMMENT ON COLUMN pe_patient_instance.version_no IS '患者实例绑定的路径版本';
COMMENT ON COLUMN pe_patient_instance.status IS '实例状态：ACTIVE/EXITED/CLOSED等';
COMMENT ON COLUMN pe_patient_instance.current_node_code IS '当前路径节点编码';
COMMENT ON COLUMN pe_patient_instance.admitted_by IS '确认入径医生或操作员';
COMMENT ON COLUMN pe_patient_instance.admission_time IS '入径时间';
COMMENT ON COLUMN pe_patient_instance.exit_time IS '退出路径时间';
COMMENT ON COLUMN pe_patient_instance.exit_reason IS '退出路径原因';
COMMENT ON COLUMN pe_patient_instance.lock_version IS '乐观锁版本号';

COMMENT ON TABLE pe_patient_node_state IS '路径引擎-患者路径节点状态表，记录每个节点进入、完成、超时等状态';
COMMENT ON COLUMN pe_patient_node_state.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_patient_node_state.node_code IS '节点编码';
COMMENT ON COLUMN pe_patient_node_state.node_name IS '节点名称';
COMMENT ON COLUMN pe_patient_node_state.status IS '节点状态：WAITING/RUNNING/COMPLETED/SKIPPED/TIMEOUT';
COMMENT ON COLUMN pe_patient_node_state.enter_time IS '进入节点时间';
COMMENT ON COLUMN pe_patient_node_state.complete_time IS '完成节点时间';
COMMENT ON COLUMN pe_patient_node_state.timeout_flag IS '是否超时，0否1是';

COMMENT ON TABLE pe_patient_task_state IS '路径引擎-患者节点任务状态表，记录检查、检验、表单、医嘱包、随访等任务完成情况';
COMMENT ON COLUMN pe_patient_task_state.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_patient_task_state.node_code IS '所属节点编码';
COMMENT ON COLUMN pe_patient_task_state.task_code IS '任务编码';
COMMENT ON COLUMN pe_patient_task_state.task_name IS '任务名称';
COMMENT ON COLUMN pe_patient_task_state.task_type IS '任务类型：FORM/LAB/EXAM/ORDER_SET/FOLLOW_UP等';
COMMENT ON COLUMN pe_patient_task_state.status IS '任务状态';
COMMENT ON COLUMN pe_patient_task_state.result_json IS '任务结果JSON快照';

COMMENT ON TABLE pe_variation_record IS '路径引擎-路径变异记录表，记录医生偏离路径、患者原因、资源限制、禁忌证等变异';
COMMENT ON COLUMN pe_variation_record.instance_id IS '患者路径实例ID';
COMMENT ON COLUMN pe_variation_record.variation_type IS '变异类型';
COMMENT ON COLUMN pe_variation_record.reason IS '变异原因说明';
COMMENT ON COLUMN pe_variation_record.operator_id IS '记录人';
COMMENT ON COLUMN pe_variation_record.tenant_id IS '租户ID';
COMMENT ON COLUMN pe_variation_record.group_code IS '集团编码';
COMMENT ON COLUMN pe_variation_record.hospital_code IS '医院编码';
COMMENT ON COLUMN pe_variation_record.campus_code IS '院区编码';
COMMENT ON COLUMN pe_variation_record.site_code IS '站点/卫生所编码';
COMMENT ON COLUMN pe_variation_record.department_code IS '科室编码';
COMMENT ON COLUMN pe_variation_record.scope_level IS '组织作用域层级';
COMMENT ON COLUMN pe_variation_record.scope_code IS '组织作用域编码';
COMMENT ON COLUMN pe_variation_record.org_source IS '组织上下文来源';

COMMENT ON TABLE pe_recommendation_record IS '路径引擎-推荐卡片记录表，保存候选路径、风险预警、治疗建议等推荐结果';
COMMENT ON COLUMN pe_recommendation_record.recommendation_id IS '推荐ID';
COMMENT ON COLUMN pe_recommendation_record.scenario IS '推荐场景：PATHWAY_ENTRY/RISK_ALERT等';
COMMENT ON COLUMN pe_recommendation_record.target_code IS '推荐目标编码';
COMMENT ON COLUMN pe_recommendation_record.score IS '综合评分';
COMMENT ON COLUMN pe_recommendation_record.confidence IS '置信度';
COMMENT ON COLUMN pe_recommendation_record.action_level IS '动作级别：BLOCK/STRONG_ALERT/WEAK_ALERT/INFO';
COMMENT ON COLUMN pe_recommendation_record.card_json IS '推荐卡片完整JSON';
COMMENT ON COLUMN pe_recommendation_record.trace_id IS '调用链追踪ID';

COMMENT ON TABLE re_rule_def IS '规则引擎-规则定义表，保存规则DSL、类型、版本、状态和审核信息';
COMMENT ON COLUMN re_rule_def.rule_code IS '规则编码';
COMMENT ON COLUMN re_rule_def.rule_name IS '规则名称';
COMMENT ON COLUMN re_rule_def.rule_type IS '规则类型：TIME_LIMIT_QC/CONTENT_QC/PATHWAY_NODE/SAFETY等';
COMMENT ON COLUMN re_rule_def.version_no IS '规则版本号';
COMMENT ON COLUMN re_rule_def.status IS '规则状态：DRAFT/PUBLISHED/DISABLED';
COMMENT ON COLUMN re_rule_def.severity IS '默认严重级别';
COMMENT ON COLUMN re_rule_def.rule_json IS '规则DSL原文';

COMMENT ON TABLE re_rule_exec_log IS '规则引擎-规则执行日志表，记录每次规则执行的输入、输出、命中、耗时和异常';
COMMENT ON COLUMN re_rule_exec_log.trace_id IS '调用链追踪ID';
COMMENT ON COLUMN re_rule_exec_log.rule_code IS '规则编码';
COMMENT ON COLUMN re_rule_exec_log.hit_flag IS '是否命中，0否1是';
COMMENT ON COLUMN re_rule_exec_log.message IS '规则结果说明';
COMMENT ON COLUMN re_rule_exec_log.input_snapshot IS '规则输入快照';
COMMENT ON COLUMN re_rule_exec_log.output_snapshot IS '规则输出快照';
COMMENT ON COLUMN re_rule_exec_log.elapsed_ms IS '执行耗时毫秒';
COMMENT ON COLUMN re_rule_exec_log.result_status IS '执行状态：SUCCESS/FAILED/TIMEOUT等';
COMMENT ON COLUMN re_rule_exec_log.tenant_id IS '租户ID';
COMMENT ON COLUMN re_rule_exec_log.group_code IS '集团编码';
COMMENT ON COLUMN re_rule_exec_log.hospital_code IS '医院编码';
COMMENT ON COLUMN re_rule_exec_log.campus_code IS '院区编码';
COMMENT ON COLUMN re_rule_exec_log.site_code IS '站点/卫生所编码';
COMMENT ON COLUMN re_rule_exec_log.department_code IS '科室编码';
COMMENT ON COLUMN re_rule_exec_log.scope_level IS '组织作用域层级';
COMMENT ON COLUMN re_rule_exec_log.scope_code IS '组织作用域编码';
COMMENT ON COLUMN re_rule_exec_log.org_source IS '组织上下文来源';

COMMENT ON TABLE tm_standard_concept IS '字典映射-平台标准概念表，保存疾病、检验、检查、药品、文书等统一编码';
COMMENT ON COLUMN tm_standard_concept.concept_code IS '标准概念编码';
COMMENT ON COLUMN tm_standard_concept.concept_name IS '标准概念名称';
COMMENT ON COLUMN tm_standard_concept.concept_type IS '概念类型';
COMMENT ON COLUMN tm_standard_concept.status IS '状态';

COMMENT ON TABLE tm_concept_mapping IS '字典映射-第三方系统编码到平台标准概念的映射表';
COMMENT ON COLUMN tm_concept_mapping.source_system IS '来源系统，如HIS/EMR/LIS/PACS';
COMMENT ON COLUMN tm_concept_mapping.source_code IS '来源系统原始编码';
COMMENT ON COLUMN tm_concept_mapping.source_name IS '来源系统原始名称';
COMMENT ON COLUMN tm_concept_mapping.concept_type IS '概念类型';
COMMENT ON COLUMN tm_concept_mapping.standard_code IS '映射后的平台标准编码';
COMMENT ON COLUMN tm_concept_mapping.mapping_status IS '映射状态：DRAFT/APPROVED/REJECTED';
COMMENT ON COLUMN tm_concept_mapping.confidence IS '映射置信度';

COMMENT ON TABLE adp_adapter_def IS '适配器中心-第三方系统适配器定义表，保存REST/SQL/WebService等连接配置';
COMMENT ON COLUMN adp_adapter_def.adapter_code IS '适配器编码';
COMMENT ON COLUMN adp_adapter_def.adapter_name IS '适配器名称';
COMMENT ON COLUMN adp_adapter_def.adapter_type IS '适配器类型：REST/SQL/WEBSERVICE/MQ/FILE';
COMMENT ON COLUMN adp_adapter_def.status IS '状态';
COMMENT ON COLUMN adp_adapter_def.config_json IS '连接配置JSON，敏感信息生产环境应加密或外部注入';

COMMENT ON TABLE adp_query_def IS '适配器中心-适配器查询定义表，保存具体查询模板和字段映射';
COMMENT ON COLUMN adp_query_def.adapter_code IS '适配器编码';
COMMENT ON COLUMN adp_query_def.query_code IS '查询编码';
COMMENT ON COLUMN adp_query_def.query_name IS '查询名称';
COMMENT ON COLUMN adp_query_def.query_config IS '查询配置JSON';
COMMENT ON COLUMN adp_query_def.status IS '状态';

COMMENT ON TABLE ge_graph_version IS '图谱引擎-图谱版本元数据表，记录医学知识图谱版本发布状态';
COMMENT ON COLUMN ge_graph_version.graph_version IS '图谱版本号';
COMMENT ON COLUMN ge_graph_version.status IS '版本状态';
COMMENT ON COLUMN ge_graph_version.description IS '版本说明';
COMMENT ON COLUMN ge_graph_version.published_by IS '发布人';
COMMENT ON COLUMN ge_graph_version.published_time IS '发布时间';

COMMENT ON TABLE engine_audit_log IS '引擎公共审计日志表，记录路径、规则、图谱、Dify等引擎关键操作';
COMMENT ON COLUMN engine_audit_log.trace_id IS '调用链追踪ID';
COMMENT ON COLUMN engine_audit_log.engine_type IS '引擎类型：PATHWAY/RULE/GRAPH/DIFY/TERMINOLOGY/ADAPTER';
COMMENT ON COLUMN engine_audit_log.action_type IS '操作类型';
COMMENT ON COLUMN engine_audit_log.target_type IS '操作对象类型';
COMMENT ON COLUMN engine_audit_log.target_code IS '操作对象编码';
COMMENT ON COLUMN engine_audit_log.patient_id IS '患者ID';
COMMENT ON COLUMN engine_audit_log.encounter_id IS '就诊ID';
COMMENT ON COLUMN engine_audit_log.operator_id IS '操作人';
COMMENT ON COLUMN engine_audit_log.tenant_id IS '租户ID';
COMMENT ON COLUMN engine_audit_log.group_code IS '集团编码';
COMMENT ON COLUMN engine_audit_log.hospital_code IS '医院编码';
COMMENT ON COLUMN engine_audit_log.campus_code IS '院区编码';
COMMENT ON COLUMN engine_audit_log.site_code IS '站点/卫生所编码';
COMMENT ON COLUMN engine_audit_log.department_code IS '科室编码';
COMMENT ON COLUMN engine_audit_log.scope_level IS '组织作用域层级';
COMMENT ON COLUMN engine_audit_log.scope_code IS '组织作用域编码';
COMMENT ON COLUMN engine_audit_log.org_source IS '组织上下文来源';
COMMENT ON COLUMN engine_audit_log.detail_json IS '操作详情JSON';

COMMENT ON TABLE src_document IS '来源追溯-来源文档主表';
COMMENT ON TABLE src_citation IS '来源追溯-文献引用片段表';
COMMENT ON TABLE src_asset_binding IS '来源追溯-业务资产与引用绑定表';
COMMENT ON TABLE src_review_record IS '来源追溯-发布前来源审核记录表';
COMMENT ON TABLE src_runtime_evidence IS '来源追溯-运行时证据链表';

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

PROMPT ZYENGINE core tables and comments are ready.
