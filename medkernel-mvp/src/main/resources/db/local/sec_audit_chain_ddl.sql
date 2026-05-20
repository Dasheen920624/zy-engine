-- SEC-010: 审计防篡改、密钥轮换和安全基线 DDL

-- 1. 加密密钥管理表：存储密钥版本和元数据
CREATE TABLE IF NOT EXISTS sec_encryption_key (
  id BIGINT PRIMARY KEY,
  key_id VARCHAR(64) NOT NULL,                   -- 密钥唯一标识（UUID）
  key_version INTEGER NOT NULL,                   -- 密钥版本号
  algorithm VARCHAR(32) NOT NULL,                 -- 加密算法（AES-256-GCM）
  key_material VARCHAR(500) NOT NULL,             -- 密钥材料（Base64 编码，生产环境应使用 KMS）
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,   -- ACTIVE/DEPRECATED/REVOKED
  activated_at TIMESTAMP NOT NULL,                -- 激活时间
  deprecated_at TIMESTAMP,                        -- 弃用时间
  expires_at TIMESTAMP,                           -- 过期时间
  description VARCHAR(500),                       -- 描述
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_encryption_key UNIQUE (key_id, key_version)
);

-- 2. 审计链校验点表：定期记录校验状态
CREATE TABLE IF NOT EXISTS sec_audit_chain_checkpoint (
  id BIGINT PRIMARY KEY,
  checkpoint_time TIMESTAMP NOT NULL,             -- 校验时间
  last_checked_id BIGINT NOT NULL,                -- 最后校验的审计记录 ID
  chain_status VARCHAR(32) NOT NULL,              -- VALID/BROKEN/IN_PROGRESS
  total_records BIGINT NOT NULL,                  -- 总记录数
  valid_records BIGINT NOT NULL,                  -- 有效记录数
  broken_records BIGINT DEFAULT 0,                -- 损坏记录数
  first_broken_id BIGINT,                         -- 第一条损坏记录 ID
  details CLOB,                                   -- 详细信息（JSON）
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. 为现有审计表添加防篡改字段

-- engine_audit_log 添加 hash 字段
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS record_hash VARCHAR(128);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(128);

-- sec_auth_audit_log 添加 hash 字段
ALTER TABLE sec_auth_audit_log ADD COLUMN IF NOT EXISTS record_hash VARCHAR(128);
ALTER TABLE sec_auth_audit_log ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(128);

-- sec_sso_audit_log 添加 hash 字段
ALTER TABLE sec_sso_audit_log ADD COLUMN IF NOT EXISTS record_hash VARCHAR(128);
ALTER TABLE sec_sso_audit_log ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(128);

-- 4. 索引
CREATE INDEX IF NOT EXISTS idx_encryption_key_status ON sec_encryption_key(status, key_version);
CREATE INDEX IF NOT EXISTS idx_audit_chain_checkpoint_time ON sec_audit_chain_checkpoint(checkpoint_time);

-- 5. 种子数据：初始加密密钥
INSERT INTO sec_encryption_key (id, key_id, key_version, algorithm, key_material, status, activated_at, description, created_by)
VALUES (10001, 'master-key-001', 1, 'AES-256-GCM',
  'base64:AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=', -- 示例密钥，生产环境应使用安全随机数
  'ACTIVE', CURRENT_TIMESTAMP, '系统初始加密密钥', 'system');
