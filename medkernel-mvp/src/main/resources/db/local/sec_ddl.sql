-- H2 local file database DDL for SEC user authentication
-- 用户体系最小可用：租户、用户、角色、权限、用户角色、用户组织范围
-- Oracle/DM DDL remains the production authority. This file mirrors the core
-- tables needed by the current persistence provider.
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

CREATE TABLE IF NOT EXISTS sec_tenant (
  id BIGINT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  contact_name VARCHAR(100),
  contact_phone VARCHAR(32),
  contact_email VARCHAR(200),
  license_type VARCHAR(32),
  max_users INTEGER DEFAULT 100 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_tenant UNIQUE (tenant_code)
);

CREATE TABLE IF NOT EXISTS sec_user (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(256) NOT NULL,
  display_name VARCHAR(100) NOT NULL,
  email VARCHAR(200),
  phone VARCHAR(32),
  avatar_url VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  last_login_time TIMESTAMP,
  last_login_ip VARCHAR(64),
  login_attempts INTEGER DEFAULT 0 NOT NULL,
  locked_until TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_user UNIQUE (tenant_id, username)
);

CREATE TABLE IF NOT EXISTS sec_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  role_type VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_role UNIQUE (tenant_id, role_code)
);

CREATE TABLE IF NOT EXISTS sec_permission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  permission_code VARCHAR(128) NOT NULL,
  permission_name VARCHAR(200) NOT NULL,
  permission_type VARCHAR(32) NOT NULL,
  resource_path VARCHAR(500),
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_permission UNIQUE (tenant_id, permission_code)
);

CREATE TABLE IF NOT EXISTS sec_user_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_role UNIQUE (tenant_id, user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sec_user_org_scope (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  scope_level VARCHAR(32) NOT NULL,
  scope_code VARCHAR(64) NOT NULL,
  scope_name VARCHAR(200),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_org_scope UNIQUE (tenant_id, user_id, scope_level, scope_code)
);

CREATE TABLE IF NOT EXISTS sec_role_permission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_role_permission UNIQUE (tenant_id, role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sec_auth_audit_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(64),
  event_type VARCHAR(32) NOT NULL,
  event_result VARCHAR(32) NOT NULL,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  failure_reason VARCHAR(500),
  trace_id VARCHAR(128),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 初始化演示租户
MERGE INTO sec_tenant (id, tenant_code, tenant_name, status, license_type, max_users, created_by)
KEY(tenant_code) VALUES (1, 'default', '默认租户', 'ACTIVE', 'ENTERPRISE', 1000, 'system');

-- 初始化演示角色（9 个角色）
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (101, 1, 'ADMIN', '系统管理员', 'PLATFORM', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (102, 1, 'DOCTOR', '医生', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (103, 1, 'QC_NURSE', '质控护士', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (104, 1, 'INSURANCE', '医保专员', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (105, 1, 'IT_ADMIN', '信息科管理员', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (106, 1, 'IMPLEMENTER', '实施工程师', 'PLATFORM', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (107, 1, 'HOSPITAL_LEADER', '院领导', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (108, 1, 'MEDICAL_EXPERT', '医学审核专家', 'HOSPITAL', 'ACTIVE', 'system');
MERGE INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) KEY(tenant_id, role_code) VALUES (109, 1, 'PLATFORM_OPS', '平台运营', 'PLATFORM', 'ACTIVE', 'system');

-- 初始化演示用户
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1001, 1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1002, 1, 'zhao01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵医生', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1003, 1, 'qian02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '钱护士', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1004, 1, 'sun03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙医保', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1005, 1, 'li04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李信息', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1006, 1, 'zhou05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '周实施', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1007, 1, 'wu06', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '吴院长', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1008, 1, 'zheng07', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑专家', 'ACTIVE', 'system');
MERGE INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) KEY(tenant_id, username) VALUES (1009, 1, 'wang08', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王运营', 'ACTIVE', 'system');

-- 用户角色关联
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2001, 1, 1001, 101, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2002, 1, 1002, 102, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2003, 1, 1003, 103, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2004, 1, 1004, 104, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2005, 1, 1005, 105, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2006, 1, 1006, 106, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2007, 1, 1007, 107, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2008, 1, 1008, 108, 'system');
MERGE INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) KEY(tenant_id, user_id, role_id) VALUES (2009, 1, 1009, 109, 'system');

-- 用户组织范围
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3001, 1, 1001, 'PLATFORM', 'DEFAULT', '全部', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3002, 1, 1002, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3003, 1, 1003, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3004, 1, 1004, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3005, 1, 1005, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3006, 1, 1006, 'PLATFORM', 'DEFAULT', '全部', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3007, 1, 1007, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3008, 1, 1008, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
MERGE INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) KEY(tenant_id, user_id, scope_level, scope_code) VALUES (3009, 1, 1009, 'PLATFORM', 'DEFAULT', '全部', 'system');
