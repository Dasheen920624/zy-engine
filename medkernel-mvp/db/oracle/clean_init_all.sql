-- MedKernel Oracle 首发干净初始化脚本
-- 版本：0.3.0-clean-baseline
-- 用途：首次发布或明确要求清空重建时使用；会删除当前 MEDKERNEL schema 下全部对象。
-- 约定：后续迭代使用 migrations/Vx_y_z__说明.sql 增量迁移，不在升级场景执行本脚本。

SET DEFINE OFF;
SET SERVEROUTPUT ON SIZE UNLIMITED;
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK;

PROMPT [1/8] 清理当前 MEDKERNEL schema 下全部对象...

BEGIN
  FOR r IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type = 'R') LOOP
    BEGIN
      EXECUTE IMMEDIATE 'ALTER TABLE "' || r.table_name || '" DROP CONSTRAINT "' || r.constraint_name || '"';
    EXCEPTION WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('跳过外键约束: ' || r.table_name || '.' || r.constraint_name || ' - ' || SQLERRM);
    END;
  END LOOP;

  FOR r IN (SELECT table_name FROM user_tables) LOOP
    BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE "' || r.table_name || '" CASCADE CONSTRAINTS PURGE';
      DBMS_OUTPUT.PUT_LINE('已删除表: ' || r.table_name);
    EXCEPTION WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('删除表失败: ' || r.table_name || ' - ' || SQLERRM);
      RAISE;
    END;
  END LOOP;

  FOR r IN (SELECT object_name, object_type FROM user_objects
             WHERE object_type IN ('VIEW','MATERIALIZED VIEW','SEQUENCE','TRIGGER','PROCEDURE','FUNCTION','PACKAGE BODY','PACKAGE','TYPE BODY','TYPE','SYNONYM')) LOOP
    BEGIN
      IF r.object_type = 'PACKAGE BODY' THEN
        EXECUTE IMMEDIATE 'DROP PACKAGE BODY "' || r.object_name || '"';
      ELSIF r.object_type = 'TYPE BODY' THEN
        EXECUTE IMMEDIATE 'DROP TYPE BODY "' || r.object_name || '"';
      ELSE
        EXECUTE IMMEDIATE 'DROP ' || r.object_type || ' "' || r.object_name || '"';
      END IF;
      DBMS_OUTPUT.PUT_LINE('已删除对象: ' || r.object_type || ' ' || r.object_name);
    EXCEPTION WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('跳过对象: ' || r.object_type || ' ' || r.object_name || ' - ' || SQLERRM);
    END;
  END LOOP;

  EXECUTE IMMEDIATE 'PURGE RECYCLEBIN';
EXCEPTION WHEN OTHERS THEN
  IF SQLCODE = -38302 THEN
    NULL;
  ELSE
    RAISE;
  END IF;
END;
/

PROMPT [2/8] 创建核心引擎与数据治理表...
@@medkernel_core_ddl_with_comments.sql
@@medkernel_org_context_migration.sql
@@medkernel_comments_unistr.sql
@@data_governance_ddl.sql
@@mpi_ddl.sql
@@re_rule_eval_result_ddl.sql

PROMPT [3/8] 创建数据库版本管理表...

CREATE TABLE schema_version (
  id NUMBER(20) PRIMARY KEY,
  version VARCHAR2(32) NOT NULL,
  script_name VARCHAR2(200) NOT NULL,
  script_type VARCHAR2(32) NOT NULL,
  checksum VARCHAR2(128),
  applied_by VARCHAR2(64) DEFAULT USER NOT NULL,
  applied_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  description VARCHAR2(500),
  CONSTRAINT uk_schema_version UNIQUE (version, script_name)
);

COMMENT ON TABLE schema_version IS '数据库版本记录表：记录首发基线和后续增量迁移执行历史';
COMMENT ON COLUMN schema_version.id IS '主键ID';
COMMENT ON COLUMN schema_version.version IS '数据库结构版本号';
COMMENT ON COLUMN schema_version.script_name IS '执行脚本名称';
COMMENT ON COLUMN schema_version.script_type IS '脚本类型：BASELINE/MIGRATION/HOTFIX';
COMMENT ON COLUMN schema_version.checksum IS '脚本校验摘要';
COMMENT ON COLUMN schema_version.applied_by IS '执行数据库用户';
COMMENT ON COLUMN schema_version.applied_time IS '执行时间';
COMMENT ON COLUMN schema_version.description IS '脚本说明';

INSERT INTO schema_version (id, version, script_name, script_type, description)
VALUES (1, '0.3.0', 'clean_init_all.sql', 'BASELINE', '首发干净基线：清空MEDKERNEL schema并重建全部表与初始化数据');

PROMPT [4/8] 创建安全、认证、SSO、用户同步和审计链表...

-- 租户表：多租户隔离基础
CREATE TABLE sec_tenant (
  id NUMBER(20) PRIMARY KEY,
  tenant_code VARCHAR2(64) NOT NULL,
  tenant_name VARCHAR2(200) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  contact_name VARCHAR2(100),
  contact_phone VARCHAR2(32),
  contact_email VARCHAR2(200),
  license_type VARCHAR2(32),
  max_users NUMBER(10) DEFAULT 1000 NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_tenant UNIQUE (tenant_code)
);
COMMENT ON TABLE sec_tenant IS '租户表：多租户隔离基础';

-- 用户表：平台独立账号，支持本地登录、SSO 和外部同步用户。
CREATE TABLE sec_user (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  username VARCHAR2(64) NOT NULL,
  password_hash VARCHAR2(256) NOT NULL,
  display_name VARCHAR2(100) NOT NULL,
  email VARCHAR2(200),
  phone VARCHAR2(32),
  avatar_url VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  user_type VARCHAR2(32),
  employee_id VARCHAR2(64),
  last_login_time TIMESTAMP,
  last_login_ip VARCHAR2(64),
  login_attempts NUMBER(10) DEFAULT 0 NOT NULL,
  locked_until TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_user UNIQUE (tenant_id, username)
);
COMMENT ON TABLE sec_user IS '用户表：平台独立账号，支持本地登录、SSO和外部同步用户';

-- 角色表：系统预置角色与租户自定义角色。
CREATE TABLE sec_role (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  role_code VARCHAR2(64) NOT NULL,
  role_name VARCHAR2(100) NOT NULL,
  role_type VARCHAR2(32) NOT NULL,
  description VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_role UNIQUE (tenant_id, role_code)
);
COMMENT ON TABLE sec_role IS '角色表：系统预置角色与租户自定义角色';

-- 权限表：菜单、按钮、API 的细粒度权限。
CREATE TABLE sec_permission (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  permission_code VARCHAR2(128) NOT NULL,
  permission_name VARCHAR2(200) NOT NULL,
  permission_type VARCHAR2(32) NOT NULL,
  resource_path VARCHAR2(500),
  description VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_permission UNIQUE (tenant_id, permission_code)
);
COMMENT ON TABLE sec_permission IS '权限表：菜单、按钮、API的细粒度权限';

