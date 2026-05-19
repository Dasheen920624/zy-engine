-- PostgreSQL SEC DDL for user authentication and authorization
-- 适用 PostgreSQL 14/15/16 与 KingbaseES V8（PG 兼容模式）
-- 用户体系最小可用：租户、用户、角色、权限、用户角色、用户组织范围
-- 字段层面统一约定：
--   主键 ID 应用层生成（BIGINT）
--   时间统一 TIMESTAMP（不带时区；应用层 Asia/Shanghai）
--   布尔统一 SMALLINT 0/1（与 Oracle/达梦保持一致；mapper 转 boolean）

-- 租户表：多租户隔离的基础
CREATE TABLE IF NOT EXISTS sec_tenant (
  id BIGINT PRIMARY KEY,
  tenant_code VARCHAR(64) NOT NULL,
  tenant_name VARCHAR(200) NOT NULL,
  status VARCHAR(32) NOT NULL,
  contact_name VARCHAR(100),
  contact_phone VARCHAR(32),
  contact_email VARCHAR(200),
  license_type VARCHAR(32),
  max_users INT NOT NULL DEFAULT 100,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_tenant UNIQUE (tenant_code)
);

COMMENT ON TABLE sec_tenant IS '租户表：多租户隔离的基础';
COMMENT ON COLUMN sec_tenant.tenant_code IS '租户编码，唯一标识';
COMMENT ON COLUMN sec_tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN sec_tenant.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sec_tenant.license_type IS '授权类型：TRIAL/STANDARD/ENTERPRISE';

-- 用户表：平台独立用户，不依赖院内身份源
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
  login_attempts INT NOT NULL DEFAULT 0,
  locked_until TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_user UNIQUE (tenant_id, username)
);

COMMENT ON TABLE sec_user IS '用户表：平台独立用户';
COMMENT ON COLUMN sec_user.tenant_id IS '所属租户ID';
COMMENT ON COLUMN sec_user.username IS '用户名，租户内唯一';
COMMENT ON COLUMN sec_user.password_hash IS '密码哈希（bcrypt + pepper）';
COMMENT ON COLUMN sec_user.display_name IS '显示名称';
COMMENT ON COLUMN sec_user.status IS '状态：ACTIVE/DISABLED/LOCKED';

-- 角色表：预置和自定义角色
CREATE TABLE IF NOT EXISTS sec_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  role_type VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_role UNIQUE (tenant_id, role_code)
);

COMMENT ON TABLE sec_role IS '角色表：预置和自定义角色';

-- 权限表：菜单、按钮、API 权限
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
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sec_permission UNIQUE (tenant_id, permission_code)
);

COMMENT ON TABLE sec_permission IS '权限表：菜单、按钮、API 权限';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sec_user_role (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sec_user_role UNIQUE (tenant_id, user_id, role_id)
);

COMMENT ON TABLE sec_user_role IS '用户角色关联表';

-- 用户组织范围表：限定用户可访问的组织范围
CREATE TABLE IF NOT EXISTS sec_user_org_scope (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  scope_level VARCHAR(32) NOT NULL,
  scope_code VARCHAR(64) NOT NULL,
  scope_name VARCHAR(200),
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sec_user_org_scope UNIQUE (tenant_id, user_id, scope_level, scope_code)
);

COMMENT ON TABLE sec_user_org_scope IS '用户组织范围表：限定用户可访问的组织范围';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sec_role_permission (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_sec_role_permission UNIQUE (tenant_id, role_id, permission_id)
);

COMMENT ON TABLE sec_role_permission IS '角色权限关联表';

-- 审计日志表：登录、登出、登录失败
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
  created_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sec_auth_audit_log IS '认证审计日志表';

-- 初始化演示租户
INSERT INTO sec_tenant (id, tenant_code, tenant_name, status, license_type, max_users, created_by)
VALUES (1, 'default', '默认租户', 'ACTIVE', 'ENTERPRISE', 1000, 'system')
ON CONFLICT (tenant_code) DO NOTHING;

-- 初始化演示角色（9 个角色）
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (101, 1, 'ADMIN', '系统管理员', 'PLATFORM', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (102, 1, 'DOCTOR', '医生', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (103, 1, 'QC_NURSE', '质控护士', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (104, 1, 'INSURANCE', '医保专员', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (105, 1, 'IT_ADMIN', '信息科管理员', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (106, 1, 'IMPLEMENTER', '实施工程师', 'PLATFORM', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (107, 1, 'HOSPITAL_LEADER', '院领导', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (108, 1, 'MEDICAL_EXPERT', '医学审核专家', 'HOSPITAL', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;
INSERT INTO sec_role (id, tenant_id, role_code, role_name, role_type, status, created_by) VALUES (109, 1, 'PLATFORM_OPS', '平台运营', 'PLATFORM', 'ACTIVE', 'system') ON CONFLICT (tenant_id, role_code) DO NOTHING;

-- 初始化演示用户
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1001, 1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1002, 1, 'zhao01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵医生', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1003, 1, 'qian02', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '钱护士', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1004, 1, 'sun03', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙医保', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1005, 1, 'li04', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李信息', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1006, 1, 'zhou05', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '周实施', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1007, 1, 'wu06', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '吴院长', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1008, 1, 'zheng07', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '郑专家', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;
INSERT INTO sec_user (id, tenant_id, username, password_hash, display_name, status, created_by) VALUES (1009, 1, 'wang08', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王运营', 'ACTIVE', 'system') ON CONFLICT (tenant_id, username) DO NOTHING;

-- 用户角色关联
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2001, 1, 1001, 101, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2002, 1, 1002, 102, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2003, 1, 1003, 103, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2004, 1, 1004, 104, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2005, 1, 1005, 105, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2006, 1, 1006, 106, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2007, 1, 1007, 107, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2008, 1, 1008, 108, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;
INSERT INTO sec_user_role (id, tenant_id, user_id, role_id, created_by) VALUES (2009, 1, 1009, 109, 'system') ON CONFLICT (tenant_id, user_id, role_id) DO NOTHING;

-- 用户组织范围
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3001, 1, 1001, 'PLATFORM', 'DEFAULT', '全部', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3002, 1, 1002, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3003, 1, 1003, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3004, 1, 1004, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3005, 1, 1005, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3006, 1, 1006, 'PLATFORM', 'DEFAULT', '全部', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3007, 1, 1007, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3008, 1, 1008, 'HOSPITAL', 'ZYHOSPITAL', '中医院', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
INSERT INTO sec_user_org_scope (id, tenant_id, user_id, scope_level, scope_code, scope_name, created_by) VALUES (3009, 1, 1009, 'PLATFORM', 'DEFAULT', '全部', 'system') ON CONFLICT (tenant_id, user_id, scope_level, scope_code) DO NOTHING;
