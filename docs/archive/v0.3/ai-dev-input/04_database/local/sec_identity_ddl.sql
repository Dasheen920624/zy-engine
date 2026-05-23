-- SEC-006: 院内用户体系同步 DDL
-- 身份源配置、外部身份绑定、同步任务、同步明细

-- 1. 身份源配置表：注册院内 HIS/EMR/OA/LDAP 等外部身份源
CREATE TABLE IF NOT EXISTS sec_identity_provider (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  provider_code VARCHAR(64) NOT NULL,
  provider_name VARCHAR(200) NOT NULL,
  provider_type VARCHAR(32) NOT NULL,       -- HIS/EMR/OA/LDAP/CAS/OIDC/SAML
  adapter_code VARCHAR(64),                 -- 关联适配器编码（如 HIS_ADAPTER）
  query_code VARCHAR(64),                   -- 关联查询编码（如 QUERY_HIS_USERS）
  priority INTEGER DEFAULT 100 NOT NULL,    -- 优先级，数字越小优先级越高
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/DISABLED
  config_json CLOB,                         -- 扩展配置（JSON）
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_provider UNIQUE (tenant_id, provider_code)
);

-- 2. 外部身份绑定表：将平台用户绑定到外部系统身份
CREATE TABLE IF NOT EXISTS sec_identity_binding (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,                  -- 平台用户 ID (sec_user.id)
  provider_id BIGINT NOT NULL,              -- 身份源 ID (sec_identity_provider.id)
  external_subject VARCHAR(200) NOT NULL,   -- 外部系统用户标识（如工号）
  external_name VARCHAR(200),               -- 外部系统用户姓名
  external_org_code VARCHAR(64),            -- 外部系统组织编码（科室编码）
  external_org_name VARCHAR(200),           -- 外部系统组织名称
  external_position VARCHAR(200),           -- 外部系统岗位
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,  -- ACTIVE/UNBOUND
  last_sync_time TIMESTAMP,                 -- 最近同步时间
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_binding UNIQUE (tenant_id, provider_id, external_subject)
);

-- 注：旧 sec_user_sync_job / sec_user_sync_detail 已在 PR-FINAL-03 删除
--    （ADR-0006：保留 security/usersync/UserSyncApiController source/task 模型，
--      废弃 security/UserSyncController provider 模型）
--    系统未上线，直接删 schema 不留 DEPRECATED 注释（用户 2026-05-21 指令）
--    新设计 source/task/log 三表见 medkernel-mvp/src/main/resources/db/local/sec_user_sync_ddl.sql

-- 索引
CREATE INDEX IF NOT EXISTS idx_identity_binding_user ON sec_identity_binding(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_identity_binding_external ON sec_identity_binding(tenant_id, provider_id, external_subject);

-- 种子数据：身份源配置样例
MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4001, 1, 'HIS_MAIN', 'HIS主系统', 'HIS', 'HIS_ADAPTER', 'QUERY_HIS_USERS', 10, 'ACTIVE', 'system');

MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4002, 1, 'EMR_SYSTEM', '电子病历系统', 'EMR', 'EMR_ADAPTER', 'QUERY_EMR_USERS', 20, 'ACTIVE', 'system');

MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4003, 1, 'OA_SYSTEM', '办公自动化系统', 'OA', null, null, 30, 'ACTIVE', 'system');