CREATE TABLE sec_user_role (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  role_id NUMBER(20) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_role UNIQUE (tenant_id, user_id, role_id),
  CONSTRAINT fk_sec_user_role_user FOREIGN KEY (user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_user_role_role FOREIGN KEY (role_id) REFERENCES sec_role(id)
);
COMMENT ON TABLE sec_user_role IS '用户角色关联表';

CREATE TABLE sec_role_permission (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  role_id NUMBER(20) NOT NULL,
  permission_id NUMBER(20) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_role_permission UNIQUE (tenant_id, role_id, permission_id),
  CONSTRAINT fk_sec_role_perm_role FOREIGN KEY (role_id) REFERENCES sec_role(id),
  CONSTRAINT fk_sec_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sec_permission(id)
);
COMMENT ON TABLE sec_role_permission IS '角色权限关联表';

CREATE TABLE sec_user_org_scope (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  scope_level VARCHAR2(32) NOT NULL,
  scope_code VARCHAR2(64) NOT NULL,
  scope_name VARCHAR2(200),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_org_scope UNIQUE (tenant_id, user_id, scope_level, scope_code),
  CONSTRAINT fk_sec_user_org_user FOREIGN KEY (user_id) REFERENCES sec_user(id)
);
COMMENT ON TABLE sec_user_org_scope IS '用户组织范围表：限定用户可访问的组织范围';

CREATE TABLE sec_auth_audit_log (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20),
  user_id NUMBER(20),
  username VARCHAR2(64),
  event_type VARCHAR2(64) NOT NULL,
  event_result VARCHAR2(32) NOT NULL,
  ip_address VARCHAR2(64),
  user_agent VARCHAR2(500),
  failure_reason VARCHAR2(500),
  trace_id VARCHAR2(128),
  record_hash VARCHAR2(128),
  chain_hash VARCHAR2(128),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
COMMENT ON TABLE sec_auth_audit_log IS '认证审计日志表：记录登录、登出、失败和锁定事件';

CREATE TABLE sec_identity_provider (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  provider_code VARCHAR2(64) NOT NULL,
  provider_name VARCHAR2(200) NOT NULL,
  provider_type VARCHAR2(32) NOT NULL,
  adapter_code VARCHAR2(64),
  query_code VARCHAR2(64),
  sync_mode VARCHAR2(32) DEFAULT 'MANUAL',
  sync_cron VARCHAR2(100),
  priority NUMBER(10) DEFAULT 100 NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  config_json CLOB,
  last_sync_time TIMESTAMP,
  last_sync_result VARCHAR2(32),
  last_sync_summary CLOB,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_provider UNIQUE (tenant_id, provider_code)
);
COMMENT ON TABLE sec_identity_provider IS '身份源配置表：注册HIS、EMR、OA、LDAP、CAS、OIDC、SAML等外部身份源';

CREATE TABLE sec_sync_source (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  source_code VARCHAR2(64) NOT NULL,
  source_name VARCHAR2(200) NOT NULL,
  source_type VARCHAR2(32) NOT NULL,
  connection_config CLOB,
  sync_mode VARCHAR2(32) DEFAULT 'MANUAL' NOT NULL,
  cron_expression VARCHAR2(100),
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  description VARCHAR2(500),
  last_sync_time TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_sync_source UNIQUE (tenant_id, source_code)
);
COMMENT ON TABLE sec_sync_source IS '用户同步源表：配置HIS、EMR、OA、统一身份平台等用户来源';

CREATE TABLE sec_identity_binding (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20),
  provider_id NUMBER(20),
  external_subject VARCHAR2(200),
  external_org_code VARCHAR2(64),
  external_org_name VARCHAR2(200),
  external_position VARCHAR2(200),
  platform_user_id NUMBER(20),
  source_id NUMBER(20),
  external_id VARCHAR2(128),
  external_username VARCHAR2(128),
  external_display_name VARCHAR2(200),
  external_name VARCHAR2(200),
  binding_status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  last_verified_time TIMESTAMP,
  last_sync_time TIMESTAMP,
  sync_hash VARCHAR2(256),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_binding_user FOREIGN KEY (user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_binding_platform_user FOREIGN KEY (platform_user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_binding_provider FOREIGN KEY (provider_id) REFERENCES sec_identity_provider(id),
  CONSTRAINT fk_sec_binding_source FOREIGN KEY (source_id) REFERENCES sec_sync_source(id)
);
COMMENT ON TABLE sec_identity_binding IS '外部身份绑定表：统一支持SSO身份绑定与用户同步身份映射';
CREATE UNIQUE INDEX uk_sec_binding_provider ON sec_identity_binding(tenant_id, provider_id, external_subject);
CREATE UNIQUE INDEX uk_sec_binding_source ON sec_identity_binding(tenant_id, source_id, external_id);

CREATE TABLE sec_sync_task (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  source_id NUMBER(20) NOT NULL,
  task_type VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  total_count NUMBER(10) DEFAULT 0,
  success_count NUMBER(10) DEFAULT 0,
  failed_count NUMBER(10) DEFAULT 0,
  skip_count NUMBER(10) DEFAULT 0,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  error_message CLOB,
  triggered_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_sync_task_source FOREIGN KEY (source_id) REFERENCES sec_sync_source(id)
);
COMMENT ON TABLE sec_sync_task IS '用户同步任务表：记录全量、增量、手动同步任务';

CREATE TABLE sec_sync_log (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  task_id NUMBER(20) NOT NULL,
  external_id VARCHAR2(128) NOT NULL,
  external_username VARCHAR2(128),
  platform_user_id NUMBER(20),
  operation VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  error_message CLOB,
  sync_data CLOB,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT fk_sec_sync_log_task FOREIGN KEY (task_id) REFERENCES sec_sync_task(id)
);
COMMENT ON TABLE sec_sync_log IS '用户同步日志表：记录单个外部用户同步结果';

CREATE TABLE sec_user_merge (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  source_user_id NUMBER(20) NOT NULL,
  target_user_id NUMBER(20) NOT NULL,
  merge_reason VARCHAR2(500),
  merge_status VARCHAR2(32) NOT NULL,
  merged_by VARCHAR2(64),
  merged_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_user_merge_source FOREIGN KEY (source_user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_user_merge_target FOREIGN KEY (target_user_id) REFERENCES sec_user(id)
);
COMMENT ON TABLE sec_user_merge IS '用户合并操作表：记录多身份源用户合并过程';

CREATE TABLE sec_user_unbind (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  binding_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  unbind_reason VARCHAR2(500),
  unbind_status VARCHAR2(32) NOT NULL,
  previous_status VARCHAR2(32) NOT NULL,
  new_status VARCHAR2(32) NOT NULL,
  unbound_by VARCHAR2(64),
  unbound_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT fk_sec_user_unbind_binding FOREIGN KEY (binding_id) REFERENCES sec_identity_binding(id),
  CONSTRAINT fk_sec_user_unbind_user FOREIGN KEY (user_id) REFERENCES sec_user(id)
);
COMMENT ON TABLE sec_user_unbind IS '用户解绑操作表：记录身份绑定解绑过程';

CREATE TABLE sec_sso_config (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  config_code VARCHAR2(64) NOT NULL,
  config_name VARCHAR2(200) NOT NULL,
  protocol_type VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  priority NUMBER(10) DEFAULT 100 NOT NULL,
  cas_server_url VARCHAR2(500),
  cas_service_url VARCHAR2(500),
  cas_callback_url VARCHAR2(500),
  oidc_issuer VARCHAR2(500),
  oidc_client_id VARCHAR2(200),
  oidc_client_secret VARCHAR2(500),
  oidc_redirect_uri VARCHAR2(500),
  oidc_scope VARCHAR2(500),
  oidc_response_type VARCHAR2(64),
  oidc_jwks_uri VARCHAR2(500),
  saml_entity_id VARCHAR2(500),
  saml_sso_url VARCHAR2(500),
  saml_slo_url VARCHAR2(500),
  saml_certificate CLOB,
  saml_metadata_url VARCHAR2(500),
  ldap_url VARCHAR2(500),
  ldap_base_dn VARCHAR2(500),
  ldap_bind_dn VARCHAR2(500),
  ldap_bind_password VARCHAR2(500),
  ldap_user_search_base VARCHAR2(500),
  ldap_user_search_filter VARCHAR2(500),
  ldap_group_search_base VARCHAR2(500),
  ldap_group_search_filter VARCHAR2(500),
  ldap_use_ssl NUMBER(1) DEFAULT 0,
  ldap_use_starttls NUMBER(1) DEFAULT 0,
  attribute_mapping CLOB,
  role_mapping CLOB,
  auto_create_user NUMBER(1) DEFAULT 0,
  auto_update_user NUMBER(1) DEFAULT 1,
  session_timeout_minutes NUMBER(10) DEFAULT 480,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_sso_config UNIQUE (tenant_id, config_code)
);
COMMENT ON TABLE sec_sso_config IS 'SSO配置表：存储CAS、OIDC、SAML、LDAP-AD接入策略';

CREATE TABLE sec_sso_session (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  config_id NUMBER(20) NOT NULL,
  external_subject VARCHAR2(200),
  external_name VARCHAR2(200),
  external_email VARCHAR2(200),
  session_token VARCHAR2(500),
  access_token VARCHAR2(1000),
  refresh_token VARCHAR2(1000),
  id_token CLOB,
  token_type VARCHAR2(32),
  issued_at TIMESTAMP,
  expires_at TIMESTAMP,
  refresh_expires_at TIMESTAMP,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  ip_address VARCHAR2(64),
  user_agent VARCHAR2(500),
  last_access_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_sso_session_user FOREIGN KEY (user_id) REFERENCES sec_user(id),
  CONSTRAINT fk_sec_sso_session_config FOREIGN KEY (config_id) REFERENCES sec_sso_config(id)
);
COMMENT ON TABLE sec_sso_session IS 'SSO会话表：存储单点登录会话和令牌快照';

CREATE TABLE sec_sso_audit_log (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20),
  config_id NUMBER(20),
  event_type VARCHAR2(64) NOT NULL,
  event_result VARCHAR2(32) NOT NULL,
  external_subject VARCHAR2(200),
  error_code VARCHAR2(64),
  error_message VARCHAR2(1000),
  ip_address VARCHAR2(64),
  user_agent VARCHAR2(500),
  trace_id VARCHAR2(128),
  record_hash VARCHAR2(128),
  chain_hash VARCHAR2(128),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
COMMENT ON TABLE sec_sso_audit_log IS 'SSO审计日志表：记录单点登录相关操作';

CREATE TABLE sec_encryption_key (
  id NUMBER(20) PRIMARY KEY,
  key_id VARCHAR2(64) NOT NULL,
  key_version NUMBER(10) NOT NULL,
  algorithm VARCHAR2(32) NOT NULL,
  key_material VARCHAR2(500) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  activated_at TIMESTAMP NOT NULL,
  deprecated_at TIMESTAMP,
  expires_at TIMESTAMP,
  description VARCHAR2(500),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_encryption_key UNIQUE (key_id, key_version)
);
COMMENT ON TABLE sec_encryption_key IS '加密密钥管理表：维护审计链和敏感配置的密钥版本';

CREATE TABLE sec_audit_chain_checkpoint (
  id NUMBER(20) PRIMARY KEY,
  checkpoint_time TIMESTAMP NOT NULL,
  last_checked_id NUMBER(20) NOT NULL,
  chain_status VARCHAR2(32) NOT NULL,
  total_records NUMBER(20) NOT NULL,
  valid_records NUMBER(20) NOT NULL,
  broken_records NUMBER(20) DEFAULT 0,
  first_broken_id NUMBER(20),
  details CLOB,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);
COMMENT ON TABLE sec_audit_chain_checkpoint IS '审计链校验点表：记录防篡改链校验结果';

ALTER TABLE engine_audit_log ADD record_hash VARCHAR2(128);
ALTER TABLE engine_audit_log ADD chain_hash VARCHAR2(128);

CREATE INDEX idx_sec_user_username ON sec_user(tenant_id, username, status);
CREATE INDEX idx_sec_user_employee ON sec_user(tenant_id, employee_id);
CREATE INDEX idx_sec_auth_audit_user ON sec_auth_audit_log(tenant_id, user_id, created_time);
CREATE INDEX idx_identity_provider_type ON sec_identity_provider(tenant_id, provider_type, status);
CREATE INDEX idx_identity_binding_user ON sec_identity_binding(tenant_id, user_id);
CREATE INDEX idx_identity_binding_platform ON sec_identity_binding(platform_user_id);
CREATE INDEX idx_identity_binding_source ON sec_identity_binding(source_id);
CREATE INDEX idx_sec_sync_task_tenant_source ON sec_sync_task(tenant_id, source_id);
CREATE INDEX idx_sec_sync_task_status ON sec_sync_task(status);
CREATE INDEX idx_sec_sync_log_task ON sec_sync_log(task_id);
CREATE INDEX idx_sec_user_merge_tenant ON sec_user_merge(tenant_id);
CREATE INDEX idx_sec_user_unbind_tenant ON sec_user_unbind(tenant_id);
CREATE INDEX idx_sso_config_tenant ON sec_sso_config(tenant_id, status);
CREATE INDEX idx_sso_session_user ON sec_sso_session(tenant_id, user_id, status);
CREATE INDEX idx_sso_session_token ON sec_sso_session(session_token);
CREATE INDEX idx_sso_audit_tenant ON sec_sso_audit_log(tenant_id, created_time);
CREATE INDEX idx_encryption_key_status ON sec_encryption_key(status, key_version);
CREATE INDEX idx_audit_chain_checkpoint_time ON sec_audit_chain_checkpoint(checkpoint_time);


PROMPT [5/8] 创建业务扩展表...

-- 来源：ai-dev-input/04_database/oracle/ai_candidate_review_ddl.sql
-- FE-AI-001 AI候选配置审核台 DDL (Oracle)

CREATE TABLE ai_candidate_review (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  candidate_code VARCHAR2(64) NOT NULL,
  candidate_type VARCHAR2(32) NOT NULL,
  candidate_name VARCHAR2(200),
  source_code VARCHAR2(64),
  source_name VARCHAR2(200),
  model_provider VARCHAR2(64),
  model_name VARCHAR2(128),
  confidence NUMBER(5,4),
  candidate_content VARCHAR2(4000),
  review_status VARCHAR2(16) DEFAULT 'PENDING' NOT NULL,
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  modified_content VARCHAR2(4000),
  quality_findings VARCHAR2(2000),
  priority VARCHAR2(16) DEFAULT 'MEDIUM',
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_candidate_review UNIQUE (tenant_id, candidate_code)
);

COMMENT ON TABLE ai_candidate_review IS 'AI候选配置审核表';
COMMENT ON COLUMN ai_candidate_review.id IS '主键ID';
COMMENT ON COLUMN ai_candidate_review.tenant_id IS '租户ID';
COMMENT ON COLUMN ai_candidate_review.candidate_code IS '候选编码';
COMMENT ON COLUMN ai_candidate_review.candidate_type IS '候选类型';
COMMENT ON COLUMN ai_candidate_review.candidate_name IS '候选名称';
COMMENT ON COLUMN ai_candidate_review.source_code IS '来源编码';
COMMENT ON COLUMN ai_candidate_review.source_name IS '来源名称';
COMMENT ON COLUMN ai_candidate_review.model_provider IS '模型供应商';
COMMENT ON COLUMN ai_candidate_review.model_name IS '模型名称';
COMMENT ON COLUMN ai_candidate_review.confidence IS '置信度';
COMMENT ON COLUMN ai_candidate_review.candidate_content IS '候选内容';
COMMENT ON COLUMN ai_candidate_review.review_status IS '审核状态: PENDING/APPROVED/REJECTED/MODIFIED';
COMMENT ON COLUMN ai_candidate_review.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_candidate_review.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_candidate_review.review_note IS '审核备注';
COMMENT ON COLUMN ai_candidate_review.modified_content IS '修改后内容';
COMMENT ON COLUMN ai_candidate_review.quality_findings IS '质检发现';
COMMENT ON COLUMN ai_candidate_review.priority IS '优先级: HIGH/MEDIUM/LOW';
COMMENT ON COLUMN ai_candidate_review.created_by IS '创建人';
COMMENT ON COLUMN ai_candidate_review.created_time IS '创建时间';
COMMENT ON COLUMN ai_candidate_review.updated_by IS '更新人';
COMMENT ON COLUMN ai_candidate_review.updated_time IS '更新时间';

CREATE INDEX idx_ai_candidate_review_tenant ON ai_candidate_review (tenant_id, candidate_type, review_status);
CREATE INDEX idx_ai_candidate_review_status ON ai_candidate_review (tenant_id, review_status, priority);

-- 来源：ai-dev-input/04_database/oracle/ai_governance_ddl.sql
-- AI-GOV-001 AI治理 DDL (Oracle)

-- 模型注册表
CREATE TABLE ai_model_registry (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  model_code VARCHAR2(64) NOT NULL,
  model_name VARCHAR2(128) NOT NULL,
  model_provider VARCHAR2(32) NOT NULL,
  model_version VARCHAR2(32),
  model_type VARCHAR2(32),
  endpoint_url VARCHAR2(500),
  api_key_ref VARCHAR2(128),
  timeout_ms NUMBER(10) DEFAULT 5000,
  max_tokens NUMBER(10) DEFAULT 4096,
  temperature NUMBER(3,2) DEFAULT 0.70,
  status VARCHAR2(16) DEFAULT 'REGISTERED' NOT NULL,
  review_status VARCHAR2(16) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_model_registry UNIQUE (tenant_id, model_code)
);

COMMENT ON TABLE ai_model_registry IS '模型注册表';
COMMENT ON COLUMN ai_model_registry.model_code IS '模型编码';
COMMENT ON COLUMN ai_model_registry.model_name IS '模型名称';
COMMENT ON COLUMN ai_model_registry.model_provider IS '模型供应商';
COMMENT ON COLUMN ai_model_registry.model_version IS '模型版本';
COMMENT ON COLUMN ai_model_registry.model_type IS '模型类型';
COMMENT ON COLUMN ai_model_registry.endpoint_url IS '服务端点URL';
COMMENT ON COLUMN ai_model_registry.api_key_ref IS 'API密钥引用';
COMMENT ON COLUMN ai_model_registry.timeout_ms IS '超时时间(毫秒)';
COMMENT ON COLUMN ai_model_registry.max_tokens IS '最大Token数';
COMMENT ON COLUMN ai_model_registry.temperature IS '温度参数';
COMMENT ON COLUMN ai_model_registry.status IS '状态: REGISTERED/ACTIVE/DEPRECATED/RETIRED';
COMMENT ON COLUMN ai_model_registry.review_status IS '审核状态: PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN ai_model_registry.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_model_registry.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_model_registry.review_note IS '审核备注';
COMMENT ON COLUMN ai_model_registry.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN ai_model_registry.description IS '描述';
COMMENT ON COLUMN ai_model_registry.created_by IS '创建人';
COMMENT ON COLUMN ai_model_registry.created_time IS '创建时间';
COMMENT ON COLUMN ai_model_registry.updated_by IS '更新人';
COMMENT ON COLUMN ai_model_registry.updated_time IS '更新时间';

-- 提示词模板表
CREATE TABLE ai_prompt_template (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  template_code VARCHAR2(64) NOT NULL,
  template_name VARCHAR2(200) NOT NULL,
  template_type VARCHAR2(32),
  model_type VARCHAR2(32),
  content CLOB,
  version VARCHAR2(32) DEFAULT '1.0.0',
  variables VARCHAR2(2000),
  hash VARCHAR2(128),
  status VARCHAR2(16) DEFAULT 'DRAFT' NOT NULL,
  review_status VARCHAR2(16) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_prompt_template UNIQUE (tenant_id, template_code, version)
);

COMMENT ON TABLE ai_prompt_template IS '提示词模板表';
COMMENT ON COLUMN ai_prompt_template.template_code IS '模板编码';
COMMENT ON COLUMN ai_prompt_template.template_name IS '模板名称';
COMMENT ON COLUMN ai_prompt_template.template_type IS '模板类型';
COMMENT ON COLUMN ai_prompt_template.model_type IS '适用模型类型';
COMMENT ON COLUMN ai_prompt_template.content IS '模板内容';
COMMENT ON COLUMN ai_prompt_template.version IS '模板版本';
COMMENT ON COLUMN ai_prompt_template.variables IS '变量定义';
COMMENT ON COLUMN ai_prompt_template.hash IS '内容哈希';
COMMENT ON COLUMN ai_prompt_template.status IS '状态: DRAFT/PUBLISHED/ARCHIVED';
COMMENT ON COLUMN ai_prompt_template.review_status IS '审核状态: PENDING/APPROVED/REJECTED';
COMMENT ON COLUMN ai_prompt_template.reviewed_by IS '审核人';
COMMENT ON COLUMN ai_prompt_template.reviewed_time IS '审核时间';
COMMENT ON COLUMN ai_prompt_template.review_note IS '审核备注';
COMMENT ON COLUMN ai_prompt_template.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN ai_prompt_template.description IS '描述';
COMMENT ON COLUMN ai_prompt_template.created_by IS '创建人';
COMMENT ON COLUMN ai_prompt_template.created_time IS '创建时间';
COMMENT ON COLUMN ai_prompt_template.updated_by IS '更新人';
COMMENT ON COLUMN ai_prompt_template.updated_time IS '更新时间';

-- 模型评测任务表
CREATE TABLE ai_model_eval_task (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  task_code VARCHAR2(64) NOT NULL,
  task_name VARCHAR2(200) NOT NULL,
  model_code VARCHAR2(64) NOT NULL,
  model_version VARCHAR2(32),
  prompt_template_code VARCHAR2(64),
  prompt_version VARCHAR2(32),
  benchmark_code VARCHAR2(64),
  benchmark_name VARCHAR2(200),
  sample_size NUMBER(10) DEFAULT 100,
  status VARCHAR2(16) DEFAULT 'PENDING' NOT NULL,
  accuracy_score NUMBER(5,4),
  latency_ms NUMBER(10,2),
  pass_rate NUMBER(5,4),
  result_summary VARCHAR2(2000),
  detail_json CLOB,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  completed_time TIMESTAMP,
  CONSTRAINT uk_ai_model_eval_task UNIQUE (tenant_id, task_code)
);

COMMENT ON TABLE ai_model_eval_task IS '模型评测任务表';
COMMENT ON COLUMN ai_model_eval_task.task_code IS '任务编码';
COMMENT ON COLUMN ai_model_eval_task.task_name IS '任务名称';
COMMENT ON COLUMN ai_model_eval_task.model_code IS '评测模型编码';
COMMENT ON COLUMN ai_model_eval_task.model_version IS '评测模型版本';
COMMENT ON COLUMN ai_model_eval_task.prompt_template_code IS '提示词模板编码';
COMMENT ON COLUMN ai_model_eval_task.prompt_version IS '提示词模板版本';
COMMENT ON COLUMN ai_model_eval_task.benchmark_code IS '基准测试编码';
COMMENT ON COLUMN ai_model_eval_task.benchmark_name IS '基准测试名称';
COMMENT ON COLUMN ai_model_eval_task.sample_size IS '样本数量';
COMMENT ON COLUMN ai_model_eval_task.status IS '状态: PENDING/RUNNING/COMPLETED/FAILED';
COMMENT ON COLUMN ai_model_eval_task.accuracy_score IS '准确率评分';
COMMENT ON COLUMN ai_model_eval_task.latency_ms IS '平均延迟(毫秒)';
COMMENT ON COLUMN ai_model_eval_task.pass_rate IS '通过率';
COMMENT ON COLUMN ai_model_eval_task.result_summary IS '结果摘要';
COMMENT ON COLUMN ai_model_eval_task.detail_json IS '详情JSON';
COMMENT ON COLUMN ai_model_eval_task.created_by IS '创建人';
COMMENT ON COLUMN ai_model_eval_task.created_time IS '创建时间';
COMMENT ON COLUMN ai_model_eval_task.completed_time IS '完成时间';

CREATE INDEX idx_ai_model_registry_tenant ON ai_model_registry (tenant_id, model_type, status);
CREATE INDEX idx_ai_prompt_template_tenant ON ai_prompt_template (tenant_id, template_type, status);
CREATE INDEX idx_ai_model_eval_task_model ON ai_model_eval_task (tenant_id, model_code, status);

-- 来源：ai-dev-input/04_database/oracle/ai_knowledge_job_ddl.sql
-- AI 知识生产任务表
CREATE TABLE ai_knowledge_job (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  job_code VARCHAR2(64) NOT NULL,
  job_name VARCHAR2(200) NOT NULL,
  job_type VARCHAR2(32) NOT NULL,
  source_code VARCHAR2(64),
  subscription_id VARCHAR2(64),
  model_provider VARCHAR2(64),
  model_name VARCHAR2(128),
  prompt_version VARCHAR2(32),
  input_hash VARCHAR2(128),
  output_hash VARCHAR2(128),
  input_summary VARCHAR2(2000),
  output_summary VARCHAR2(2000),
  evidence_ids VARCHAR2(1000),
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  review_status VARCHAR2(32) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(2000),
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  max_retries NUMBER(10) DEFAULT 3 NOT NULL,
  started_time TIMESTAMP,
  finished_time TIMESTAMP,
  duration_ms NUMBER(10),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_knowledge_job UNIQUE (tenant_id, job_code)
);

COMMENT ON TABLE ai_knowledge_job IS 'AI知识生产任务';
COMMENT ON COLUMN ai_knowledge_job.job_type IS '任务类型: EXTRACT/MAP/RULE_GENERATE/GRAPH_BUILD/QUALITY_CHECK';
COMMENT ON COLUMN ai_knowledge_job.status IS '状态: PENDING/RUNNING/SUCCESS/FAILED/RETRY/CANCELLED';
COMMENT ON COLUMN ai_knowledge_job.review_status IS '审核状态: PENDING/APPROVED/REJECTED';

-- AI 模型调用日志表
CREATE TABLE ai_model_call_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  job_id NUMBER(19),
  call_type VARCHAR2(32) NOT NULL,
  model_provider VARCHAR2(64) NOT NULL,
  model_name VARCHAR2(128) NOT NULL,
  model_version VARCHAR2(32),
  prompt_template_id VARCHAR2(64),
  prompt_version VARCHAR2(32),
  prompt_hash VARCHAR2(128),
  input_hash VARCHAR2(128),
  output_hash VARCHAR2(128),
  input_token_count NUMBER(10),
  output_token_count NUMBER(10),
  total_token_count NUMBER(10),
  call_status VARCHAR2(32) NOT NULL,
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  fallback_used VARCHAR2(32),
  fallback_provider VARCHAR2(64),
  fallback_model VARCHAR2(128),
  trace_id VARCHAR2(64),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  elapsed_ms NUMBER(10),
  called_time TIMESTAMP NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

COMMENT ON TABLE ai_model_call_log IS 'AI模型调用日志';
COMMENT ON COLUMN ai_model_call_log.call_type IS '调用类型: RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/WORKFLOW';
COMMENT ON COLUMN ai_model_call_log.call_status IS '调用状态: SUCCESS/ERROR/FALLBACK/TIMEOUT';

-- 来源：ai-dev-input/04_database/oracle/ai_knowledge_sync_log_ddl.sql
-- AI 知识同步日志表：记录知识来源同步历史、差异和审核状态
-- 支持定时同步、手动同步、差异预览、失败重试、前台审核和驳回

CREATE TABLE ai_knowledge_sync_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  sync_code VARCHAR2(64) NOT NULL,
  source_code VARCHAR2(64) NOT NULL,
  subscription_id VARCHAR2(64),
  sync_type VARCHAR2(32) NOT NULL,
  sync_mode VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  diff_summary CLOB,
  diff_detail CLOB,
  items_added NUMBER(10) DEFAULT 0,
  items_updated NUMBER(10) DEFAULT 0,
  items_deleted NUMBER(10) DEFAULT 0,
  items_total NUMBER(10) DEFAULT 0,
  review_status VARCHAR2(32) DEFAULT 'PENDING',
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(2000),
  ops_task_id NUMBER(19),
  error_code VARCHAR2(64),
  error_message VARCHAR2(2000),
  started_time TIMESTAMP,
  completed_time TIMESTAMP,
  duration_ms NUMBER(10),
  triggered_by VARCHAR2(64),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ai_knowledge_sync_log UNIQUE (tenant_id, sync_code)
);

COMMENT ON TABLE ai_knowledge_sync_log IS 'AI知识同步日志';
COMMENT ON COLUMN ai_knowledge_sync_log.sync_type IS '同步类型: AUTO/MANUAL';
COMMENT ON COLUMN ai_knowledge_sync_log.sync_mode IS '同步模式: FULL/INCREMENTAL/DRY_RUN';
COMMENT ON COLUMN ai_knowledge_sync_log.status IS '状态: PENDING/RUNNING/DIFF_READY/APPROVED/SYNCING/COMPLETED/FAILED/CANCELLED';
COMMENT ON COLUMN ai_knowledge_sync_log.review_status IS '审核状态: PENDING/APPROVED/REJECTED';

-- 索引
CREATE INDEX idx_aik_sync_log_tenant ON ai_knowledge_sync_log(tenant_id, status);
CREATE INDEX idx_aik_sync_log_source ON ai_knowledge_sync_log(tenant_id, source_code);
CREATE INDEX idx_aik_sync_log_review ON ai_knowledge_sync_log(tenant_id, review_status);
CREATE INDEX idx_aik_sync_log_ops ON ai_knowledge_sync_log(ops_task_id);

-- 来源：ai-dev-input/04_database/oracle/cdss_override_log_ddl.sql
-- CDSS-003 覆盖审计日志表 (Oracle)
-- 记录规则覆盖/确认/升级的审计日志

CREATE TABLE cdss_override_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  alert_id VARCHAR2(64),
  trigger_code VARCHAR2(64),
  rule_code VARCHAR2(64),
  risk_level VARCHAR2(16),
  alert_level VARCHAR2(16),
  override_type VARCHAR2(32) NOT NULL,
  override_reason VARCHAR2(2000),
  override_category VARCHAR2(32),
  supervisor_name VARCHAR2(128),
  confirmed_by VARCHAR2(128),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  operator_id VARCHAR2(64),
  department_code VARCHAR2(64),
  is_audit_red_line VARCHAR2(5) DEFAULT 'FALSE',
  fatigue_suppressed VARCHAR2(5) DEFAULT 'FALSE',
  override_time TIMESTAMP NOT NULL,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

COMMENT ON TABLE cdss_override_log IS 'CDSS覆盖审计日志表';
COMMENT ON COLUMN cdss_override_log.alert_id IS '告警ID';
COMMENT ON COLUMN cdss_override_log.trigger_code IS '触发点编码';
COMMENT ON COLUMN cdss_override_log.rule_code IS '规则编码';
COMMENT ON COLUMN cdss_override_log.risk_level IS '风险等级';
COMMENT ON COLUMN cdss_override_log.alert_level IS '告警级别: NOTICE/SOFT/BLOCK/ESCALATE';
COMMENT ON COLUMN cdss_override_log.override_type IS '覆盖类型: ACKNOWLEDGE/OVERRIDE/ESCALATE';
COMMENT ON COLUMN cdss_override_log.override_reason IS '覆盖原因';
COMMENT ON COLUMN cdss_override_log.override_category IS '覆盖分类: CLINICAL_JUDGEMENT/ALTERNATIVE_THERAPY/PATIENT_REQUEST/EMERGENCY/OTHER';
COMMENT ON COLUMN cdss_override_log.supervisor_name IS '上级审批人';
COMMENT ON COLUMN cdss_override_log.confirmed_by IS '确认人';
COMMENT ON COLUMN cdss_override_log.patient_id IS '患者ID';
COMMENT ON COLUMN cdss_override_log.encounter_id IS '就诊ID';
COMMENT ON COLUMN cdss_override_log.operator_id IS '操作人ID';
COMMENT ON COLUMN cdss_override_log.department_code IS '科室编码';
COMMENT ON COLUMN cdss_override_log.is_audit_red_line IS '是否审计红线: TRUE/FALSE';
COMMENT ON COLUMN cdss_override_log.fatigue_suppressed IS '是否疲劳抑制: TRUE/FALSE';
COMMENT ON COLUMN cdss_override_log.override_time IS '覆盖时间';
COMMENT ON COLUMN cdss_override_log.created_time IS '创建时间';

CREATE INDEX idx_cdss_override_tenant ON cdss_override_log (tenant_id, override_time);
CREATE INDEX idx_cdss_override_operator ON cdss_override_log (tenant_id, operator_id, rule_code, override_time);
CREATE INDEX idx_cdss_override_patient ON cdss_override_log (tenant_id, patient_id);

-- CDSS-003 疲劳配置表 (Oracle)
-- 定义覆盖疲劳抑制策略

CREATE TABLE cdss_fatigue_config (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  config_code VARCHAR2(64) NOT NULL,
  config_name VARCHAR2(200) NOT NULL,
  rule_code VARCHAR2(64),
  department_code VARCHAR2(64),
  time_window_hours NUMBER(10) DEFAULT 24 NOT NULL,
  override_threshold NUMBER(10) DEFAULT 3 NOT NULL,
  suppress_action VARCHAR2(32) DEFAULT 'SUPPRESS' NOT NULL,
  suppress_level VARCHAR2(16),
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_fatigue_config UNIQUE (tenant_id, config_code)
);

COMMENT ON TABLE cdss_fatigue_config IS 'CDSS疲劳配置表';
COMMENT ON COLUMN cdss_fatigue_config.config_code IS '配置编码';
COMMENT ON COLUMN cdss_fatigue_config.config_name IS '配置名称';
COMMENT ON COLUMN cdss_fatigue_config.rule_code IS '规则编码';
COMMENT ON COLUMN cdss_fatigue_config.department_code IS '科室编码';
COMMENT ON COLUMN cdss_fatigue_config.time_window_hours IS '时间窗口(小时)';
COMMENT ON COLUMN cdss_fatigue_config.override_threshold IS '覆盖阈值';
COMMENT ON COLUMN cdss_fatigue_config.suppress_action IS '抑制动作: SUPPRESS/DOWNGRADE/NOTIFY_SUPERVISOR';
COMMENT ON COLUMN cdss_fatigue_config.suppress_level IS '抑制级别';
COMMENT ON COLUMN cdss_fatigue_config.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN cdss_fatigue_config.description IS '描述';
COMMENT ON COLUMN cdss_fatigue_config.created_by IS '创建人';
COMMENT ON COLUMN cdss_fatigue_config.created_time IS '创建时间';
COMMENT ON COLUMN cdss_fatigue_config.updated_by IS '更新人';
COMMENT ON COLUMN cdss_fatigue_config.updated_time IS '更新时间';

-- 来源：ai-dev-input/04_database/oracle/cdss_safety_red_line_ddl.sql
-- CDSS-004 医疗安全红线扫描 DDL (Oracle)

-- 安全红线定义
CREATE TABLE cdss_safety_red_line (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  red_line_code VARCHAR2(64) NOT NULL,
  red_line_name VARCHAR2(200) NOT NULL,
  category VARCHAR2(32) NOT NULL,
  description VARCHAR2(2000),
  condition_expression VARCHAR2(2000),
  blocking_action VARCHAR2(32),
  severity VARCHAR2(16),
  applicable_scenarios CLOB,
  enabled VARCHAR2(1) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_safety_red_line UNIQUE (tenant_id, red_line_code)
);

COMMENT ON TABLE cdss_safety_red_line IS '安全红线定义';
COMMENT ON COLUMN cdss_safety_red_line.red_line_code IS '红线编码';
COMMENT ON COLUMN cdss_safety_red_line.red_line_name IS '红线名称';
COMMENT ON COLUMN cdss_safety_red_line.category IS '分类: MEDICATION/DIAGNOSIS/PROCEDURE/PATHWAY/AI_OUTPUT';
COMMENT ON COLUMN cdss_safety_red_line.description IS '描述';
COMMENT ON COLUMN cdss_safety_red_line.condition_expression IS '触发条件表达式';
COMMENT ON COLUMN cdss_safety_red_line.blocking_action IS '阻断策略: WARN/BLOCK/ESCALATE/REQUIRE_DUAL_CONFIRM';
COMMENT ON COLUMN cdss_safety_red_line.severity IS '严重度: HIGH/CRITICAL';
COMMENT ON COLUMN cdss_safety_red_line.applicable_scenarios IS '适用场景 JSON';
COMMENT ON COLUMN cdss_safety_red_line.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN cdss_safety_red_line.created_by IS '创建人';
COMMENT ON COLUMN cdss_safety_red_line.created_time IS '创建时间';
COMMENT ON COLUMN cdss_safety_red_line.updated_by IS '更新人';
COMMENT ON COLUMN cdss_safety_red_line.updated_time IS '更新时间';

-- 红线扫描结果
CREATE TABLE cdss_red_line_scan_result (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  scan_code VARCHAR2(64) NOT NULL,
  scan_type VARCHAR2(16),
  red_line_code VARCHAR2(64) NOT NULL,
  red_line_name VARCHAR2(200),
  category VARCHAR2(32),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  trigger_context CLOB,
  violation_detail VARCHAR2(2000),
  severity VARCHAR2(16),
  blocking_action VARCHAR2(32),
  status VARCHAR2(16) DEFAULT 'DETECTED' NOT NULL,
  overridden_by VARCHAR2(64),
  override_reason VARCHAR2(2000),
  resolved_by VARCHAR2(64),
  resolution_note VARCHAR2(2000),
  resolved_time TIMESTAMP,
  scan_by VARCHAR2(64),
  scan_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT uk_cdss_red_line_scan_result UNIQUE (tenant_id, scan_code)
);

COMMENT ON TABLE cdss_red_line_scan_result IS '红线扫描结果';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_code IS '扫描编码';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_type IS '扫描类型: MANUAL/SCHEDULED/REALTIME';
COMMENT ON COLUMN cdss_red_line_scan_result.red_line_code IS '红线编码';
COMMENT ON COLUMN cdss_red_line_scan_result.red_line_name IS '红线名称';
COMMENT ON COLUMN cdss_red_line_scan_result.category IS '分类';
COMMENT ON COLUMN cdss_red_line_scan_result.patient_id IS '患者ID';
COMMENT ON COLUMN cdss_red_line_scan_result.encounter_id IS '就诊ID';
COMMENT ON COLUMN cdss_red_line_scan_result.trigger_context IS '触发上下文 JSON';
COMMENT ON COLUMN cdss_red_line_scan_result.violation_detail IS '违反详情';
COMMENT ON COLUMN cdss_red_line_scan_result.severity IS '严重度: HIGH/CRITICAL';
COMMENT ON COLUMN cdss_red_line_scan_result.blocking_action IS '阻断策略';
COMMENT ON COLUMN cdss_red_line_scan_result.status IS '状态: DETECTED/BLOCKED/OVERRIDDEN/RESOLVED';
COMMENT ON COLUMN cdss_red_line_scan_result.overridden_by IS '覆盖人';
COMMENT ON COLUMN cdss_red_line_scan_result.override_reason IS '覆盖原因';
COMMENT ON COLUMN cdss_red_line_scan_result.resolved_by IS '解决人';
COMMENT ON COLUMN cdss_red_line_scan_result.resolution_note IS '解决备注';
COMMENT ON COLUMN cdss_red_line_scan_result.resolved_time IS '解决时间';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_by IS '扫描人';
COMMENT ON COLUMN cdss_red_line_scan_result.scan_time IS '扫描时间';
COMMENT ON COLUMN cdss_red_line_scan_result.created_time IS '创建时间';

CREATE INDEX idx_red_line_scan_patient ON cdss_red_line_scan_result (tenant_id, patient_id, status);
CREATE INDEX idx_red_line_scan_severity ON cdss_red_line_scan_result (tenant_id, severity, status);

-- 来源：ai-dev-input/04_database/oracle/cdss_trigger_point_ddl.sql
-- CDSS 触发点配置表：定义院内业务触发点和接入策略
CREATE TABLE cdss_trigger_point (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  trigger_code VARCHAR2(64) NOT NULL,
  trigger_name VARCHAR2(200) NOT NULL,
  trigger_type VARCHAR2(32) NOT NULL,
  business_scenario VARCHAR2(32) NOT NULL,
  access_strategy VARCHAR2(32) NOT NULL,
  adapter_code VARCHAR2(64),
  endpoint_url VARCHAR2(500),
  rule_codes VARCHAR2(1000),
  pathway_codes VARCHAR2(1000),
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  risk_level VARCHAR2(16) DEFAULT 'LOW',
  timeout_ms NUMBER(10) DEFAULT 5000 NOT NULL,
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_cdss_trigger_point UNIQUE (tenant_id, trigger_code)
);

COMMENT ON TABLE cdss_trigger_point IS 'CDSS触发点配置表';
COMMENT ON COLUMN cdss_trigger_point.trigger_code IS '触发点编码';
COMMENT ON COLUMN cdss_trigger_point.trigger_name IS '触发点名称';
COMMENT ON COLUMN cdss_trigger_point.trigger_type IS '触发类型: ORDER/EMR/EXAM/PATHWAY/INSURANCE';
COMMENT ON COLUMN cdss_trigger_point.business_scenario IS '业务场景: PRESCRIBE/ADMISSION/DIAGNOSIS/EXAM/PATHWAY_ADMIT/SETTLEMENT';
COMMENT ON COLUMN cdss_trigger_point.access_strategy IS '接入策略: API/IFRAME/CDS_HOOKS/MESSAGE';
COMMENT ON COLUMN cdss_trigger_point.adapter_code IS '适配器编码';
COMMENT ON COLUMN cdss_trigger_point.endpoint_url IS '端点URL';
COMMENT ON COLUMN cdss_trigger_point.rule_codes IS '关联规则编码';
COMMENT ON COLUMN cdss_trigger_point.pathway_codes IS '关联路径编码';
COMMENT ON COLUMN cdss_trigger_point.priority IS '优先级';
COMMENT ON COLUMN cdss_trigger_point.risk_level IS '风险等级: LOW/MEDIUM/HIGH';
COMMENT ON COLUMN cdss_trigger_point.timeout_ms IS '超时时间(毫秒)';
COMMENT ON COLUMN cdss_trigger_point.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN cdss_trigger_point.description IS '描述';

-- 来源：ai-dev-input/04_database/oracle/clinical_safety_ddl.sql
-- RISK-001 临床安全 DDL (Oracle)

-- 危险日志
CREATE TABLE clinical_hazard_log (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  hazard_code VARCHAR2(64) NOT NULL,
  hazard_name VARCHAR2(200) NOT NULL,
  hazard_category VARCHAR2(32) NOT NULL,
  hazard_description VARCHAR2(2000),
  affected_process VARCHAR2(32),
  likelihood VARCHAR2(32),
  severity VARCHAR2(32),
  risk_level VARCHAR2(16),
  control_measures VARCHAR2(2000),
  residual_risk VARCHAR2(16),
  status VARCHAR2(16) DEFAULT 'IDENTIFIED' NOT NULL,
  accepted_by VARCHAR2(64),
  accepted_time TIMESTAMP,
  acceptance_note VARCHAR2(2000),
  blocking_strategy VARCHAR2(32),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_hazard_log UNIQUE (tenant_id, hazard_code)
);

COMMENT ON TABLE clinical_hazard_log IS '危险日志';
COMMENT ON COLUMN clinical_hazard_log.hazard_code IS '危险编码';
COMMENT ON COLUMN clinical_hazard_log.hazard_name IS '危险名称';
COMMENT ON COLUMN clinical_hazard_log.hazard_category IS '危险分类';
COMMENT ON COLUMN clinical_hazard_log.hazard_description IS '危险描述';
COMMENT ON COLUMN clinical_hazard_log.affected_process IS '受影响流程';
COMMENT ON COLUMN clinical_hazard_log.likelihood IS '可能性';
COMMENT ON COLUMN clinical_hazard_log.severity IS '严重度';
COMMENT ON COLUMN clinical_hazard_log.risk_level IS '风险等级';
COMMENT ON COLUMN clinical_hazard_log.control_measures IS '控制措施';
COMMENT ON COLUMN clinical_hazard_log.residual_risk IS '残余风险';
COMMENT ON COLUMN clinical_hazard_log.status IS '状态: IDENTIFIED/ANALYSED/CONTROLLED/ACCEPTED/CLOSED';
COMMENT ON COLUMN clinical_hazard_log.accepted_by IS '接受人';
COMMENT ON COLUMN clinical_hazard_log.accepted_time IS '接受时间';
COMMENT ON COLUMN clinical_hazard_log.acceptance_note IS '接受备注';
COMMENT ON COLUMN clinical_hazard_log.blocking_strategy IS '阻断策略';
COMMENT ON COLUMN clinical_hazard_log.created_by IS '创建人';
COMMENT ON COLUMN clinical_hazard_log.created_time IS '创建时间';
COMMENT ON COLUMN clinical_hazard_log.updated_by IS '更新人';
COMMENT ON COLUMN clinical_hazard_log.updated_time IS '更新时间';

-- 安全案例
CREATE TABLE clinical_safety_case (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  case_code VARCHAR2(64) NOT NULL,
  case_name VARCHAR2(200) NOT NULL,
  case_type VARCHAR2(32),
  scope VARCHAR2(2000),
  goal VARCHAR2(2000),
  argument CLOB,
  evidence_refs CLOB,
  status VARCHAR2(16) DEFAULT 'DRAFT' NOT NULL,
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_note VARCHAR2(2000),
  version VARCHAR2(32) DEFAULT '1.0.0',
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_clinical_safety_case UNIQUE (tenant_id, case_code, version)
);

COMMENT ON TABLE clinical_safety_case IS '安全案例';
COMMENT ON COLUMN clinical_safety_case.case_code IS '案例编码';
COMMENT ON COLUMN clinical_safety_case.case_name IS '案例名称';
COMMENT ON COLUMN clinical_safety_case.case_type IS '案例类型';
COMMENT ON COLUMN clinical_safety_case.scope IS '范围';
COMMENT ON COLUMN clinical_safety_case.goal IS '目标';
COMMENT ON COLUMN clinical_safety_case.argument IS '论证';
COMMENT ON COLUMN clinical_safety_case.evidence_refs IS '证据引用';
COMMENT ON COLUMN clinical_safety_case.status IS '状态: DRAFT/REVIEWED/APPROVED/ARCHIVED';
COMMENT ON COLUMN clinical_safety_case.reviewed_by IS '审核人';
COMMENT ON COLUMN clinical_safety_case.reviewed_time IS '审核时间';
COMMENT ON COLUMN clinical_safety_case.review_note IS '审核备注';
COMMENT ON COLUMN clinical_safety_case.version IS '版本';
COMMENT ON COLUMN clinical_safety_case.created_by IS '创建人';
COMMENT ON COLUMN clinical_safety_case.created_time IS '创建时间';
COMMENT ON COLUMN clinical_safety_case.updated_by IS '更新人';
COMMENT ON COLUMN clinical_safety_case.updated_time IS '更新时间';

CREATE INDEX idx_clinical_hazard_tenant ON clinical_hazard_log (tenant_id, hazard_category, risk_level);
CREATE INDEX idx_clinical_hazard_status ON clinical_hazard_log (tenant_id, status, risk_level);
CREATE INDEX idx_clinical_safety_case_tenant ON clinical_safety_case (tenant_id, case_type, status);

-- 来源：ai-dev-input/04_database/oracle/interop_ddl.sql
-- INTEROP-001: 院内互联互通标准适配矩阵 DDL
-- 扩展适配器表结构，支持多协议和标准

-- 1. 扩展 adp_adapter_def 表，增加协议和标准相关字段
ALTER TABLE adp_adapter_def ADD (
  protocol VARCHAR2(32) DEFAULT 'REST' NOT NULL,
  source_system VARCHAR2(32),
  base_url VARCHAR2(500),
  auth_type VARCHAR2(32) DEFAULT 'NONE',
  timeout_ms NUMBER(10) DEFAULT 30000,
  retry_count NUMBER(3) DEFAULT 3,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. 扩展 adp_query_def 表，增加标准相关字段
ALTER TABLE adp_query_def ADD (
  query_type VARCHAR2(32) DEFAULT 'READ',
  request_template CLOB,
  response_mapping CLOB,
  fhir_resource_type VARCHAR2(64),
  hl7_message_type VARCHAR2(32),
  dicom_sop_class VARCHAR2(64),
  sample_data CLOB,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 创建适配器连接配置表
CREATE TABLE adp_connection_config (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) DEFAULT 'default' NOT NULL,
  hospital_code VARCHAR2(64) DEFAULT 'DEFAULT_HOSPITAL' NOT NULL,
  adapter_code VARCHAR2(64) NOT NULL,
  config_key VARCHAR2(100) NOT NULL,
  config_value CLOB,
  config_type VARCHAR2(32) DEFAULT 'STRING',
  description VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_connection_config UNIQUE (tenant_id, hospital_code, adapter_code, config_key)
);

-- 4. 创建适配器认证凭据表（加密存储）
CREATE TABLE adp_credential (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) DEFAULT 'default' NOT NULL,
  hospital_code VARCHAR2(64) DEFAULT 'DEFAULT_HOSPITAL' NOT NULL,
  adapter_code VARCHAR2(64) NOT NULL,
  credential_type VARCHAR2(32) NOT NULL, -- BASIC/OAUTH2/CERT/APIKEY
  credential_key VARCHAR2(100) NOT NULL,
  credential_value CLOB, -- 加密存储
  expires_at TIMESTAMP,
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_credential UNIQUE (tenant_id, hospital_code, adapter_code, credential_type, credential_key)
);

-- 5. 创建适配器调用日志表
CREATE TABLE adp_call_log (
  id NUMBER(20) PRIMARY KEY,
  trace_id VARCHAR2(128),
  tenant_id VARCHAR2(64) NOT NULL,
  hospital_code VARCHAR2(64) NOT NULL,
  adapter_code VARCHAR2(64) NOT NULL,
  query_code VARCHAR2(64) NOT NULL,
  request_params CLOB,
  response_data CLOB,
  status VARCHAR2(32) NOT NULL, -- SUCCESS/FAILED/TIMEOUT
  error_code VARCHAR2(64),
  error_message VARCHAR2(1000),
  elapsed_ms NUMBER(10),
  patient_id VARCHAR2(64),
  encounter_id VARCHAR2(64),
  operator_id VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. 创建标准映射表
CREATE TABLE adp_standard_mapping (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) DEFAULT 'default' NOT NULL,
  hospital_code VARCHAR2(64) DEFAULT 'DEFAULT_HOSPITAL' NOT NULL,
  adapter_code VARCHAR2(64) NOT NULL,
  source_field VARCHAR2(100) NOT NULL,
  target_standard VARCHAR2(32) NOT NULL, -- HL7/FHIR/CDA/DICOM
  target_field VARCHAR2(100) NOT NULL,
  mapping_rule CLOB, -- 映射规则（JSON或表达式）
  transform_function VARCHAR2(200), -- 转换函数
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_standard_mapping UNIQUE (tenant_id, hospital_code, adapter_code, source_field, target_standard)
);

-- 7. 创建CDS Hooks服务配置表
CREATE TABLE adp_cds_hooks_service (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) DEFAULT 'default' NOT NULL,
  hospital_code VARCHAR2(64) DEFAULT 'DEFAULT_HOSPITAL' NOT NULL,
  hook_id VARCHAR2(64) NOT NULL,
  hook_type VARCHAR2(32) NOT NULL, -- patient-view/order-select/order-sign
  service_id VARCHAR2(64) NOT NULL,
  service_title VARCHAR2(200) NOT NULL,
  description VARCHAR2(1000),
  usage_requirements VARCHAR2(1000),
  prefetch_data CLOB,
  response_template CLOB,
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_cds_hooks_service UNIQUE (tenant_id, hospital_code, hook_id, service_id)
);

-- 8. 创建SMART on FHIR应用配置表
CREATE TABLE adp_smart_app (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) DEFAULT 'default' NOT NULL,
  hospital_code VARCHAR2(64) DEFAULT 'DEFAULT_HOSPITAL' NOT NULL,
  app_id VARCHAR2(64) NOT NULL,
  app_name VARCHAR2(200) NOT NULL,
  app_type VARCHAR2(32) NOT NULL, -- EHR_LAUNCH/PATIENT_LAUNCH
  client_id VARCHAR2(100) NOT NULL,
  client_secret CLOB, -- 加密存储
  redirect_uri VARCHAR2(500) NOT NULL,
  scope VARCHAR2(500),
  launch_url VARCHAR2(500),
  status VARCHAR2(32) DEFAULT 'ACTIVE',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_adp_smart_app UNIQUE (tenant_id, hospital_code, app_id)
);

