-- 达梦 DM 8 组织上下文迁移脚本（可重复执行）
-- 用途：升级旧 zyengine DM 库，给 PE_VARIATION_RECORD / RE_RULE_EXEC_LOG / ENGINE_AUDIT_LOG
--      三张表补齐组织字段与对应索引，并把 PE_PATIENT_INSTANCE 活动唯一约束升级。
--
-- 执行方式（disql 或 DM 管理工具）：
--   SQL> START 'zyengine_org_context_migration.sql';
--
-- 幂等性：DM 8 支持 IF NOT EXISTS；本脚本可重复执行不会报错。

-- ============================================================================
-- 1. PE_VARIATION_RECORD 补组织字段
-- ============================================================================
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS group_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS hospital_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS campus_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS department_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS scope_level VARCHAR(32);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS scope_code VARCHAR(64);
ALTER TABLE pe_variation_record ADD COLUMN IF NOT EXISTS org_source VARCHAR(32);

-- ============================================================================
-- 2. RE_RULE_EXEC_LOG 补组织字段
-- ============================================================================
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS group_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS hospital_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS campus_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS department_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS scope_level VARCHAR(32);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS scope_code VARCHAR(64);
ALTER TABLE re_rule_exec_log ADD COLUMN IF NOT EXISTS org_source VARCHAR(32);

-- ============================================================================
-- 3. ENGINE_AUDIT_LOG 补组织字段
-- ============================================================================
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS group_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS hospital_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS campus_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS department_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS scope_level VARCHAR(32);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS scope_code VARCHAR(64);
ALTER TABLE engine_audit_log ADD COLUMN IF NOT EXISTS org_source VARCHAR(32);

-- ============================================================================
-- 4. 索引
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_pe_variation_org ON pe_variation_record (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_re_log_org ON re_rule_exec_log (tenant_id, hospital_code, scope_level, scope_code);
CREATE INDEX IF NOT EXISTS idx_audit_org ON engine_audit_log (tenant_id, hospital_code, scope_level, scope_code);

-- ============================================================================
-- 5. PE_PATIENT_INSTANCE 活动实例唯一约束升级
-- ============================================================================
-- 旧约束（如存在）：UK_PE_ACTIVE_INSTANCE(encounter_id, pathway_code, status)
-- 新约束：UK_PE_ACTIVE_INSTANCE(tenant_id, org_code, encounter_id, pathway_code, status)
-- DM 不支持简写"修改约束"，采用 drop+add；如名称已为新版本则会失败，可忽略。
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE pe_patient_instance DROP CONSTRAINT uk_pe_active_instance';
  EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
  EXECUTE IMMEDIATE 'ALTER TABLE pe_patient_instance ADD CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)';
  EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- ============================================================================
-- 6. 中文备注（DM UTF8 库直接写中文）
-- ============================================================================
COMMENT ON COLUMN pe_variation_record.org_source IS '组织来源：HEADER/QUERY/BODY/DEFAULT/NONE';
COMMENT ON COLUMN re_rule_exec_log.org_source IS '组织来源：HEADER/QUERY/BODY/DEFAULT/NONE';
COMMENT ON COLUMN engine_audit_log.org_source IS '组织来源：HEADER/QUERY/BODY/DEFAULT/NONE';
