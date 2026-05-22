-- H2 local file database DDL for SEC user synchronization
-- 院内用户体系同步：同步源、同步任务、同步日志、身份绑定
-- Oracle/DM DDL remains the production authority. This file mirrors the core
-- tables needed by the current persistence provider.
-- H2-compatible syntax: BIGINT instead of NUMBER(20), VARCHAR instead of VARCHAR2, etc.

-- 同步源配置表：HIS/EMR/OA/统一身份平台
CREATE TABLE IF NOT EXISTS sec_sync_source (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_code VARCHAR(64) NOT NULL,
  source_name VARCHAR(200) NOT NULL,
  source_type VARCHAR(32) NOT NULL,
  connection_config CLOB,
  sync_mode VARCHAR(32) NOT NULL,
  cron_expression VARCHAR(100),
  status VARCHAR(32) NOT NULL,
  description VARCHAR(500),
  last_sync_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_sync_source UNIQUE (tenant_id, source_code)
);

-- 同步任务记录表：全量/增量/手动同步任务
CREATE TABLE IF NOT EXISTS sec_sync_task (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  task_type VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_count INTEGER DEFAULT 0,
  success_count INTEGER DEFAULT 0,
  failed_count INTEGER DEFAULT 0,
  skip_count INTEGER DEFAULT 0,
  start_time TIMESTAMP,
  end_time TIMESTAMP,
  error_message CLOB,
  triggered_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_time TIMESTAMP,
  CONSTRAINT fk_sec_sync_task_source FOREIGN KEY (source_id) REFERENCES sec_sync_source(id)
);

-- 同步操作日志表：记录每个用户的同步结果
CREATE TABLE IF NOT EXISTS sec_sync_log (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  external_username VARCHAR(128),
  platform_user_id BIGINT,
  operation VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  error_message CLOB,
  sync_data CLOB,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_sec_sync_log_task FOREIGN KEY (task_id) REFERENCES sec_sync_task(id)
);

-- 外部身份绑定表：外部用户到平台用户的映射
CREATE TABLE IF NOT EXISTS sec_identity_binding (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  platform_user_id BIGINT NOT NULL,
  source_id BIGINT NOT NULL,
  external_id VARCHAR(128) NOT NULL,
  external_username VARCHAR(128),
  external_display_name VARCHAR(200),
  binding_status VARCHAR(32) NOT NULL,
  last_sync_time TIMESTAMP,
  sync_hash VARCHAR(256),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_identity_binding UNIQUE (tenant_id, source_id, external_id),
  CONSTRAINT fk_sec_identity_binding_source FOREIGN KEY (source_id) REFERENCES sec_sync_source(id),
  CONSTRAINT fk_sec_identity_binding_user FOREIGN KEY (platform_user_id) REFERENCES sec_user(id)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_sec_sync_task_tenant_source ON sec_sync_task(tenant_id, source_id);
CREATE INDEX IF NOT EXISTS idx_sec_sync_task_status ON sec_sync_task(status);
CREATE INDEX IF NOT EXISTS idx_sec_sync_task_created ON sec_sync_task(created_time);

CREATE INDEX IF NOT EXISTS idx_sec_sync_log_task ON sec_sync_log(task_id);
CREATE INDEX IF NOT EXISTS idx_sec_sync_log_external ON sec_sync_log(external_id);
CREATE INDEX IF NOT EXISTS idx_sec_sync_log_status ON sec_sync_log(status);

CREATE INDEX IF NOT EXISTS idx_sec_identity_binding_user ON sec_identity_binding(platform_user_id);
CREATE INDEX IF NOT EXISTS idx_sec_identity_binding_source ON sec_identity_binding(source_id);
CREATE INDEX IF NOT EXISTS idx_sec_identity_binding_external ON sec_identity_binding(external_id);

-- 初始化默认同步源（HIS）
MERGE INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by)
KEY(tenant_id, source_code) VALUES (4001, 1, 'HIS', '医院信息系统', 'HIS', 'MANUAL', 'ACTIVE', 'HIS 用户同步源', 'system');

-- 初始化默认同步源（EMR）
MERGE INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by)
KEY(tenant_id, source_code) VALUES (4002, 1, 'EMR', '电子病历系统', 'EMR', 'MANUAL', 'ACTIVE', 'EMR 用户同步源', 'system');

-- 初始化默认同步源（OA）
MERGE INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by)
KEY(tenant_id, source_code) VALUES (4003, 1, 'OA', '办公自动化系统', 'OA', 'MANUAL', 'ACTIVE', 'OA 用户同步源', 'system');

-- 初始化默认同步源（统一身份平台）
MERGE INTO sec_sync_source (id, tenant_id, source_code, source_name, source_type, sync_mode, status, description, created_by)
KEY(tenant_id, source_code) VALUES (4004, 1, 'UNIFIED_IDENTITY', '统一身份平台', 'IDENTITY_PLATFORM', 'MANUAL', 'ACTIVE', '统一身份平台用户同步源', 'system');