-- 9. 创建索引
CREATE INDEX idx_adp_call_log_trace ON adp_call_log(trace_id);
CREATE INDEX idx_adp_call_log_adapter ON adp_call_log(tenant_id, hospital_code, adapter_code, query_code);
CREATE INDEX idx_adp_call_log_time ON adp_call_log(created_time);
CREATE INDEX idx_adp_call_log_patient ON adp_call_log(patient_id);

-- 10. 添加注释
COMMENT ON TABLE adp_adapter_def IS '适配器定义表';
COMMENT ON COLUMN adp_adapter_def.protocol IS '通信协议：REST/HL7/FHIR/CDA/DICOM/SOAP/MLLP';
COMMENT ON COLUMN adp_adapter_def.source_system IS '源系统：HIS/EMR/LIS/PACS/INSURANCE/OA';
COMMENT ON COLUMN adp_adapter_def.auth_type IS '认证类型：NONE/BASIC/OAUTH2/CERT/APIKEY';

COMMENT ON TABLE adp_query_def IS '适配器查询定义表';
COMMENT ON COLUMN adp_query_def.query_type IS '查询类型：READ/WRITE/SUBSCRIBE';
COMMENT ON COLUMN adp_query_def.fhir_resource_type IS 'FHIR资源类型：Patient/Encounter/DiagnosticReport等';
COMMENT ON COLUMN adp_query_def.hl7_message_type IS 'HL7消息类型：ADT^A01/ORU^R01等';
COMMENT ON COLUMN adp_query_def.dicom_sop_class IS 'DICOM SOP类：CTImageStorage/MRImageStorage等';

