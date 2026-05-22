-- SEC-007: 单点登录 SSO 接入 DDL
-- SSO 配置、SSO 会话、SSO 审计日志

-- 1. SSO 配置表：存储 CAS/OIDC/SAML/LDAP-AD 接入策略
CREATE TABLE IF NOT EXISTS sec_sso_config (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  config_code VARCHAR(64) NOT NULL,           -- 配置编码（如 CAS_CONFIG, OIDC_CONFIG）
  config_name VARCHAR(200) NOT NULL,          -- 配置名称
  protocol_type VARCHAR(32) NOT NULL,         -- CAS/OIDC/SAML/LDAP-AD
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/DISABLED
  priority INTEGER DEFAULT 100 NOT NULL,      -- 优先级，数字越小优先级越高
  
  -- CAS 配置
  cas_server_url VARCHAR(500),                -- CAS 服务器地址
  cas_service_url VARCHAR(500),               -- CAS 服务地址
  cas_callback_url VARCHAR(500),              -- CAS 回调地址
  
  -- OIDC 配置
  oidc_issuer VARCHAR(500),                   -- OIDC Issuer
  oidc_client_id VARCHAR(200),                -- OIDC Client ID
  oidc_client_secret VARCHAR(500),            -- OIDC Client Secret
  oidc_redirect_uri VARCHAR(500),             -- OIDC Redirect URI
  oidc_scope VARCHAR(500),                    -- OIDC Scope
  oidc_response_type VARCHAR(64),             -- OIDC Response Type
  oidc_jwks_uri VARCHAR(500),                 -- OIDC JWKS URI
  
  -- SAML 配置
  saml_entity_id VARCHAR(500),                -- SAML Entity ID
  saml_sso_url VARCHAR(500),                  -- SAML SSO URL
  saml_slo_url VARCHAR(500),                  -- SAML SLO URL
  saml_certificate CLOB,                      -- SAML 证书
  saml_metadata_url VARCHAR(500),             -- SAML Metadata URL
  
  -- LDAP-AD 配置
  ldap_url VARCHAR(500),                      -- LDAP 服务器地址
  ldap_base_dn VARCHAR(500),                  -- LDAP Base DN
  ldap_bind_dn VARCHAR(500),                  -- LDAP Bind DN
  ldap_bind_password VARCHAR(500),            -- LDAP Bind Password
  ldap_user_search_base VARCHAR(500),         -- LDAP 用户搜索基础
  ldap_user_search_filter VARCHAR(500),       -- LDAP 用户搜索过滤器
  ldap_group_search_base VARCHAR(500),        -- LDAP 组搜索基础
  ldap_group_search_filter VARCHAR(500),      -- LDAP 组搜索过滤器
  ldap_use_ssl BOOLEAN DEFAULT FALSE,         -- 是否使用 SSL
  ldap_use_starttls BOOLEAN DEFAULT FALSE,    -- 是否使用 STARTTLS
  
  -- 通用配置
  attribute_mapping CLOB,                     -- 属性映射（JSON）
  role_mapping CLOB,                          -- 角色映射（JSON）
  auto_create_user BOOLEAN DEFAULT FALSE,     -- 是否自动创建用户
  auto_update_user BOOLEAN DEFAULT TRUE,      -- 是否自动更新用户信息
  session_timeout_minutes INTEGER DEFAULT 480, -- 会话超时时间（分钟）
  
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
  user_id BIGINT NOT NULL,                    -- 平台用户 ID
  config_id BIGINT NOT NULL,                  -- SSO 配置 ID
  external_subject VARCHAR(200),              -- 外部用户标识
  external_name VARCHAR(200),                 -- 外部用户姓名
  external_email VARCHAR(200),                -- 外部用户邮箱
  session_token VARCHAR(500),                 -- 会话令牌
  access_token VARCHAR(1000),                 -- 访问令牌（OIDC/SAML）
  refresh_token VARCHAR(1000),                -- 刷新令牌（OIDC）
  id_token CLOB,                              -- ID 令牌（OIDC）
  token_type VARCHAR(32),                     -- 令牌类型
  issued_at TIMESTAMP,                        -- 令牌签发时间
  expires_at TIMESTAMP,                       -- 令牌过期时间
  refresh_expires_at TIMESTAMP,               -- 刷新令牌过期时间
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/EXPIRED/REVOKED
  ip_address VARCHAR(64),                     -- 登录 IP
  user_agent VARCHAR(500),                    -- 用户代理
  last_access_time TIMESTAMP,                 -- 最后访问时间
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_sso_session UNIQUE (id)
);

