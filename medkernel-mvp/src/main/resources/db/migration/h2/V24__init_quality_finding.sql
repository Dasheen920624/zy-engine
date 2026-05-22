-- AIK-004 质量发现表 (H2 本地开发库)
-- 记录知识资产质量检测结果

CREATE TABLE IF NOT EXISTS quality_finding (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  finding_code VARCHAR(64) NOT NULL,
  finding_type VARCHAR(32) NOT NULL,
  severity VARCHAR(16) DEFAULT 'WARNING',
  asset_type VARCHAR(32) NOT NULL,
  asset_code VARCHAR(128) NOT NULL,
  asset_name VARCHAR(200),
  asset_version VARCHAR(32),
  description VARCHAR(2000),
  detail_json VARCHAR(4000),
  detection_rule VARCHAR(500),
  status VARCHAR(16) DEFAULT 'OPEN' NOT NULL,
  resolved_by VARCHAR(64),
  resolution_note VARCHAR(2000),
  resolved_time TIMESTAMP,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_quality_finding UNIQUE (tenant_id, finding_code)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_quality_finding_tenant ON quality_finding (tenant_id, finding_type, status);
CREATE INDEX IF NOT EXISTS idx_quality_finding_asset ON quality_finding (tenant_id, asset_type, asset_code);
CREATE INDEX IF NOT EXISTS idx_quality_finding_severity ON quality_finding (tenant_id, severity, status);
