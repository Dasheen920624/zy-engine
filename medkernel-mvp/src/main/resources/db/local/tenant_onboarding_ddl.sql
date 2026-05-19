-- 客户租户开通 DDL (H2 本地开发库)
-- SEC-011: 外网通用服务账号和客户租户开通
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

-- 客户租户申请表
CREATE TABLE IF NOT EXISTS sec_tenant_application (
  id BIGINT PRIMARY KEY,
  application_code VARCHAR(64) NOT NULL,
  company_name VARCHAR(200) NOT NULL,
  company_type VARCHAR(64),  -- HOSPITAL/CLINIC/PHARMA/INSURANCE/OTHER
  contact_name VARCHAR(100) NOT NULL,
  contact_phone VARCHAR(32) NOT NULL,
  contact_email VARCHAR(200) NOT NULL,
  contact_title VARCHAR(100),
  province VARCHAR(64),
  city VARCHAR(64),
  address VARCHAR(500),
  license_number VARCHAR(100),  -- 医疗机构执业许可证号
  license_type VARCHAR(64),     -- 试用/标准/企业
  expected_users INTEGER,
  business_needs CLOB,
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,  -- PENDING/APPROVED/REJECTED/ACTIVATED
  reviewed_by VARCHAR(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR(1000),
  tenant_id VARCHAR(64),  -- 审批通过后生成的租户ID
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_tenant_application UNIQUE (application_code)
);

-- 客户管理员邀请表
CREATE TABLE IF NOT EXISTS sec_admin_invitation (
  id BIGINT PRIMARY KEY,
  invitation_code VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  email VARCHAR(200) NOT NULL,
  phone VARCHAR(32),
  invited_by VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) DEFAULT 'TENANT_ADMIN' NOT NULL,
  status VARCHAR(32) DEFAULT 'PENDING' NOT NULL,  -- PENDING/ACCEPTED/EXPIRED/CANCELLED
  expire_time TIMESTAMP NOT NULL,
  accepted_time TIMESTAMP,
  user_id VARCHAR(64),  -- 接受邀请后创建的用户ID
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_admin_invitation UNIQUE (invitation_code)
);

-- 服务账号表
CREATE TABLE IF NOT EXISTS sec_service_account (
  id BIGINT PRIMARY KEY,
  account_code VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  account_name VARCHAR(100) NOT NULL,
  account_type VARCHAR(32) DEFAULT 'API_CLIENT' NOT NULL,  -- API_CLIENT/SERVICE_USER/WEBHOOK
  client_id VARCHAR(64) NOT NULL,
  client_secret_hash VARCHAR(200) NOT NULL,
  scopes CLOB,  -- JSON array of allowed scopes
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/SUSPENDED/REVOKED
  expire_time TIMESTAMP,
  last_used_time TIMESTAMP,
  last_used_ip VARCHAR(64),
  usage_count BIGINT DEFAULT 0 NOT NULL,
  rate_limit INTEGER DEFAULT 1000 NOT NULL,  -- 每分钟请求限制
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_service_account UNIQUE (tenant_id, account_code),
  CONSTRAINT uk_service_client_id UNIQUE (client_id)
);

-- 服务账号访问日志表
CREATE TABLE IF NOT EXISTS sec_service_access_log (
  id BIGINT PRIMARY KEY,
  client_id VARCHAR(64) NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  request_path VARCHAR(500),
  request_method VARCHAR(32),
  response_status INTEGER,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 平台运营授权表
CREATE TABLE IF NOT EXISTS sec_platform_authorization (
  id BIGINT PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  auth_type VARCHAR(32) NOT NULL,  -- FEATURE/API/MODULE
  auth_code VARCHAR(64) NOT NULL,
  auth_name VARCHAR(200) NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/EXPIRED/REVOKED
  effective_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expire_time TIMESTAMP,
  quota_limit BIGINT,  -- 配额限制
  quota_used BIGINT DEFAULT 0 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_platform_auth UNIQUE (tenant_id, auth_code)
);

-- MFA 配置表
CREATE TABLE IF NOT EXISTS sec_mfa_config (
  id BIGINT PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  mfa_type VARCHAR(32) NOT NULL,  -- TOTP/SMS/EMAIL
  secret_key VARCHAR(200),
  phone VARCHAR(32),
  email VARCHAR(200),
  enabled SMALLINT DEFAULT 0 NOT NULL,
  backup_codes CLOB,  -- JSON array of backup codes
  last_used_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mfa_config UNIQUE (user_id, mfa_type)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_tenant_app_status ON sec_tenant_application(status);
CREATE INDEX IF NOT EXISTS idx_tenant_app_company ON sec_tenant_application(company_name);
CREATE INDEX IF NOT EXISTS idx_admin_invitation_tenant ON sec_admin_invitation(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_admin_invitation_email ON sec_admin_invitation(email);
CREATE INDEX IF NOT EXISTS idx_service_account_tenant ON sec_service_account(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_service_account_client ON sec_service_account(client_id);
CREATE INDEX IF NOT EXISTS idx_service_access_client ON sec_service_access_log(client_id, created_time);
CREATE INDEX IF NOT EXISTS idx_platform_auth_tenant ON sec_platform_authorization(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mfa_config_user ON sec_mfa_config(user_id, enabled);
