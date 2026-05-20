-- 客户租户开通 DDL (H2 本地开发库)
-- SEC-011: 外网通用服务账号和客户租户开通

-- 客户租户申请表
CREATE TABLE IF NOT EXISTS SEC_TENANT_APPLICATION (
  id NUMBER(20) PRIMARY KEY,
  application_code VARCHAR2(64) NOT NULL,
  company_name VARCHAR2(200) NOT NULL,
  company_type VARCHAR2(64),  -- HOSPITAL/CLINIC/PHARMA/INSURANCE/OTHER
  contact_name VARCHAR2(100) NOT NULL,
  contact_phone VARCHAR2(32) NOT NULL,
  contact_email VARCHAR2(200) NOT NULL,
  contact_title VARCHAR2(100),
  province VARCHAR2(64),
  city VARCHAR2(64),
  address VARCHAR2(500),
  license_number VARCHAR2(100),  -- 医疗机构执业许可证号
  license_type VARCHAR2(64),     -- 试用/标准/企业
  expected_users NUMBER(10),
  business_needs CLOB,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,  -- PENDING/APPROVED/REJECTED/ACTIVATED
  reviewed_by VARCHAR2(64),
  reviewed_time TIMESTAMP,
  review_comment VARCHAR2(1000),
  tenant_id VARCHAR2(64),  -- 审批通过后生成的租户ID
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_tenant_application UNIQUE (application_code)
);

-- 客户管理员邀请表
CREATE TABLE IF NOT EXISTS SEC_ADMIN_INVITATION (
  id NUMBER(20) PRIMARY KEY,
  invitation_code VARCHAR2(64) NOT NULL,
  tenant_id VARCHAR2(64) NOT NULL,
  email VARCHAR2(200) NOT NULL,
  phone VARCHAR2(32),
  invited_by VARCHAR2(64) NOT NULL,
  role_code VARCHAR2(64) DEFAULT 'TENANT_ADMIN' NOT NULL,
  status VARCHAR2(32) DEFAULT 'PENDING' NOT NULL,  -- PENDING/ACCEPTED/EXPIRED/CANCELLED
  expire_time TIMESTAMP NOT NULL,
  accepted_time TIMESTAMP,
  user_id VARCHAR2(64),  -- 接受邀请后创建的用户ID
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_admin_invitation UNIQUE (invitation_code)
);

-- 服务账号表
CREATE TABLE IF NOT EXISTS SEC_SERVICE_ACCOUNT (
  id NUMBER(20) PRIMARY KEY,
  account_code VARCHAR2(64) NOT NULL,
  tenant_id VARCHAR2(64) NOT NULL,
  account_name VARCHAR2(100) NOT NULL,
  account_type VARCHAR2(32) DEFAULT 'API_CLIENT' NOT NULL,  -- API_CLIENT/SERVICE_USER/WEBHOOK
  client_id VARCHAR2(64) NOT NULL,
  client_secret_hash VARCHAR2(200) NOT NULL,
  scopes CLOB,  -- JSON array of allowed scopes
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/SUSPENDED/REVOKED
  expire_time TIMESTAMP,
  last_used_time TIMESTAMP,
  last_used_ip VARCHAR2(64),
  usage_count NUMBER(20) DEFAULT 0 NOT NULL,
  rate_limit NUMBER(10) DEFAULT 1000 NOT NULL,  -- 每分钟请求限制
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_service_account UNIQUE (tenant_id, account_code),
  CONSTRAINT uk_service_client_id UNIQUE (client_id)
);

-- 服务账号访问日志表
CREATE TABLE IF NOT EXISTS SEC_SERVICE_ACCESS_LOG (
  id NUMBER(20) PRIMARY KEY,
  client_id VARCHAR2(64) NOT NULL,
  tenant_id VARCHAR2(64) NOT NULL,
  request_path VARCHAR2(500),
  request_method VARCHAR2(32),
  response_status NUMBER(10),
  ip_address VARCHAR2(64),
  user_agent VARCHAR2(500),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 平台运营授权表
CREATE TABLE IF NOT EXISTS SEC_PLATFORM_AUTHORIZATION (
  id NUMBER(20) PRIMARY KEY,
  tenant_id VARCHAR2(64) NOT NULL,
  auth_type VARCHAR2(32) NOT NULL,  -- FEATURE/API/MODULE
  auth_code VARCHAR2(64) NOT NULL,
  auth_name VARCHAR2(200) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/EXPIRED/REVOKED
  effective_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expire_time TIMESTAMP,
  quota_limit NUMBER(20),  -- 配额限制
  quota_used NUMBER(20) DEFAULT 0 NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_platform_auth UNIQUE (tenant_id, auth_code)
);

-- MFA 配置表
CREATE TABLE IF NOT EXISTS SEC_MFA_CONFIG (
  id NUMBER(20) PRIMARY KEY,
  user_id VARCHAR2(64) NOT NULL,
  mfa_type VARCHAR2(32) NOT NULL,  -- TOTP/SMS/EMAIL
  secret_key VARCHAR2(200),
  phone VARCHAR2(32),
  email VARCHAR2(200),
  enabled NUMBER(1) DEFAULT 0 NOT NULL,
  backup_codes CLOB,  -- JSON array of backup codes
  last_used_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT uk_mfa_config UNIQUE (user_id, mfa_type)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_tenant_app_status ON SEC_TENANT_APPLICATION(status);
CREATE INDEX IF NOT EXISTS idx_tenant_app_company ON SEC_TENANT_APPLICATION(company_name);
CREATE INDEX IF NOT EXISTS idx_admin_invitation_tenant ON SEC_ADMIN_INVITATION(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_admin_invitation_email ON SEC_ADMIN_INVITATION(email);
CREATE INDEX IF NOT EXISTS idx_service_account_tenant ON SEC_SERVICE_ACCOUNT(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_service_account_client ON SEC_SERVICE_ACCOUNT(client_id);
CREATE INDEX IF NOT EXISTS idx_service_access_client ON SEC_SERVICE_ACCESS_LOG(client_id, created_time DESC);
CREATE INDEX IF NOT EXISTS idx_platform_auth_tenant ON SEC_PLATFORM_AUTHORIZATION(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_mfa_config_user ON SEC_MFA_CONFIG(user_id, enabled);