COMMENT ON TABLE adp_connection_config IS '适配器连接配置表';
COMMENT ON TABLE adp_credential IS '适配器认证凭据表（加密存储）';
COMMENT ON TABLE adp_call_log IS '适配器调用日志表';
COMMENT ON TABLE adp_standard_mapping IS '标准映射表';
COMMENT ON TABLE adp_cds_hooks_service IS 'CDS Hooks服务配置表';
COMMENT ON TABLE adp_smart_app IS 'SMART on FHIR应用配置表';

-- 来源：ai-dev-input/04_database/oracle/knowledge_package_ddl.sql
-- 知识包表
CREATE TABLE knowledge_package (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  package_code VARCHAR2(64) NOT NULL,
  package_name VARCHAR2(200) NOT NULL,
  package_version VARCHAR2(32) DEFAULT '1.0.0',
  description VARCHAR2(2000),
  export_type VARCHAR2(16) DEFAULT 'FULL',
  status VARCHAR2(16) DEFAULT 'DRAFT' NOT NULL,
  source_tenant_id VARCHAR2(64),
  source_tenant_name VARCHAR2(200),
  target_tenant_id VARCHAR2(64),
  target_tenant_name VARCHAR2(200),
  rule_count NUMBER(10) DEFAULT 0,
  terminology_count NUMBER(10) DEFAULT 0,
  pathway_count NUMBER(10) DEFAULT 0,
  graph_count NUMBER(10) DEFAULT 0,
  source_count NUMBER(10) DEFAULT 0,
  content_hash VARCHAR2(128),
  content_json CLOB,
  conflict_strategy VARCHAR2(16) DEFAULT 'SKIP',
  sync_mode VARCHAR2(16) DEFAULT 'MANUAL',
  sync_status VARCHAR2(16) DEFAULT 'IDLE',
  sync_error VARCHAR2(2000),
  sync_time TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_knowledge_package UNIQUE (tenant_id, package_code, package_version)
);

