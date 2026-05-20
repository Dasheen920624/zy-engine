-- SEC-007: 单点登录 SSO 接入 DDL (Oracle)
-- SSO 配置、SSO 会话、SSO 审计日志

-- 1. SSO 配置表：存储 CAS/OIDC/SAML/LDAP-AD 接入策略
CREATE TABLE sec_sso_config (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  config_code VARCHAR2(64) NOT NULL,
  config_name VARCHAR2(200) NOT NULL,
  protocol_type VARCHAR2(32) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  priority NUMBER(10) DEFAULT 100 NOT NULL,

  -- CAS 配置
  cas_server_url VARCHAR2(500),
  cas_service_url VARCHAR2(500),
  cas_callback_url VARCHAR2(500),

  -- OIDC 配置
  oidc_issuer VARCHAR2(500),
  oidc_client_id VARCHAR2(200),
  oidc_client_secret VARCHAR2(500),
  oidc_redirect_uri VARCHAR2(500),
  oidc_scope VARCHAR2(500),
  oidc_response_type VARCHAR2(64),
  oidc_jwks_uri VARCHAR2(500),

  -- SAML 配置
  saml_entity_id VARCHAR2(500),
  saml_sso_url VARCHAR2(500),
  saml_slo_url VARCHAR2(500),
  saml_certificate CLOB,
  saml_metadata_url VARCHAR2(500),

  -- LDAP-AD 配置
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

  -- 通用配置
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

-- 2. SSO 会话表：存储 SSO 登录会话信息
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
  updated_time TIMESTAMP
);

-- 3. SSO 审计日志表：记录 SSO 相关操作
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
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

-- 索引
CREATE INDEX idx_sso_config_tenant ON sec_sso_config(tenant_id, status);
CREATE INDEX idx_sso_session_user ON sec_sso_session(tenant_id, user_id, status);
CREATE INDEX idx_sso_session_token ON sec_sso_session(session_token);
CREATE INDEX idx_sso_audit_tenant ON sec_sso_audit_log(tenant_id, created_time);
CREATE INDEX idx_sso_audit_user ON sec_sso_audit_log(tenant_id, user_id, created_time);

-- 种子数据：SSO 配置样例（默认禁用）
INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  cas_server_url, cas_service_url, cas_callback_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5001, 1, 'CAS_DEFAULT', 'CAS 默认配置', 'CAS', 'DISABLED', 100,
  'https://cas.example.com/cas', 'https://medkernel.example.com', 'https://medkernel.example.com/api/sso/cas/callback',
  0, 480, 'system');

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  oidc_issuer, oidc_client_id, oidc_redirect_uri, oidc_scope, oidc_response_type, auto_create_user, session_timeout_minutes, created_by)
VALUES (5002, 1, 'OIDC_DEFAULT', 'OIDC 默认配置', 'OIDC', 'DISABLED', 200,
  'https://keycloak.example.com/realms/medkernel', 'medkernel-client', 'https://medkernel.example.com/api/sso/oidc/callback',
  'openid profile email', 'code', 0, 480, 'system');

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  saml_entity_id, saml_sso_url, saml_slo_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5003, 1, 'SAML_DEFAULT', 'SAML 默认配置', 'SAML', 'DISABLED', 300,
  'https://medkernel.example.com/saml/metadata', 'https://idp.example.com/sso/saml', 'https://idp.example.com/slo/saml',
  0, 480, 'system');

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  ldap_url, ldap_base_dn, ldap_user_search_base, ldap_user_search_filter, ldap_use_ssl, auto_create_user, session_timeout_minutes, created_by)
VALUES (5004, 1, 'LDAP_DEFAULT', 'LDAP-AD 默认配置', 'LDAP-AD', 'DISABLED', 400,
  'ldap://ldap.example.com:389', 'dc=example,dc=com', 'ou=users,dc=example,dc=com', '(sAMAccountName={0})', 0,
  0, 480, 'system');

COMMIT;
