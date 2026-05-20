-- PostgreSQL SEC DDL for user authentication and authorization
-- 用户体系最小可用：租户、用户、角色、权限、用户角色、用户组织范围
-- 中文备注规范：表和关键字段必须有中文注释

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

COMMENT ON TABLE sec_tenant IS '租户表：多租户隔离的基础';
COMMENT ON COLUMN sec_tenant.tenant_code IS '租户编码，唯一标识';
COMMENT ON COLUMN sec_tenant.tenant_name IS '租户名称';
COMMENT ON COLUMN sec_tenant.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sec_tenant.license_type IS '授权类型：TRIAL/STANDARD/ENTERPRISE';
COMMENT ON COLUMN sec_tenant.max_users IS '最大用户数';

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

COMMENT ON TABLE sec_user IS '用户表：平台独立用户';
COMMENT ON COLUMN sec_user.tenant_id IS '所属租户ID';
COMMENT ON COLUMN sec_user.username IS '用户名，租户内唯一';
COMMENT ON COLUMN sec_user.password_hash IS '密码哈希（bcrypt + pepper）';
COMMENT ON COLUMN sec_user.display_name IS '显示名称';
COMMENT ON COLUMN sec_user.status IS '状态：ACTIVE/DISABLED/LOCKED';
COMMENT ON COLUMN sec_user.login_attempts IS '连续登录失败次数';
COMMENT ON COLUMN sec_user.locked_until IS '锁定截止时间';

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

COMMENT ON TABLE sec_role IS '角色表：预置和自定义角色';
COMMENT ON COLUMN sec_role.role_code IS '角色编码';
COMMENT ON COLUMN sec_role.role_name IS '角色名称';
COMMENT ON COLUMN sec_role.role_type IS '角色类型：SYSTEM/PLATFORM/HOSPITAL/DEPARTMENT';
COMMENT ON COLUMN sec_role.status IS '状态：ACTIVE/DISABLED';

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

COMMENT ON TABLE sec_permission IS '权限表：菜单、按钮、API 权限';
COMMENT ON COLUMN sec_permission.permission_code IS '权限编码';
COMMENT ON COLUMN sec_permission.permission_type IS '权限类型：MENU/BUTTON/API';
COMMENT ON COLUMN sec_permission.resource_path IS '资源路径（菜单路径或 API 路径）';

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

COMMENT ON TABLE sec_user_role IS '用户角色关联表';
COMMENT ON COLUMN sec_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sec_user_role.role_id IS '角色ID';

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

COMMENT ON TABLE sec_user_org_scope IS '用户组织范围表：限定用户可访问的组织范围';
COMMENT ON COLUMN sec_user_org_scope.scope_level IS '组织层级：PLATFORM/GROUP/HOSPITAL/CAMPUS/SITE/DEPARTMENT';
COMMENT ON COLUMN sec_user_org_scope.scope_code IS '组织编码';

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

COMMENT ON TABLE sec_role_permission IS '角色权限关联表';

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

COMMENT ON TABLE sec_auth_audit_log IS '认证审计日志表';
COMMENT ON COLUMN sec_auth_audit_log.event_type IS '事件类型：LOGIN/LOGOUT/LOGIN_FAILED';
COMMENT ON COLUMN sec_auth_audit_log.event_result IS '事件结果：SUCCESS/FAILURE';

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
-- 密码：demo123 -> $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
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

-- 用户组织范围（默认租户全部可见）
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

COMMENT ON TABLE sec_identity_provider IS '身份源配置表：注册院内HIS/EMR/OA/LDAP等外部身份源';
COMMENT ON COLUMN sec_identity_provider.provider_code IS '身份源编码，租户内唯一';
COMMENT ON COLUMN sec_identity_provider.provider_name IS '身份源名称';
COMMENT ON COLUMN sec_identity_provider.provider_type IS '身份源类型：HIS/EMR/OA/LDAP/CAS/OIDC/SAML';
COMMENT ON COLUMN sec_identity_provider.adapter_code IS '关联适配器编码';
COMMENT ON COLUMN sec_identity_provider.query_code IS '关联查询编码';
COMMENT ON COLUMN sec_identity_provider.priority IS '优先级，数字越小优先级越高';
COMMENT ON COLUMN sec_identity_provider.status IS '状态：ACTIVE/DISABLED';
COMMENT ON COLUMN sec_identity_provider.config_json IS '扩展配置（JSON格式）';

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

