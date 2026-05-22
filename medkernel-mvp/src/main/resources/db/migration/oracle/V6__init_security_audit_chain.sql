-- SEC-010: 审计防篡改、密钥轮换和安全基线 DDL (Oracle)

-- 1. 加密密钥管理表
CREATE TABLE sec_encryption_key (
  id NUMBER(20) PRIMARY KEY,
  key_id VARCHAR2(64) NOT NULL,
  key_version NUMBER(10) NOT NULL,
  algorithm VARCHAR2(32) NOT NULL,
  key_material VARCHAR2(500) NOT NULL,
  status VARCHAR2(32) DEFAULT 'ACTIVE' NOT NULL,
  activated_at TIMESTAMP NOT NULL,
  deprecated_at TIMESTAMP,
  expires_at TIMESTAMP,
  description VARCHAR2(500),
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  updated_by VARCHAR2(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_sec_encryption_key UNIQUE (key_id, key_version)
);

-- 2. 审计链校验点表
CREATE TABLE sec_audit_chain_checkpoint (
  id NUMBER(20) PRIMARY KEY,
  checkpoint_time TIMESTAMP NOT NULL,
  last_checked_id NUMBER(20) NOT NULL,
  chain_status VARCHAR2(32) NOT NULL,
  total_records NUMBER(20) NOT NULL,
  valid_records NUMBER(20) NOT NULL,
  broken_records NUMBER(20) DEFAULT 0,
  first_broken_id NUMBER(20),
  details CLOB,
  created_by VARCHAR2(64),
  created_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

-- 3. 为现有审计表添加防篡改字段
ALTER TABLE engine_audit_log ADD record_hash VARCHAR2(128);
ALTER TABLE engine_audit_log ADD chain_hash VARCHAR2(128);

ALTER TABLE sec_auth_audit_log ADD record_hash VARCHAR2(128);
ALTER TABLE sec_auth_audit_log ADD chain_hash VARCHAR2(128);

ALTER TABLE sec_sso_audit_log ADD record_hash VARCHAR2(128);
ALTER TABLE sec_sso_audit_log ADD chain_hash VARCHAR2(128);

-- 4. 索引
CREATE INDEX idx_encryption_key_status ON sec_encryption_key(status, key_version);
CREATE INDEX idx_audit_chain_checkpoint_time ON sec_audit_chain_checkpoint(checkpoint_time);

-- 5. 种子数据
INSERT INTO sec_encryption_key (id, key_id, key_version, algorithm, key_material, status, activated_at, description, created_by)
VALUES (10001, 'master-key-001', 1, 'AES-256-GCM',
  'base64:AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=',
  'ACTIVE', SYSTIMESTAMP, '系统初始加密密钥', 'system');

COMMIT;
