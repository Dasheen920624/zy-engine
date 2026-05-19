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

-- 3. 同步任务记录表：记录每次同步任务的状态和统计
CREATE TABLE IF NOT EXISTS sec_user_sync_job (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  provider_id BIGINT NOT NULL,              -- 身份源 ID
  sync_type VARCHAR(32) NOT NULL,           -- FULL/INCREMENTAL/MANUAL
  status VARCHAR(32) DEFAULT 'RUNNING' NOT NULL,  -- RUNNING/SUCCESS/PARTIAL/FAILED
  total_count INTEGER DEFAULT 0 NOT NULL,
  created_count INTEGER DEFAULT 0 NOT NULL,
  updated_count INTEGER DEFAULT 0 NOT NULL,
  disabled_count INTEGER DEFAULT 0 NOT NULL,
  skipped_count INTEGER DEFAULT 0 NOT NULL,
  error_count INTEGER DEFAULT 0 NOT NULL,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  finished_at TIMESTAMP,
  triggered_by VARCHAR(64),                 -- 触发人（user_id 或 SCHEDULED）
  error_message CLOB,
  CONSTRAINT uk_sec_user_sync_job UNIQUE (id)
);

-- 4. 同步明细表：记录每个外部用户的同步动作和结果
CREATE TABLE IF NOT EXISTS sec_user_sync_detail (
  id BIGINT PRIMARY KEY,
  job_id BIGINT NOT NULL,                   -- 同步任务 ID (sec_user_sync_job.id)
  tenant_id BIGINT NOT NULL,
  external_subject VARCHAR(200) NOT NULL,   -- 外部用户标识
  external_name VARCHAR(200),
  action VARCHAR(32) NOT NULL,              -- CREATED/UPDATED/DISABLED/SKIPPED/ERROR
  platform_user_id BIGINT,                  -- 关联的平台用户 ID
  message VARCHAR(1000),                    -- 详情或错误消息
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_identity_binding_user ON sec_identity_binding(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_identity_binding_external ON sec_identity_binding(tenant_id, provider_id, external_subject);
CREATE INDEX IF NOT EXISTS idx_sync_job_tenant ON sec_user_sync_job(tenant_id, provider_id, started_at);
CREATE INDEX IF NOT EXISTS idx_sync_detail_job ON sec_user_sync_detail(job_id);

-- 种子数据：身份源配置样例
MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4001, 1, 'HIS_MAIN', 'HIS主系统', 'HIS', 'HIS_ADAPTER', 'QUERY_HIS_USERS', 10, 'ACTIVE', 'system');

MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4002, 1, 'EMR_SYSTEM', '电子病历系统', 'EMR', 'EMR_ADAPTER', 'QUERY_EMR_USERS', 20, 'ACTIVE', 'system');

MERGE INTO sec_identity_provider (id, tenant_id, provider_code, provider_name, provider_type, adapter_code, query_code, priority, status, created_by)
KEY(tenant_id, provider_code) VALUES (4003, 1, 'OA_SYSTEM', '办公自动化系统', 'OA', null, null, 30, 'ACTIVE', 'system');