COMMENT ON TABLE knowledge_package IS '知识包';
COMMENT ON COLUMN knowledge_package.export_type IS '导出类型: FULL/INCREMENTAL';
COMMENT ON COLUMN knowledge_package.status IS '状态: DRAFT/PUBLISHED/IMPORTED/ARCHIVED';
COMMENT ON COLUMN knowledge_package.conflict_strategy IS '冲突策略: SKIP/OVERWRITE/MERGE';
COMMENT ON COLUMN knowledge_package.sync_mode IS '同步模式: MANUAL/AUTO';
COMMENT ON COLUMN knowledge_package.sync_status IS '同步状态: IDLE/SYNCING/SUCCESS/FAILED';

CREATE INDEX idx_knowledge_package_tenant ON knowledge_package(tenant_id, status);
CREATE INDEX idx_knowledge_package_sync ON knowledge_package(tenant_id, sync_status);

-- 来源：ai-dev-input/04_database/oracle/model_provider_config_ddl.sql
-- 模型 Provider 配置表
CREATE TABLE model_provider_config (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  provider_type VARCHAR2(32) NOT NULL,
  provider_name VARCHAR2(128) NOT NULL,
  endpoint_url VARCHAR2(500),
  api_key VARCHAR2(500),
  model_name VARCHAR2(128),
  model_version VARCHAR2(32),
  timeout_ms NUMBER(10) DEFAULT 5000 NOT NULL,
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  priority NUMBER(10) DEFAULT 0 NOT NULL,
  enabled VARCHAR2(5) DEFAULT 'TRUE' NOT NULL,
  degradation_target VARCHAR2(32),
  config_json VARCHAR2(4000),
  description VARCHAR2(2000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_model_provider_config UNIQUE (tenant_id, provider_type, provider_name)
);

COMMENT ON TABLE model_provider_config IS '模型Provider配置';
COMMENT ON COLUMN model_provider_config.provider_type IS 'Provider类型: RESEARCH/EXTRACT/EMBEDDING/RERANK/CRITIC/DIFY/LOCAL';
COMMENT ON COLUMN model_provider_config.enabled IS '是否启用: TRUE/FALSE';
COMMENT ON COLUMN model_provider_config.priority IS '优先级（数字越大优先级越高）';
COMMENT ON COLUMN model_provider_config.degradation_target IS '降级目标Provider类型';

-- 来源：ai-dev-input/04_database/oracle/notify_ddl.sql
-- 通知和消息中心 DDL (Oracle)
-- NOTIFY-001: 通知和消息中心

-- 通知主表
CREATE TABLE NOTIFY_NOTIFICATION (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  org_code VARCHAR2(64) NOT NULL,
  notification_code VARCHAR2(64) NOT NULL,
  title VARCHAR2(200) NOT NULL,
  content CLOB NOT NULL,
  notification_type VARCHAR2(32) NOT NULL,
  priority VARCHAR2(32) DEFAULT 'NORMAL' NOT NULL,
  status VARCHAR2(32) DEFAULT 'UNREAD' NOT NULL,
  sender_id VARCHAR2(64),
  sender_name VARCHAR2(100),
  recipient_id VARCHAR2(64) NOT NULL,
  recipient_name VARCHAR2(100),
  business_type VARCHAR2(64),
  business_id VARCHAR2(64),
  business_url VARCHAR2(500),
  channel VARCHAR2(32) DEFAULT 'IN_APP' NOT NULL,
  scheduled_time TIMESTAMP,
  sent_time TIMESTAMP,
  read_time TIMESTAMP,
  expire_time TIMESTAMP,
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  max_retries NUMBER(10) DEFAULT 3 NOT NULL,
  error_message VARCHAR2(1000),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_notification UNIQUE (tenant_id, notification_code)
);

-- 通知渠道配置表
CREATE TABLE NOTIFY_CHANNEL_CONFIG (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  channel_code VARCHAR2(64) NOT NULL,
  channel_name VARCHAR2(100) NOT NULL,
  channel_type VARCHAR2(32) NOT NULL,
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  config_json CLOB NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_channel UNIQUE (tenant_id, channel_code)
);

-- 通知模板表
CREATE TABLE NOTIFY_TEMPLATE (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  template_code VARCHAR2(64) NOT NULL,
  template_name VARCHAR2(100) NOT NULL,
  template_type VARCHAR2(32) NOT NULL,
  title_template VARCHAR2(200),
  content_template CLOB NOT NULL,
  channel VARCHAR2(32) NOT NULL,
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_template UNIQUE (tenant_id, template_code, channel)
);

-- 用户订阅设置表
CREATE TABLE NOTIFY_SUBSCRIPTION (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  user_id VARCHAR2(64) NOT NULL,
  notification_type VARCHAR2(32) NOT NULL,
  channel VARCHAR2(32) NOT NULL,
  enabled NUMBER(1) DEFAULT 1 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_notify_subscription UNIQUE (tenant_id, user_id, notification_type, channel)
);

-- 投递日志表
CREATE TABLE NOTIFY_DELIVERY_LOG (
  id NUMBER(20) PRIMARY KEY,
  notification_id NUMBER(20) NOT NULL,
  channel VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) NOT NULL,
  provider_response CLOB,
  error_message VARCHAR2(1000),
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 索引
CREATE INDEX idx_notify_recipient ON NOTIFY_NOTIFICATION(tenant_id, recipient_id, status);
CREATE INDEX idx_notify_type ON NOTIFY_NOTIFICATION(tenant_id, notification_type);
CREATE INDEX idx_notify_business ON NOTIFY_NOTIFICATION(tenant_id, business_type, business_id);
CREATE INDEX idx_notify_created ON NOTIFY_NOTIFICATION(tenant_id, created_time DESC);
CREATE INDEX idx_notify_delivery_notification ON NOTIFY_DELIVERY_LOG(notification_id);
CREATE INDEX idx_notify_subscription_user ON NOTIFY_SUBSCRIPTION(tenant_id, user_id);

-- 表注释
COMMENT ON TABLE NOTIFY_NOTIFICATION IS '通知主表';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.id IS '主键ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.tenant_id IS '租户ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.org_code IS '组织编码';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.notification_code IS '通知编码';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.title IS '通知标题';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.content IS '通知内容';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.notification_type IS '通知类型: SYSTEM/WORKFLOW/ALERT/REMINDER';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.priority IS '优先级: LOW/NORMAL/HIGH/URGENT';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.status IS '状态: UNREAD/READ/ARCHIVED';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sender_id IS '发送人ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sender_name IS '发送人姓名';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.recipient_id IS '接收人ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.recipient_name IS '接收人姓名';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_type IS '关联业务类型';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_id IS '关联业务ID';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.business_url IS '关联业务跳转链接';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.channel IS '通知渠道: IN_APP/EMAIL/SMS/WECHAT';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.scheduled_time IS '定时发送时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.sent_time IS '实际发送时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.read_time IS '阅读时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.expire_time IS '过期时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.retry_count IS '重试次数';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.max_retries IS '最大重试次数';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.error_message IS '错误信息';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.created_by IS '创建人';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.created_time IS '创建时间';
COMMENT ON COLUMN NOTIFY_NOTIFICATION.updated_time IS '更新时间';

COMMENT ON TABLE NOTIFY_CHANNEL_CONFIG IS '通知渠道配置表';
COMMENT ON TABLE NOTIFY_TEMPLATE IS '通知模板表';
COMMENT ON TABLE NOTIFY_SUBSCRIPTION IS '用户订阅设置表';
COMMENT ON TABLE NOTIFY_DELIVERY_LOG IS '投递日志表';

-- 来源：ai-dev-input/04_database/oracle/ops_ddl.sql
-- Oracle OPS DDL for async task management
-- 异步任务管理：统一的异步任务执行、重试、审计机制
-- 中文备注规范：表和关键字段必须有中文注释

-- 异步任务表：记录异步任务执行状态和重试信息
CREATE TABLE ops_sync_task (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  task_code VARCHAR2(64) NOT NULL,
  task_type VARCHAR2(64) NOT NULL,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,
  retry_count NUMBER(10) DEFAULT 0 NOT NULL,
  max_retries NUMBER(10) DEFAULT 3 NOT NULL,
  error_message CLOB,
  result_summary CLOB,
  scheduled_time TIMESTAMP,
  started_time TIMESTAMP,
  completed_time TIMESTAMP,
  triggered_by VARCHAR2(64),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_ops_sync_task UNIQUE (tenant_id, task_code)
);

COMMENT ON TABLE ops_sync_task IS '异步任务表：记录异步任务执行状态和重试信息';
COMMENT ON COLUMN ops_sync_task.tenant_id IS '所属租户ID';
COMMENT ON COLUMN ops_sync_task.task_code IS '任务编码，租户内唯一';
COMMENT ON COLUMN ops_sync_task.task_type IS '任务类型：KNOWLEDGE_SYNC/GRAPH_SYNC/CONFIG_SYNC/EXPORT/IMPORT';
COMMENT ON COLUMN ops_sync_task.status IS '任务状态：PENDING/RUNNING/COMPLETED/FAILED/RETRYING/CANCELLED';
COMMENT ON COLUMN ops_sync_task.retry_count IS '当前重试次数';
COMMENT ON COLUMN ops_sync_task.max_retries IS '最大重试次数，默认3';
COMMENT ON COLUMN ops_sync_task.error_message IS '错误信息（最后一次执行失败时）';
COMMENT ON COLUMN ops_sync_task.result_summary IS '执行结果摘要';
COMMENT ON COLUMN ops_sync_task.scheduled_time IS '计划执行时间';
COMMENT ON COLUMN ops_sync_task.started_time IS '实际开始时间';
COMMENT ON COLUMN ops_sync_task.completed_time IS '完成时间';
COMMENT ON COLUMN ops_sync_task.triggered_by IS '触发人（user_id 或 SCHEDULED/SYSTEM）';

-- 索引
CREATE INDEX idx_ops_sync_task_tenant ON ops_sync_task(tenant_id, status);
CREATE INDEX idx_ops_sync_task_type ON ops_sync_task(tenant_id, task_type);
CREATE INDEX idx_ops_sync_task_scheduled ON ops_sync_task(scheduled_time);

-- 来源：ai-dev-input/04_database/oracle/quality_finding_ddl.sql
-- AIK-004 质量发现表 (Oracle)
-- 记录知识资产质量检测结果

CREATE TABLE quality_finding (
  id NUMBER(19) PRIMARY KEY,
  tenant_id NUMBER(19) NOT NULL,
  finding_code VARCHAR2(64) NOT NULL,
  finding_type VARCHAR2(32) NOT NULL,
  severity VARCHAR2(16) DEFAULT 'WARNING',
  asset_type VARCHAR2(32) NOT NULL,
  asset_code VARCHAR2(128) NOT NULL,
  asset_name VARCHAR2(200),
  asset_version VARCHAR2(32),
  description VARCHAR2(2000),
  detail_json VARCHAR2(4000),
  detection_rule VARCHAR2(500),
  status VARCHAR2(16) DEFAULT 'OPEN' NOT NULL,
  resolved_by VARCHAR2(64),
  resolution_note VARCHAR2(2000),
  resolved_time TIMESTAMP,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_quality_finding UNIQUE (tenant_id, finding_code)
);

COMMENT ON TABLE quality_finding IS '质量发现表';
COMMENT ON COLUMN quality_finding.finding_code IS '发现编码';
COMMENT ON COLUMN quality_finding.finding_type IS '发现类型: MISSING_SOURCE/EXPIRED/UNCLEAR_AUTH/RULE_CONFLICT/LOW_CONFIDENCE/MULTI_CANDIDATE_CONFLICT';
COMMENT ON COLUMN quality_finding.severity IS '严重程度: INFO/WARNING/CRITICAL';
COMMENT ON COLUMN quality_finding.asset_type IS '资产类型: RULE/TERMINOLOGY_MAPPING/KNOWLEDGE_ASSET/PATHWAY';
COMMENT ON COLUMN quality_finding.asset_code IS '资产编码';
COMMENT ON COLUMN quality_finding.asset_name IS '资产名称';
COMMENT ON COLUMN quality_finding.asset_version IS '资产版本';
COMMENT ON COLUMN quality_finding.description IS '描述';
COMMENT ON COLUMN quality_finding.detail_json IS '详情JSON';
COMMENT ON COLUMN quality_finding.detection_rule IS '检测规则';
COMMENT ON COLUMN quality_finding.status IS '状态: OPEN/ACKNOWLEDGED/RESOLVED/DISMISSED';
COMMENT ON COLUMN quality_finding.resolved_by IS '处理人';
COMMENT ON COLUMN quality_finding.resolution_note IS '处理备注';
COMMENT ON COLUMN quality_finding.resolved_time IS '处理时间';
COMMENT ON COLUMN quality_finding.created_by IS '创建人';
COMMENT ON COLUMN quality_finding.created_time IS '创建时间';
COMMENT ON COLUMN quality_finding.updated_by IS '更新人';
COMMENT ON COLUMN quality_finding.updated_time IS '更新时间';

CREATE INDEX idx_quality_finding_tenant ON quality_finding (tenant_id, finding_type, status);
CREATE INDEX idx_quality_finding_asset ON quality_finding (tenant_id, asset_type, asset_code);
CREATE INDEX idx_quality_finding_severity ON quality_finding (tenant_id, severity, status);

-- 来源：ai-dev-input/04_database/oracle/wf_ddl.sql
-- Oracle WF DDL for unified workflow and approval
-- 统一待办和审批工作流：审核、发布、回滚、整改、知识包、合规、同步异常统一进入待办
-- 命名约定：wf_* workflow

-- 待办任务表：统一待办入口
CREATE TABLE wf_todo_task (
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

COMMENT ON TABLE wf_todo_task IS '统一待办任务表：审核、发布、回滚、整改、知识包、合规、同步异常统一入口';
COMMENT ON COLUMN wf_todo_task.tenant_id IS '租户ID';
COMMENT ON COLUMN wf_todo_task.task_code IS '任务编码，唯一标识';
COMMENT ON COLUMN wf_todo_task.business_type IS '业务类型：REVIEW/PUBLISH/ROLLBACK/RECTIFY/KNOWLEDGE/COMPLIANCE/SYNC';
COMMENT ON COLUMN wf_todo_task.business_code IS '业务编码（如配置包编码、规则编码等）';
COMMENT ON COLUMN wf_todo_task.business_version IS '业务版本号';
COMMENT ON COLUMN wf_todo_task.title IS '待办标题';
COMMENT ON COLUMN wf_todo_task.description IS '待办描述';
COMMENT ON COLUMN wf_todo_task.priority IS '优先级：URGENT/HIGH/NORMAL/LOW';
COMMENT ON COLUMN wf_todo_task.status IS '状态：PENDING/APPROVED/REJECTED/CANCELLED/EXPIRED';
COMMENT ON COLUMN wf_todo_task.assigned_type IS '分配类型：USER/ROLE/GROUP';
COMMENT ON COLUMN wf_todo_task.assigned_to IS '分配目标（用户ID或角色编码）';
COMMENT ON COLUMN wf_todo_task.created_by IS '创建人';
COMMENT ON COLUMN wf_todo_task.due_time IS '截止时间';
COMMENT ON COLUMN wf_todo_task.completed_by IS '完成人';
COMMENT ON COLUMN wf_todo_task.completed_time IS '完成时间';
COMMENT ON COLUMN wf_todo_task.completed_comment IS '完成备注';
COMMENT ON COLUMN wf_todo_task.cancelled_by IS '取消人';
COMMENT ON COLUMN wf_todo_task.cancelled_time IS '取消时间';
COMMENT ON COLUMN wf_todo_task.cancel_reason IS '取消原因';
COMMENT ON COLUMN wf_todo_task.metadata_json IS '扩展元数据JSON';

-- 审批操作记录表：审批流程日志
CREATE TABLE wf_approval_action (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  task_id NUMBER(20) NOT NULL,
  task_code VARCHAR2(128) NOT NULL,
  action_type VARCHAR2(32) NOT NULL,
  action_result VARCHAR2(32) NOT NULL,
  operator_id VARCHAR2(64) NOT NULL,
  operator_name VARCHAR2(100),
  approval_comment VARCHAR2(2000),
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

COMMENT ON TABLE wf_approval_action IS '审批操作记录表：审批流程日志';
COMMENT ON COLUMN wf_approval_action.task_id IS '关联待办任务ID';
COMMENT ON COLUMN wf_approval_action.action_type IS '操作类型：APPROVE/REJECT/DELEGATE/CANCEL/EXPIRE/ADD_SIGN';
COMMENT ON COLUMN wf_approval_action.action_result IS '操作结果：SUCCESS/FAILURE';
COMMENT ON COLUMN wf_approval_action.operator_id IS '操作人ID';
COMMENT ON COLUMN wf_approval_action.approval_comment IS '审批意见';
COMMENT ON COLUMN wf_approval_action.delegate_to IS '转办目标用户ID';

-- 审批规则配置表：审批流程规则
CREATE TABLE wf_approval_rule (
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

COMMENT ON TABLE wf_approval_rule IS '审批规则配置表：审批流程规则';
COMMENT ON COLUMN wf_approval_rule.rule_code IS '规则编码';
COMMENT ON COLUMN wf_approval_rule.business_type IS '业务类型：REVIEW/PUBLISH/ROLLBACK/RECTIFY/KNOWLEDGE/COMPLIANCE/SYNC';
COMMENT ON COLUMN wf_approval_rule.approval_type IS '审批类型：SINGLE/MULTI/SEQUENTIAL';
COMMENT ON COLUMN wf_approval_rule.approver_type IS '审批人类型：USER/ROLE/DEPARTMENT_HEAD';
COMMENT ON COLUMN wf_approval_rule.approver_value IS '审批人值（用户ID或角色编码）';
COMMENT ON COLUMN wf_approval_rule.timeout_hours IS '超时时间（小时）';
COMMENT ON COLUMN wf_approval_rule.timeout_action IS '超时动作：AUTO_APPROVE/AUTO_REJECT/NOTIFY';

-- 索引
CREATE INDEX idx_wf_todo_tenant ON wf_todo_task(tenant_id, status, business_type);
CREATE INDEX idx_wf_todo_assigned ON wf_todo_task(tenant_id, assigned_type, assigned_to, status);
CREATE INDEX idx_wf_todo_due ON wf_todo_task(tenant_id, due_time, status);
CREATE INDEX idx_wf_action_task ON wf_approval_action(tenant_id, task_id);
CREATE INDEX idx_wf_action_operator ON wf_approval_action(tenant_id, operator_id);
CREATE INDEX idx_wf_rule_type ON wf_approval_rule(tenant_id, business_type, status);

PROMPT [6/8] 写入首发初始化数据...

INSERT INTO sec_tenant (id, tenant_code, tenant_name, status, contact_name, contact_phone, contact_email, license_type, max_users, created_by)
VALUES (1, 'default', '默认租户', 'ACTIVE', '系统管理员', '00000000000', 'admin@example.com', 'ENTERPRISE', 1000, 'system');

INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (101, 1, 'ADMIN', '系统管理员', 'PLATFORM', '拥有平台全部管理权限', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (102, 1, 'DOCTOR', '医生', 'HOSPITAL', '临床医生使用角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (103, 1, 'QC_NURSE', '质控护士', 'HOSPITAL', '病案质控和护理质控角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (104, 1, 'INSURANCE', '医保专员', 'HOSPITAL', '医保审核与控费角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (105, 1, 'IT_ADMIN', '信息科管理员', 'HOSPITAL', '院内系统配置与运维角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (106, 1, 'IMPLEMENTER', '实施工程师', 'PLATFORM', '项目实施与交付角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (107, 1, 'HOSPITAL_LEADER', '院领导', 'HOSPITAL', '院级管理驾驶舱角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (108, 1, 'MEDICAL_EXPERT', '医学审核专家', 'HOSPITAL', '规则、路径和知识审核角色', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, description, status, created_by) VALUES (109, 1, 'PLATFORM_OPS', '平台运营', 'PLATFORM', '平台运营和客户开通角色', 'ACTIVE', 'system');

INSERT INTO sec_permission (id, tenant_id, permission_code, permission_name, permission_type, resource_path, description, status, created_by)
VALUES (201, 1, '*', '全部权限', 'API', '*', '系统管理员全部权限', 'ACTIVE', 'system');
INSERT INTO sec_role_permission (id, tenant_id, role_id, permission_id, created_by) VALUES (301, 1, 101, 201, 'system');

INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1001, 1, 'admin', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '系统管理员', 'admin@example.com', '13800000001', 'ACTIVE', 'PLATFORM', 'EMP-ADMIN', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1002, 1, 'zhao01', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '赵医生', 'zhao01@example.com', '13800000002', 'ACTIVE', 'DOCTOR', 'EMP-ZHAO01', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1003, 1, 'qian02', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '钱护士', 'qian02@example.com', '13800000003', 'ACTIVE', 'NURSE', 'EMP-QIAN02', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1004, 1, 'sun03', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '孙医保', 'sun03@example.com', '13800000004', 'ACTIVE', 'INSURANCE', 'EMP-SUN03', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1005, 1, 'li04', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '李信息', 'li04@example.com', '13800000005', 'ACTIVE', 'IT', 'EMP-LI04', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1006, 1, 'zhou05', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '周实施', 'zhou05@example.com', '13800000006', 'ACTIVE', 'IMPLEMENTER', 'EMP-ZHOU05', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1007, 1, 'wu06', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '吴院长', 'wu06@example.com', '13800000007', 'ACTIVE', 'LEADER', 'EMP-WU06', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1008, 1, 'zheng07', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '郑专家', 'zheng07@example.com', '13800000008', 'ACTIVE', 'EXPERT', 'EMP-ZHENG07', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, email, phone, status, user_type, employee_id, created_by) VALUES (1009, 1, 'wang08', '$2a$10$GNycXiBqR1ydr7zFjdVuKec3GoB4Y1x.YUyLK2jvhYsUTBEZlSjkC', '王运营', 'wang08@example.com', '13800000009', 'ACTIVE', 'OPS', 'EMP-WANG08', 'system');

INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2001, 1, 1001, 101, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2002, 1, 1002, 102, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2003, 1, 1003, 103, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2004, 1, 1004, 104, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2005, 1, 1005, 105, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2006, 1, 1006, 106, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2007, 1, 1007, 107, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2008, 1, 1008, 108, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2009, 1, 1009, 109, 'system');

INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3001, 1, 1001, 'PLATFORM', 'DEFAULT', '全部', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3002, 1, 1002, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3003, 1, 1003, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3004, 1, 1004, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3005, 1, 1005, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3006, 1, 1006, 'PLATFORM', 'DEFAULT', '全部', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3007, 1, 1007, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3008, 1, 1008, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3009, 1, 1009, 'PLATFORM', 'DEFAULT', '全部', 'system');

INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, sync_mode, priority, status, created_by)
VALUES (4001, 1, 'HIS_MAIN', 'HIS主系统', 'HIS', 'HIS_ADAPTER', 'QUERY_HIS_USERS', 'MANUAL', 10, 'ACTIVE', 'system');
INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, sync_mode, priority, status, created_by)
VALUES (4002, 1, 'EMR_SYSTEM', '电子病历系统', 'EMR', 'EMR_ADAPTER', 'QUERY_EMR_USERS', 'MANUAL', 20, 'ACTIVE', 'system');
INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, sync_mode, priority, status, created_by)
VALUES (4003, 1, 'OA_SYSTEM', '办公自动化系统', 'OA', null, null, 'MANUAL', 30, 'ACTIVE', 'system');

INSERT INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by) VALUES (4101, 1, 'HIS', '医院信息系统', 'HIS', 'MANUAL', 'ACTIVE', 'HIS用户同步源', 'system');
INSERT INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by) VALUES (4102, 1, 'EMR', '电子病历系统', 'EMR', 'MANUAL', 'ACTIVE', 'EMR用户同步源', 'system');
INSERT INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by) VALUES (4103, 1, 'OA', '办公自动化系统', 'OA', 'MANUAL', 'ACTIVE', 'OA用户同步源', 'system');
INSERT INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by) VALUES (4104, 1, 'UNIFIED_IDENTITY', '统一身份平台', 'IDENTITY_PLATFORM', 'MANUAL', 'ACTIVE', '统一身份平台用户同步源', 'system');

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, cas_server_url, cas_service_url, cas_callback_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5001, 1, 'CAS_DEFAULT', 'CAS默认配置', 'CAS', 'DISABLED', 100, 'https://cas.example.com/cas', 'https://medkernel.example.com', 'https://medkernel.example.com/api/sso/cas/callback', 0, 480, 'system');
INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, oidc_issuer, oidc_client_id, oidc_redirect_uri, oidc_scope, oidc_response_type, auto_create_user, session_timeout_minutes, created_by)
VALUES (5002, 1, 'OIDC_DEFAULT', 'OIDC默认配置', 'OIDC', 'DISABLED', 200, 'https://keycloak.example.com/realms/medkernel', 'medkernel-client', 'https://medkernel.example.com/api/sso/oidc/callback', 'openid profile email', 'code', 0, 480, 'system');
INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, saml_entity_id, saml_sso_url, saml_slo_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5003, 1, 'SAML_DEFAULT', 'SAML默认配置', 'SAML', 'DISABLED', 300, 'https://medkernel.example.com/saml/metadata', 'https://idp.example.com/sso/saml', 'https://idp.example.com/slo/saml', 0, 480, 'system');
INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, ldap_url, ldap_base_dn, ldap_user_search_base, ldap_user_search_filter, ldap_use_ssl, auto_create_user, session_timeout_minutes, created_by)
VALUES (5004, 1, 'LDAP_DEFAULT', 'LDAP-AD默认配置', 'LDAP-AD', 'DISABLED', 400, 'ldap://ldap.example.com:389', 'dc=example,dc=com', 'ou=users,dc=example,dc=com', '(sAMAccountName={0})', 0, 0, 480, 'system');