-- 3. SSO 审计日志表：记录 SSO 相关操作
CREATE TABLE IF NOT EXISTS sec_sso_audit_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT,                             -- 平台用户 ID
  config_id BIGINT,                           -- SSO 配置 ID
  event_type VARCHAR(64) NOT NULL,            -- LOGIN/LOGOUT/SESSION_EXPIRED/CONFIG_CHANGE/ERROR
  event_result VARCHAR(32) NOT NULL,          -- SUCCESS/FAILURE
  external_subject VARCHAR(200),              -- 外部用户标识
  error_code VARCHAR(64),                     -- 错误码
  error_message VARCHAR(1000),                -- 错误消息
  ip_address VARCHAR(64),                     -- IP 地址
  user_agent VARCHAR(500),                    -- 用户代理
  trace_id VARCHAR(128),                      -- 链路追踪 ID
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_sso_config_tenant ON sec_sso_config(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sso_session_user ON sec_sso_session(tenant_id, user_id, status);
CREATE INDEX IF NOT EXISTS idx_sso_session_token ON sec_sso_session(session_token);
CREATE INDEX IF NOT EXISTS idx_sso_audit_tenant ON sec_sso_audit_log(tenant_id, created_time);
CREATE INDEX IF NOT EXISTS idx_sso_audit_user ON sec_sso_audit_log(tenant_id, user_id, created_time);

-- 种子数据：SSO 配置样例（默认禁用）
MERGE INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority, 
  cas_server_url, cas_service_url, cas_callback_url, auto_create_user, session_timeout_minutes, created_by)
KEY(tenant_id, config_code) VALUES (5001, 1, 'CAS_DEFAULT', 'CAS 默认配置', 'CAS', 'DISABLED', 100,
  'https://cas.example.com/cas', 'https://medkernel.example.com', 'https://medkernel.example.com/api/sso/cas/callback',
  FALSE, 480, 'system');

MERGE INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  oidc_issuer, oidc_client_id, oidc_redirect_uri, oidc_scope, oidc_response_type, auto_create_user, session_timeout_minutes, created_by)
KEY(tenant_id, config_code) VALUES (5002, 1, 'OIDC_DEFAULT', 'OIDC 默认配置', 'OIDC', 'DISABLED', 200,
  'https://keycloak.example.com/realms/medkernel', 'medkernel-client', 'https://medkernel.example.com/api/sso/oidc/callback',
  'openid profile email', 'code', FALSE, 480, 'system');

MERGE INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  saml_entity_id, saml_sso_url, saml_slo_url, auto_create_user, session_timeout_minutes, created_by)
KEY(tenant_id, config_code) VALUES (5003, 1, 'SAML_DEFAULT', 'SAML 默认配置', 'SAML', 'DISABLED', 300,
  'https://medkernel.example.com/saml/metadata', 'https://idp.example.com/sso/saml', 'https://idp.example.com/slo/saml',
  FALSE, 480, 'system');

MERGE INTO sec_sso_config (id, tenant_id, config_code, config_name, protocol_type, status, priority,
  ldap_url, ldap_base_dn, ldap_user_search_base, ldap_user_search_filter, ldap_use_ssl, auto_create_user, session_timeout_minutes, created_by)
KEY(tenant_id, config_code) VALUES (5004, 1, 'LDAP_DEFAULT', 'LDAP-AD 默认配置', 'LDAP-AD', 'DISABLED', 400,
  'ldap://ldap.example.com:389', 'dc=example,dc=com', 'ou=users,dc=example,dc=com', '(sAMAccountName={0})', FALSE,
  FALSE, 480, 'system');