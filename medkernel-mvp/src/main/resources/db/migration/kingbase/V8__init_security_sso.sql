-- PR-FINAL-25: KingbaseES V8 兼容 PostgreSQL 8.x 协议，本目录从 db/migration/postgres/ 复制。
-- 如发现 KingbaseES 实测差异，在此独立覆盖；正常情况两套保持同步。
-- SEC-007: 单点登录 SSO 接入 DDL (PostgreSQL)
-- SSO 配置、SSO 会话、SSO 审计日志

-- 1. SSO 配置表：存储 CAS/OIDC/SAML/LDAP-AD 接入策略
CREATE TABLE IF NOT EXISTS sec_sso_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_code VARCHAR(64) NOT NULL,
  config_name VARCHAR(200) NOT NULL,
  protocol_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  priority INTEGER DEFAULT 100 NOT NULL,

  -- CAS 配置
  cas_server_url VARCHAR(500),
  cas_service_url VARCHAR(500),
  cas_callback_url VARCHAR(500),

  -- OIDC 配置
  oidc_issuer VARCHAR(500),
  oidc_client_id VARCHAR(200),
  oidc_client_secret VARCHAR(500),
  oidc_redirect_uri VARCHAR(500),
  oidc_scope VARCHAR(500),
  oidc_response_type VARCHAR(64),
  oidc_jwks_uri VARCHAR(500),

  -- SAML 配置
  saml_entity_id VARCHAR(500),
  saml_sso_url VARCHAR(500),
  saml_slo_url VARCHAR(500),
  saml_certificate TEXT,
  saml_metadata_url VARCHAR(500),

  -- LDAP-AD 配置
  ldap_url VARCHAR(500),
  ldap_base_dn VARCHAR(500),
  ldap_bind_dn VARCHAR(500),
  ldap_bind_password VARCHAR(500),
  ldap_user_search_base VARCHAR(500),
  ldap_user_search_filter VARCHAR(500),
  ldap_group_search_base VARCHAR(500),
  ldap_group_search_filter VARCHAR(500),
  ldap_use_ssl BOOLEAN DEFAULT FALSE,
  ldap_use_starttls BOOLEAN DEFAULT FALSE,

  -- 通用配置
  attribute_mapping TEXT,
  role_mapping TEXT,
  auto_create_user BOOLEAN DEFAULT FALSE,
  auto_update_user BOOLEAN DEFAULT TRUE,
  session_timeout_minutes INTEGER DEFAULT 480,

  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_sso_config UNIQUE (tenant_id, config_code)
);

-- 2. SSO 会话表：存储 SSO 登录会话信息
CREATE TABLE IF NOT EXISTS sec_sso_session (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  config_id BIGINT NOT NULL,
  external_subject VARCHAR(200),
  external_name VARCHAR(200),
  external_email VARCHAR(200),
  session_token VARCHAR(500),
  access_token VARCHAR(1000),
  refresh_token VARCHAR(1000),
  id_token TEXT,
  token_type VARCHAR(32),
  issued_at TIMESTAMP,
  expires_at TIMESTAMP,
  refresh_expires_at TIMESTAMP,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  last_access_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP
);

-- 3. SSO 审计日志表：记录 SSO 相关操作
CREATE TABLE IF NOT EXISTS sec_sso_audit_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT,
  config_id BIGINT,
  event_type VARCHAR(64) NOT NULL,
  event_result VARCHAR(32) NOT NULL,
  external_subject VARCHAR(200),
  error_code VARCHAR(64),
  error_message VARCHAR(1000),
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  trace_id VARCHAR(128),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sso_config_tenant ON sec_sso_config(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sso_session_user ON sec_sso_session(tenant_id, user_id, status);
CREATE INDEX IF NOT EXISTS idx_sso_session_token ON sec_sso_session(session_token);
CREATE INDEX IF NOT EXISTS idx_sso_audit_tenant ON sec_sso_audit_log(tenant_id, created_time);
CREATE INDEX IF NOT EXISTS idx_sso_audit_user ON sec_sso_audit_log(tenant_id, user_id, created_time);

-- 种子数据：SSO 配置样例（默认禁用）
INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  cas_server_url, cas_service_url, cas_callback_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5001, 1, 'CAS_DEFAULT', 'CAS 默认配置', 'CAS', 'DISABLED', 100,
  'https://cas.example.com/cas', 'https://medkernel.example.com', 'https://medkernel.example.com/api/sso/cas/callback',
  FALSE, 480, 'system')
ON CONFLICT (tenant_id, config_code) DO NOTHING;

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  oidc_issuer, oidc_client_id, oidc_redirect_uri, oidc_scope, oidc_response_type, auto_create_user, session_timeout_minutes, created_by)
VALUES (5002, 1, 'OIDC_DEFAULT', 'OIDC 默认配置', 'OIDC', 'DISABLED', 200,
  'https://keycloak.example.com/realms/medkernel', 'medkernel-client', 'https://medkernel.example.com/api/sso/oidc/callback',
  'openid profile email', 'code', FALSE, 480, 'system')
ON CONFLICT (tenant_id, config_code) DO NOTHING;

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  saml_entity_id, saml_sso_url, saml_slo_url, auto_create_user, session_timeout_minutes, created_by)
VALUES (5003, 1, 'SAML_DEFAULT', 'SAML 默认配置', 'SAML', 'DISABLED', 300,
  'https://medkernel.example.com/saml/metadata', 'https://idp.example.com/sso/saml', 'https://idp.example.com/slo/saml',
  FALSE, 480, 'system')
ON CONFLICT (tenant_id, config_code) DO NOTHING;

INSERT INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  ldap_url, ldap_base_dn, ldap_user_search_base, ldap_user_search_filter, ldap_use_ssl, auto_create_user, session_timeout_minutes, created_by)
VALUES (5004, 1, 'LDAP_DEFAULT', 'LDAP-AD 默认配置', 'LDAP-AD', 'DISABLED', 400,
  'ldap://ldap.example.com:389', 'dc=example,dc=com', 'ou=users,dc=example,dc=com', '(sAMAccountName={0})', FALSE,
  FALSE, 480, 'system')
ON CONFLICT (tenant_id, config_code) DO NOTHING;