INSERT INTO sec_encryption_key (id, key_id, key_version, algorithm, key_material, status, activated_at, description, created_by)
VALUES (10001, 'master-key-001', 1, 'AES-256-GCM', 'base64:AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=', 'ACTIVE', SYSTIMESTAMP, '系统初始加密密钥', 'system');

COMMIT;

PROMPT [7/8] 补齐所有表和字段中文备注并校验...

DECLARE
  v_comment VARCHAR2(1000);
BEGIN
  FOR r IN (SELECT table_name FROM user_tab_comments WHERE table_name IN (SELECT table_name FROM user_tables) AND comments IS NULL) LOOP
    v_comment := CASE
      WHEN r.table_name LIKE 'SEC_%' THEN '安全权限模块表：' || r.table_name
      WHEN r.table_name LIKE 'PE_%' THEN '路径引擎模块表：' || r.table_name
      WHEN r.table_name LIKE 'RE_%' THEN '规则引擎模块表：' || r.table_name
      WHEN r.table_name LIKE 'TM_%' THEN '术语映射模块表：' || r.table_name
      WHEN r.table_name LIKE 'ADP_%' THEN '互联互通适配模块表：' || r.table_name
      WHEN r.table_name LIKE 'AI_%' THEN '人工智能治理模块表：' || r.table_name
      WHEN r.table_name LIKE 'CDSS_%' THEN '临床决策支持模块表：' || r.table_name
      WHEN r.table_name LIKE 'CLINICAL_%' THEN '临床安全模块表：' || r.table_name
      WHEN r.table_name LIKE 'MD_%' THEN '主数据治理模块表：' || r.table_name
      WHEN r.table_name LIKE 'DG_%' THEN '数据治理模块表：' || r.table_name
      WHEN r.table_name LIKE 'MPI_%' THEN '患者主索引模块表：' || r.table_name
      WHEN r.table_name LIKE 'NOTIFY_%' THEN '通知中心模块表：' || r.table_name
      WHEN r.table_name LIKE 'WF_%' THEN '流程待办模块表：' || r.table_name
      WHEN r.table_name LIKE 'OPS_%' THEN '运维任务模块表：' || r.table_name
      WHEN r.table_name LIKE 'SRC_%' THEN '来源追溯模块表：' || r.table_name
      WHEN r.table_name LIKE 'CFG_%' THEN '配置包管理模块表：' || r.table_name
      ELSE 'MedKernel业务数据表：' || r.table_name
    END;
    EXECUTE IMMEDIATE 'COMMENT ON TABLE "' || r.table_name || '" IS ''' || REPLACE(v_comment, '''', '''''') || '''';
  END LOOP;

  FOR c IN (SELECT table_name, column_name FROM user_col_comments WHERE table_name IN (SELECT table_name FROM user_tables) AND comments IS NULL) LOOP
    v_comment := CASE c.column_name
      WHEN 'ID' THEN '主键ID'
      WHEN 'TENANT_ID' THEN '租户ID'
      WHEN 'TENANT_CODE' THEN '租户编码'
      WHEN 'GROUP_CODE' THEN '集团编码'
      WHEN 'HOSPITAL_CODE' THEN '医院编码'
      WHEN 'CAMPUS_CODE' THEN '院区编码'
      WHEN 'SITE_CODE' THEN '站点编码'
      WHEN 'DEPARTMENT_CODE' THEN '科室编码'
      WHEN 'SCOPE_LEVEL' THEN '组织作用域层级'
      WHEN 'SCOPE_CODE' THEN '组织作用域编码'
      WHEN 'STATUS' THEN '业务状态'
      WHEN 'CREATED_BY' THEN '创建人'
      WHEN 'CREATED_TIME' THEN '创建时间'
      WHEN 'UPDATED_BY' THEN '更新人'
      WHEN 'UPDATED_TIME' THEN '更新时间'
      WHEN 'DELETED_FLAG' THEN '逻辑删除标识'
      WHEN 'TRACE_ID' THEN '调用链追踪ID'
      WHEN 'USER_ID' THEN '用户ID'
      WHEN 'USERNAME' THEN '用户名'
      WHEN 'PASSWORD_HASH' THEN '密码哈希'
      WHEN 'DISPLAY_NAME' THEN '显示名称'
      WHEN 'EMAIL' THEN '邮箱地址'
      WHEN 'PHONE' THEN '联系电话'
      WHEN 'ROLE_CODE' THEN '角色编码'
      WHEN 'ROLE_NAME' THEN '角色名称'
      WHEN 'PERMISSION_CODE' THEN '权限编码'
      WHEN 'EVENT_TYPE' THEN '事件类型'
      WHEN 'EVENT_RESULT' THEN '事件结果'
      WHEN 'IP_ADDRESS' THEN 'IP地址'
      WHEN 'USER_AGENT' THEN '客户端标识'
      WHEN 'RECORD_HASH' THEN '记录哈希值'
      WHEN 'CHAIN_HASH' THEN '审计链哈希值'
      WHEN 'CONFIG_JSON' THEN '配置JSON'
      WHEN 'METADATA_JSON' THEN '元数据JSON'
      WHEN 'DESCRIPTION' THEN '描述信息'
      ELSE c.table_name || '表字段：' || c.column_name
    END;
    EXECUTE IMMEDIATE 'COMMENT ON COLUMN "' || c.table_name || '"."' || c.column_name || '" IS ''' || REPLACE(v_comment, '''', '''''') || '''';
  END LOOP;
END;
/

DECLARE
  v_missing_tables NUMBER;
  v_missing_columns NUMBER;
BEGIN
  SELECT COUNT(*) INTO v_missing_tables
    FROM user_tab_comments
   WHERE table_name IN (SELECT table_name FROM user_tables)
     AND comments IS NULL;

  SELECT COUNT(*) INTO v_missing_columns
    FROM user_col_comments
   WHERE table_name IN (SELECT table_name FROM user_tables)
     AND comments IS NULL;

  IF v_missing_tables > 0 OR v_missing_columns > 0 THEN
    RAISE_APPLICATION_ERROR(-20001, '中文备注缺失：tables=' || v_missing_tables || ', columns=' || v_missing_columns);
  END IF;
END;
/

COMMIT;

PROMPT [8/8] 初始化结果校验...
COLUMN metric FORMAT A30;
COLUMN value FORMAT A80;
SELECT 'TABLE_COUNT' AS metric, TO_CHAR(COUNT(*)) AS value FROM user_tables
UNION ALL SELECT 'MISSING_TABLE_COMMENTS', TO_CHAR(COUNT(*)) FROM user_tab_comments WHERE table_name IN (SELECT table_name FROM user_tables) AND comments IS NULL
UNION ALL SELECT 'MISSING_COLUMN_COMMENTS', TO_CHAR(COUNT(*)) FROM user_col_comments WHERE table_name IN (SELECT table_name FROM user_tables) AND comments IS NULL
UNION ALL SELECT 'SEED_USERS', TO_CHAR(COUNT(*)) FROM sec_user
UNION ALL SELECT 'SEED_ROLES', TO_CHAR(COUNT(*)) FROM sec_role
UNION ALL SELECT 'SCHEMA_VERSION', MAX(version) FROM schema_version;

PROMPT INIT_OK
