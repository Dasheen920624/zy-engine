-- SEC-010: 审计防篡改、密钥轮换和安全基线 DDL (PostgreSQL)

-- 1. 加密密钥管理表
CREATE TABLE IF NOT EXISTS sec_encryption_key (
  id BIGINT PRIMARY KEY,
  key_id VARCHAR(64) NOT NULL,
  key_version INTEGER NOT NULL,
  algorithm VARCHAR(32) NOT NULL,
  key_material VARCHAR(500) NOT NULL,
  status VARCHAR(32) DEFAULT 'ACTIVE' NOT NULL,
  activated_at TIMESTAMP NOT NULL,
  deprecated_at TIMESTAMP,
  expires_at TIMESTAMP,
  description VARCHAR(500),
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_encryption_key UNIQUE (key_id, key_version)
);

-- 2. 审计链校验点表
CREATE TABLE IF NOT EXISTS sec_audit_chain_checkpoint (
  id BIGINT PRIMARY KEY,
  checkpoint_time TIMESTAMP NOT NULL,
  last_checked_id BIGINT NOT NULL,
  chain_status VARCHAR(32) NOT NULL,
  total_records BIGINT NOT NULL,
  valid_records BIGINT NOT NULL,
  broken_records BIGINT DEFAULT 0,
  first_broken_id BIGINT,
  details TEXT,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. 为现有审计表添加防篡改字段
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='engine_audit_log' AND column_name='record_hash') THEN
    ALTER TABLE engine_audit_log ADD COLUMN record_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='engine_audit_log' AND column_name='chain_hash') THEN
    ALTER TABLE engine_audit_log ADD COLUMN chain_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sec_auth_audit_log' AND column_name='record_hash') THEN
    ALTER TABLE sec_auth_audit_log ADD COLUMN record_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sec_auth_audit_log' AND column_name='chain_hash') THEN
    ALTER TABLE sec_auth_audit_log ADD COLUMN chain_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sec_sso_audit_log' AND column_name='record_hash') THEN
    ALTER TABLE sec_sso_audit_log ADD COLUMN record_hash VARCHAR(128);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='sec_sso_audit_log' AND column_name='chain_hash') THEN
    ALTER TABLE sec_sso_audit_log ADD COLUMN chain_hash VARCHAR(128);
  END IF;
END $$;

-- 4. 索引
CREATE INDEX IF NOT EXISTS idx_encryption_key_status ON sec_encryption_key(status, key_version);
CREATE INDEX IF NOT EXISTS idx_audit_chain_checkpoint_time ON sec_audit_chain_checkpoint(checkpoint_time);

-- 5. 种子数据
INSERT INTO sec_encryption_key (id, key_id, key_version, algorithm, key_material, status, activated_at, description, created_by)
VALUES (10001, 'master-key-001', 1, 'AES-256-GCM',
  'base64:AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=',
  'ACTIVE', CURRENT_TIMESTAMP, '系统初始加密密钥', 'system')
ON CONFLICT (key_id, key_version) DO NOTHING;
