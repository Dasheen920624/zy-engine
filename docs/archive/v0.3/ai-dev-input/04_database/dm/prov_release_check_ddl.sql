-- PROV-004 发布检查阻断 DDL (DM 达梦)

-- 发布检查清单表
CREATE TABLE IF NOT EXISTS prov_release_checklist (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  checklist_code VARCHAR(64) NOT NULL,
  checklist_name VARCHAR(200) NOT NULL,
  resource_type VARCHAR(32) NOT NULL,
  description VARCHAR(2000),
  check_items CLOB,
  blocking_rules CLOB,
  approval_required VARCHAR(5) DEFAULT 'FALSE',
  approval_roles VARCHAR(500),
  enabled VARCHAR(5) DEFAULT 'Y' NOT NULL,
  created_by VARCHAR(64),
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_by VARCHAR(64),
  updated_time TIMESTAMP,
  CONSTRAINT uk_prov_release_checklist UNIQUE (tenant_id, checklist_code)
);

COMMENT ON TABLE prov_release_checklist IS '发布检查清单表';
COMMENT ON COLUMN prov_release_checklist.checklist_code IS '清单编码';
COMMENT ON COLUMN prov_release_checklist.checklist_name IS '清单名称';
COMMENT ON COLUMN prov_release_checklist.resource_type IS '资源类型: RULE/PATHWAY/KNOWLEDGE_ASSET/CONFIG_PACKAGE/AI_MODEL';
COMMENT ON COLUMN prov_release_checklist.description IS '描述';
COMMENT ON COLUMN prov_release_checklist.check_items IS '检查项JSON';
COMMENT ON COLUMN prov_release_checklist.blocking_rules IS '阻断规则JSON';
COMMENT ON COLUMN prov_release_checklist.approval_required IS '是否需要审批: TRUE/FALSE';
COMMENT ON COLUMN prov_release_checklist.approval_roles IS '审批角色';
COMMENT ON COLUMN prov_release_checklist.enabled IS '是否启用: Y/N';
COMMENT ON COLUMN prov_release_checklist.created_by IS '创建人';
COMMENT ON COLUMN prov_release_checklist.created_time IS '创建时间';
COMMENT ON COLUMN prov_release_checklist.updated_by IS '更新人';
COMMENT ON COLUMN prov_release_checklist.updated_time IS '更新时间';

-- 发布检查结果表
CREATE TABLE IF NOT EXISTS prov_release_check_result (
  id BIGINT PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  check_code VARCHAR(64) NOT NULL,
  checklist_code VARCHAR(64),
  checklist_name VARCHAR(200),
  resource_type VARCHAR(32) NOT NULL,
  resource_code VARCHAR(128) NOT NULL,
  resource_version VARCHAR(32),
  check_status VARCHAR(16) DEFAULT 'PENDING' NOT NULL,
  check_detail CLOB,
  total_items INTEGER DEFAULT 0,
  passed_items INTEGER DEFAULT 0,
  failed_items INTEGER DEFAULT 0,
  blocked_items INTEGER DEFAULT 0,
  blocked_reason VARCHAR(2000),
  approved_by VARCHAR(64),
  approved_time TIMESTAMP,
  approval_note VARCHAR(2000),
  waived_by VARCHAR(64),
  waive_reason VARCHAR(2000),
  checked_by VARCHAR(64),
  checked_time TIMESTAMP,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT uk_prov_release_check_result UNIQUE (tenant_id, check_code)
);

COMMENT ON TABLE prov_release_check_result IS '发布检查结果表';
COMMENT ON COLUMN prov_release_check_result.check_code IS '检查编码';
COMMENT ON COLUMN prov_release_check_result.checklist_code IS '清单编码';
COMMENT ON COLUMN prov_release_check_result.checklist_name IS '清单名称';
COMMENT ON COLUMN prov_release_check_result.resource_type IS '资源类型: RULE/PATHWAY/KNOWLEDGE_ASSET/CONFIG_PACKAGE/AI_MODEL';
COMMENT ON COLUMN prov_release_check_result.resource_code IS '资源编码';
COMMENT ON COLUMN prov_release_check_result.resource_version IS '资源版本';
COMMENT ON COLUMN prov_release_check_result.check_status IS '检查状态: PENDING/PASSED/FAILED/BLOCKED/WAIVED';
COMMENT ON COLUMN prov_release_check_result.check_detail IS '检查详情JSON';
COMMENT ON COLUMN prov_release_check_result.total_items IS '总检查项数';
COMMENT ON COLUMN prov_release_check_result.passed_items IS '通过项数';
COMMENT ON COLUMN prov_release_check_result.failed_items IS '失败项数';
COMMENT ON COLUMN prov_release_check_result.blocked_items IS '阻断项数';
COMMENT ON COLUMN prov_release_check_result.blocked_reason IS '阻断原因';
COMMENT ON COLUMN prov_release_check_result.approved_by IS '审批人';
COMMENT ON COLUMN prov_release_check_result.approved_time IS '审批时间';
COMMENT ON COLUMN prov_release_check_result.approval_note IS '审批备注';
COMMENT ON COLUMN prov_release_check_result.waived_by IS '豁免人';
COMMENT ON COLUMN prov_release_check_result.waive_reason IS '豁免原因';
COMMENT ON COLUMN prov_release_check_result.checked_by IS '检查人';
COMMENT ON COLUMN prov_release_check_result.checked_time IS '检查时间';
COMMENT ON COLUMN prov_release_check_result.created_time IS '创建时间';

CREATE INDEX IF NOT EXISTS idx_prov_rc_result_tenant_status ON prov_release_check_result (tenant_id, resource_type, check_status);
CREATE INDEX IF NOT EXISTS idx_prov_rc_result_resource ON prov_release_check_result (tenant_id, resource_code, resource_version);
