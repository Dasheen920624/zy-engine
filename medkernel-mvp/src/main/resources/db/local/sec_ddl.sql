-- H2 local file database DDL for SEC user authentication
-- 用户体系最小可用：租户、用户、角色、权限、用户角色、用户组织范围
-- Oracle/DM DDL remains the production authority. This file mirrors the core
-- tables needed by the current persistence provider.

-- 租户表：多租户隔离的基础
CREATE TABLE IF NOT EXISTS sec_tenant (
  id NUMBER(20) PRIMARY KEY,
  tenant_code VARCHAR2(64) NOT NULL,     -- 租户编码，唯一标识
  tenant_name VARCHAR2(200) NOT NULL,    -- 租户名称
  status VARCHAR2(32) NOT NULL,          -- 状态：ACTIVE/DISABLED
  contact_name VARCHAR2(100),
  contact_phone VARCHAR2(32),
  contact_email VARCHAR2(200),
  license_type VARCHAR2(32),             -- 授权类型：TRIAL/STANDARD/ENTERPRISE
  max_users NUMBER(10) DEFAULT 100 NOT NULL, -- 最大用户数
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_tenant UNIQUE (tenant_code)
);

-- 用户表：平台独立用户，不依赖院内身份源
CREATE TABLE IF NOT EXISTS sec_user (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,         -- 所属租户ID
  username VARCHAR2(64) NOT NULL,        -- 用户名，租户内唯一
  password_hash VARCHAR2(256) NOT NULL,  -- 密码哈希（bcrypt + pepper）
  display_name VARCHAR2(100) NOT NULL,   -- 显示名称
  email VARCHAR2(200),
  phone VARCHAR2(32),
  avatar_url VARCHAR2(500),
  status VARCHAR2(32) NOT NULL,          -- 状态：ACTIVE/DISABLED/LOCKED
  last_login_time TIMESTAMP,
  last_login_ip VARCHAR2(64),
  login_attempts NUMBER(10) DEFAULT 0 NOT NULL, -- 连续登录失败次数
  locked_until TIMESTAMP,                -- 锁定截止时间
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_user UNIQUE (tenant_id, username)
);

-- 角色表：预置和自定义角色
CREATE TABLE IF NOT EXISTS sec_role (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  role_code VARCHAR2(64) NOT NULL,       -- 角色编码
  role_name VARCHAR2(100) NOT NULL,      -- 角色名称
  role_type VARCHAR2(32) NOT NULL,       -- 角色类型：SYSTEM/PLATFORM/HOSPITAL/DEPARTMENT
  description VARCHAR2(500),
  status VARCHAR2(32) NOT NULL,          -- 状态：ACTIVE/DISABLED
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_role UNIQUE (tenant_id, role_code)
);

-- 权限表：菜单、按钮、API 权限
CREATE TABLE IF NOT EXISTS sec_permission (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  permission_code VARCHAR2(128) NOT NULL, -- 权限编码
  permission_name VARCHAR2(200) NOT NULL,
  permission_type VARCHAR2(32) NOT NULL,  -- 权限类型：MENU/BUTTON/API
  resource_path VARCHAR2(500),            -- 资源路径（菜单路径或 API 路径）
  description VARCHAR2(500),
  status VARCHAR2(32) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_permission UNIQUE (tenant_id, permission_code)
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sec_user_role (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  role_id NUMBER(20) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_role UNIQUE (tenant_id, user_id, role_id)
);

-- 用户组织范围表：限定用户可访问的组织范围
CREATE TABLE IF NOT EXISTS sec_user_org_scope (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  user_id NUMBER(20) NOT NULL,
  scope_level VARCHAR2(32) NOT NULL,     -- 组织层级：PLATFORM/GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT
  scope_code VARCHAR2(64) NOT NULL,      -- 组织编码
  scope_name VARCHAR2(200),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_org_scope UNIQUE (tenant_id, user_id, scope_level, scope_code)
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sec_role_permission (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20) NOT NULL,
  role_id NUMBER(20) NOT NULL,
  permission_id NUMBER(20) NOT NULL,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_role_permission UNIQUE (tenant_id, role_id, permission_id)
);

-- 认证审计日志表：登录、登出、登录失败
CREATE TABLE IF NOT EXISTS sec_auth_audit_log (
  id NUMBER(20) PRIMARY KEY,
  tenant_id NUMBER(20),
  user_id NUMBER(20),
  username VARCHAR2(64),
  event_type VARCHAR2(32) NOT NULL,      -- 事件类型：LOGIN/LOGOUT/LOGIN_FAILED
  event_result VARCHAR2(32) NOT NULL,    -- 事件结果：SUCCESS/FAILURE
  ip_address VARCHAR2(64),
  user_agent VARCHAR2(500),
  failure_reason VARCHAR2(500),
  trace_id VARCHAR2(128),
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
