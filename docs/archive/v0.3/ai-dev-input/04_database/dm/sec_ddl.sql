-- 达梦 SEC DDL for user authentication and authorization
-- 用户体系最小可用：租户、用户、角色、权限、用户角色、用户组织范围
-- 达梦语法整体接近Oracle，避免使用复杂数据库特性

-- 租户表：多租户隔离的基础
CREATE TABLE sec_tenant (
  id BIGINT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  contact_name VARCHAR(100),
  contact_phone VARCHAR(32),
  contact_email VARCHAR(200),
  license_type VARCHAR(32),
  max_users INT DEFAULT 100 NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_tenant UNIQUE (tenant_code)
);

-- 用户表：平台独立用户，不依赖院内身份源
CREATE TABLE sec_user (
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
  login_attempts INT DEFAULT 0 NOT NULL,
  locked_until TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_user UNIQUE (tenant_id, username)
);

-- 角色表：预置和自定义角色
CREATE TABLE sec_role (
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

-- 权限表：菜单、按钮、API 权限
CREATE TABLE sec_permission (
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

-- 用户角色关联表
CREATE TABLE sec_user_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_user_role UNIQUE (tenant_id, user_id, role_id)
);

-- 用户组织范围表：限定用户可访问的组织范围
CREATE TABLE sec_user_org_scope (
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

-- 角色权限关联表
CREATE TABLE sec_role_permission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_sec_role_permission UNIQUE (tenant_id, role_id, permission_id)
);

-- 审计日志表：登录、登出、登录失败
CREATE TABLE sec_auth_audit_log (
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
INSERT INTO sec_tenant (id, tenant_code, tenant_name, status, license_type, max_users, created_by)
VALUES (1, 'default', '默认租户', 'ACTIVE', 'ENTERPRISE', 1000, 'system');

-- 初始化演示角色（9 个角色）
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (101, 1, 'ADMIN', '系统管理员', 'PLATFORM', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (102, 1, 'DOCTOR', '医生', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (103, 1, 'QC_NURSE', '质控护士', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (104, 1, 'INSURANCE', '医保专员', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (105, 1, 'IT_ADMIN', '信息科管理员', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (106, 1, 'IMPLEMENTER', '实施工程师', 'PLATFORM', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (107, 1, 'HOSPITAL_LEADER', '院领导', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (108, 1, 'MEDICAL_EXPERT', '医学审核专家', 'HOSPITAL', 'ACTIVE', 'system');
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (109, 1, 'PLATFORM_OPS', '平台运营', 'PLATFORM', 'ACTIVE', 'system');

-- 初始化演示用户（9 个用户，密码均为 demo123，bcrypt hash）
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1001, 1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1002, 1, 'zhao01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵医生', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1003, 1, 'qian02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '钱护士', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1004, 1, 'sun03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙医保', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1005, 1, 'li04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李信息', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1006, 1, 'zhou05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '周实施', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1007, 1, 'wu06', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '吴院长', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1008, 1, 'zheng07', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑专家', 'ACTIVE', 'system');
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1009, 1, 'wang08', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王运营', 'ACTIVE', 'system');

-- 用户角色关联
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2001, 1, 1001, 101, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2002, 1, 1002, 102, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2003, 1, 1003, 103, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2004, 1, 1004, 104, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2005, 1, 1005, 105, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2006, 1, 1006, 106, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2007, 1, 1007, 107, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2008, 1, 1008, 108, 'system');
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2009, 1, 1009, 109, 'system');

-- 用户组织范围
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3001, 1, 1001, 'PLATFORM', 'DEFAULT', '全部', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3002, 1, 1002, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3003, 1, 1003, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3004, 1, 1004, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3005, 1, 1005, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3006, 1, 1006, 'PLATFORM', 'DEFAULT', '全部', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3007, 1, 1007, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3008, 1, 1008, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system');
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3009, 1, 1009, 'PLATFORM', 'DEFAULT', '全部', 'system');

-- ============================================================
-- SEC-006: 院内用户体系同步 (Identity Provider & Sync)
-- ============================================================

-- 身份源配置表：注册院内 HIS/EMR/OA/LDAP 等外部身份源
CREATE TABLE sec_identity_provider (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  provider_name VARCHAR(200) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,
  adapter_code VARCHAR(64),
  query_code VARCHAR(64),
  priority INT DEFAULT 100 NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  config_json TEXT,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_provider UNIQUE (tenant_id, provider_code)
);

-- 外部身份绑定表：将平台用户绑定到外部系统身份
CREATE TABLE sec_identity_binding (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  external_subject VARCHAR(200) NOT NULL,
  external_name VARCHAR(200),
  external_org_code VARCHAR(64),
  external_org_name VARCHAR(200),
  external_position VARCHAR(200),
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  last_sync_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_binding UNIQUE (tenant_id, provider_id, external_subject)
);

-- 注：旧 sec_user_sync_job / sec_user_sync_detail 已在 PR-FINAL-03 删除（ADR-0006）
--    新设计 source/task/log 三表见 medkernel-mvp/src/main/resources/db/local/sec_user_sync_ddl.sql

-- 索引
CREATE INDEX idx_identity_binding_user ON sec_identity_binding(tenant_id, user_id);
CREATE INDEX idx_identity_binding_external ON sec_identity_binding(tenant_id, provider_id, external_subject);

-- 种子数据：身份源配置样例
INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4001, 1, 'HIS_MAIN', 'HIS主系统', 'HIS', 'HIS_ADAPTER', 'QUERY_HIS_USERS', 10, 'ACTIVE', 'system');

INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4002, 1, 'EMR_SYSTEM', '电子病历系统', 'EMR', 'EMR_ADAPTER', 'QUERY_EMR_USERS', 20, 'ACTIVE', 'system');

INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4003, 1, 'OA_SYSTEM', '办公自动化系统', 'OA', null, null, 30, 'ACTIVE', 'system');