COMMENT ON TABLE sec_identity_binding IS '外部身份绑定表：将平台用户绑定到外部系统身份';
COMMENT ON COLUMN sec_identity_binding.user_id IS '平台用户ID（sec_user.id）';
COMMENT ON COLUMN sec_identity_binding.provider_id IS '身份源ID（sec_identity_provider.id）';
COMMENT ON COLUMN sec_identity_binding.external_subject IS '外部系统用户标识（如工号）';
COMMENT ON COLUMN sec_identity_binding.external_name IS '外部系统用户姓名';
COMMENT ON COLUMN sec_identity_binding.external_org_code IS '外部系统组织编码（科室编码）';
COMMENT ON COLUMN sec_identity_binding.external_org_name IS '外部系统组织名称';
COMMENT ON COLUMN sec_identity_binding.external_position IS '外部系统岗位';
COMMENT ON COLUMN sec_identity_binding.status IS '状态：ACTIVE/UNBOUND';
COMMENT ON COLUMN sec_identity_binding.last_sync_time IS '最近同步时间';

-- 同步任务记录表：记录每次同步任务的状态和统计
CREATE TABLE sec_user_sync_job (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,
  sync_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) DEFAULT 'RUNNING' NOT NULL,
  total_count INT DEFAULT 0 NOT NULL,
  created_count INT DEFAULT 0 NOT NULL,
  updated_count INT DEFAULT 0 NOT NULL,
  disabled_count INT DEFAULT 0 NOT NULL,
  skipped_count INT DEFAULT 0 NOT NULL,
  error_count INT DEFAULT 0 NOT NULL,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  triggered_by VARCHAR(64),
  error_message TEXT,
  CONSTRAINT uk_sec_user_sync_job UNIQUE (id)
);

COMMENT ON TABLE sec_user_sync_job IS '同步任务记录表：记录每次同步任务的状态和统计';
COMMENT ON COLUMN sec_user_sync_job.provider_id IS '身份源ID';
COMMENT ON COLUMN sec_user_sync_job.sync_type IS '同步类型：FULL/INCREMENTAL/MANUAL';
COMMENT ON COLUMN sec_user_sync_job.status IS '任务状态：RUNNING/SUCCESS/PARTIAL/FAILED';
COMMENT ON COLUMN sec_user_sync_job.total_count IS '总处理数';
COMMENT ON COLUMN sec_user_sync_job.created_count IS '新建用户数';
COMMENT ON COLUMN sec_user_sync_job.updated_count IS '更新用户数';
COMMENT ON COLUMN sec_user_sync_job.disabled_count IS '禁用用户数';
COMMENT ON COLUMN sec_user_sync_job.skipped_count IS '跳过用户数';
COMMENT ON COLUMN sec_user_sync_job.error_count IS '错误用户数';
COMMENT ON COLUMN sec_user_sync_job.triggered_by IS '触发人（user_id 或 SCHEDULED）';
COMMENT ON COLUMN sec_user_sync_job.error_message IS '错误消息';

-- 同步明细表：记录每个外部用户的同步动作和结果
CREATE TABLE sec_user_sync_detail (
  id BIGINT PRIMARY KEY,
  job_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  external_subject VARCHAR(200) NOT NULL,
  external_name VARCHAR(200),
  action VARCHAR(32) NOT NULL,
  platform_user_id BIGINT,
  message VARCHAR(1000),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

COMMENT ON TABLE sec_user_sync_detail IS '同步明细表：记录每个外部用户的同步动作和结果';
COMMENT ON COLUMN sec_user_sync_detail.job_id IS '同步任务ID（sec_user_sync_job.id）';
COMMENT ON COLUMN sec_user_sync_detail.external_subject IS '外部用户标识';
COMMENT ON COLUMN sec_user_sync_detail.external_name IS '外部用户姓名';
COMMENT ON COLUMN sec_user_sync_detail.action IS '同步动作：CREATED/UPDATED/DISABLED/SKIPPED/ERROR';
COMMENT ON COLUMN sec_user_sync_detail.platform_user_id IS '关联的平台用户ID';
COMMENT ON COLUMN sec_user_sync_detail.message IS '详情或错误消息';

-- 索引
CREATE INDEX idx_identity_binding_user ON sec_identity_binding(tenant_id, user_id);
CREATE INDEX idx_identity_binding_external ON sec_identity_binding(tenant_id, provider_id, external_subject);
CREATE INDEX idx_sync_job_tenant ON sec_user_sync_job(tenant_id, provider_id, started_at);
CREATE INDEX idx_sync_detail_job ON sec_user_sync_detail(job_id);

-- 种子数据：身份源配置样例
INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4001, 1, 'HIS_MAIN', 'HIS主系统', 'HIS', 'HIS_ADAPTER', 'QUERY_HIS_USERS', 10, 'ACTIVE', 'system');

INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4002, 1, 'EMR_SYSTEM', '电子病历系统', 'EMR', 'EMR_ADAPTER', 'QUERY_EMR_USERS', 20, 'ACTIVE', 'system');

INSERT INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
VALUES (4003, 1, 'OA_SYSTEM', '办公自动化系统', 'OA', null, null, 30, 'ACTIVE', 'system');
