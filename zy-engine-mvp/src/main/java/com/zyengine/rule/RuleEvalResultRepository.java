package com.zyengine.rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

/**
 * 规则评估结果 Repository
 * 支持跨实例查询和持久化
 */
@Repository
public class RuleEvalResultRepository {

    private static final Logger log = LoggerFactory.getLogger(RuleEvalResultRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存评估结果
     */
    public void save(RuleEvalResultEntity entity) {
        if (entity.getId() == null) {
            entity.setId(generateId());
        }
        if (entity.getEvalId() == null) {
            entity.setEvalId(UUID.randomUUID().toString().replace("-", ""));
        }

        String sql = "INSERT INTO re_rule_eval_result (" +
                "id, eval_id, rule_code, rule_version, patient_id, encounter_id, " +
                "hit_flag, severity, message, actions, evidence, " +
                "input_snapshot, output_snapshot, elapsed_ms, result_status, " +
                "error_code, error_message, tenant_id, group_code, hospital_code, " +
                "campus_code, site_code, department_code, scope_level, scope_code, org_source" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            jdbcTemplate.update(sql,
                    entity.getId(),
                    entity.getEvalId(),
                    entity.getRuleCode(),
                    entity.getRuleVersion(),
                    entity.getPatientId(),
                    entity.getEncounterId(),
                    entity.isHit() ? 1 : 0,
                    entity.getSeverity(),
                    entity.getMessage(),
                    entity.getActionsJson(),
                    entity.getEvidenceJson(),
                    entity.getInputSnapshotJson(),
                    entity.getOutputSnapshotJson(),
                    entity.getElapsedMs(),
                    entity.getResultStatus(),
                    entity.getErrorCode(),
                    entity.getErrorMessage(),
                    entity.getTenantId(),
                    entity.getGroupCode(),
                    entity.getHospitalCode(),
                    entity.getCampusCode(),
                    entity.getSiteCode(),
                    entity.getDepartmentCode(),
                    entity.getScopeLevel(),
                    entity.getScopeCode(),
                    entity.getOrgSource()
            );
            log.debug("Saved rule eval result: evalId={}, ruleCode={}", entity.getEvalId(), entity.getRuleCode());
        } catch (Exception e) {
            log.error("Failed to save rule eval result: evalId={}, ruleCode={}", entity.getEvalId(), entity.getRuleCode(), e);
            throw e;
        }
    }

    /**
     * 根据 evalId 查询评估结果
     */
    public List<RuleEvalResultEntity> findByEvalId(String evalId) {
        String sql = "SELECT * FROM re_rule_eval_result WHERE eval_id = ?";
        return jdbcTemplate.query(sql, new Object[]{evalId}, new RuleEvalResultRowMapper());
    }

    /**
     * 根据 evalId 和 ruleCode 查询单个评估结果
     */
    public RuleEvalResultEntity findByEvalIdAndRuleCode(String evalId, String ruleCode) {
        String sql = "SELECT * FROM re_rule_eval_result WHERE eval_id = ? AND rule_code = ?";
        List<RuleEvalResultEntity> results = jdbcTemplate.query(sql, new Object[]{evalId, ruleCode}, new RuleEvalResultRowMapper());
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 根据患者和就诊查询评估结果
     */
    public List<RuleEvalResultEntity> findByPatientAndEncounter(String patientId, String encounterId) {
        String sql = "SELECT * FROM re_rule_eval_result WHERE patient_id = ? AND encounter_id = ? ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, new Object[]{patientId, encounterId}, new RuleEvalResultRowMapper());
    }

    /**
     * 根据组织上下文查询评估结果
     */
    public List<RuleEvalResultEntity> findByOrgContext(String tenantId, String hospitalCode, String scopeLevel, String scopeCode) {
        String sql = "SELECT * FROM re_rule_eval_result WHERE tenant_id = ? AND hospital_code = ? AND scope_level = ? AND scope_code = ? ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, new Object[]{tenantId, hospitalCode, scopeLevel, scopeCode}, new RuleEvalResultRowMapper());
    }

    /**
     * 根据规则编码查询评估结果
     */
    public List<RuleEvalResultEntity> findByRuleCode(String ruleCode, String ruleVersion) {
        String sql;
        Object[] params;
        if (ruleVersion != null) {
            sql = "SELECT * FROM re_rule_eval_result WHERE rule_code = ? AND rule_version = ? ORDER BY created_time DESC";
            params = new Object[]{ruleCode, ruleVersion};
        } else {
            sql = "SELECT * FROM re_rule_eval_result WHERE rule_code = ? ORDER BY created_time DESC";
            params = new Object[]{ruleCode};
        }
        return jdbcTemplate.query(sql, params, new RuleEvalResultRowMapper());
    }

    /**
     * 查询命中的评估结果
     */
    public List<RuleEvalResultEntity> findHitResults(String tenantId, String ruleCode) {
        String sql = "SELECT * FROM re_rule_eval_result WHERE tenant_id = ? AND rule_code = ? AND hit_flag = 1 ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, new Object[]{tenantId, ruleCode}, new RuleEvalResultRowMapper());
    }

    /**
     * 统计评估结果数量
     */
    public int countByTenantAndRule(String tenantId, String ruleCode) {
        String sql = "SELECT COUNT(*) FROM re_rule_eval_result WHERE tenant_id = ? AND rule_code = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{tenantId, ruleCode}, Integer.class);
    }

    /**
     * 删除指定时间之前的评估结果
     */
    public int deleteBeforeTime(String timestamp) {
        String sql = "DELETE FROM re_rule_eval_result WHERE created_time < ?";
        return jdbcTemplate.update(sql, timestamp);
    }

    /**
     * 生成主键ID
     */
    private Long generateId() {
        return System.currentTimeMillis() * 1000 + (long) (Math.random() * 1000);
    }

    /**
     * RowMapper 内部类
     */
    private static class RuleEvalResultRowMapper implements RowMapper<RuleEvalResultEntity> {
        @Override
        public RuleEvalResultEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            RuleEvalResultEntity entity = new RuleEvalResultEntity();
            entity.setId(rs.getLong("id"));
            entity.setEvalId(rs.getString("eval_id"));
            entity.setRuleCode(rs.getString("rule_code"));
            entity.setRuleVersion(rs.getString("rule_version"));
            entity.setPatientId(rs.getString("patient_id"));
            entity.setEncounterId(rs.getString("encounter_id"));
            entity.setHit(rs.getInt("hit_flag") == 1);
            entity.setSeverity(rs.getString("severity"));
            entity.setMessage(rs.getString("message"));
            entity.setActionsJson(rs.getString("actions"));
            entity.setEvidenceJson(rs.getString("evidence"));
            entity.setInputSnapshotJson(rs.getString("input_snapshot"));
            entity.setOutputSnapshotJson(rs.getString("output_snapshot"));
            entity.setElapsedMs(rs.getLong("elapsed_ms"));
            entity.setResultStatus(rs.getString("result_status"));
            entity.setErrorCode(rs.getString("error_code"));
            entity.setErrorMessage(rs.getString("error_message"));
            entity.setTenantId(rs.getString("tenant_id"));
            entity.setGroupCode(rs.getString("group_code"));
            entity.setHospitalCode(rs.getString("hospital_code"));
            entity.setCampusCode(rs.getString("campus_code"));
            entity.setSiteCode(rs.getString("site_code"));
            entity.setDepartmentCode(rs.getString("department_code"));
            entity.setScopeLevel(rs.getString("scope_level"));
            entity.setScopeCode(rs.getString("scope_code"));
            entity.setOrgSource(rs.getString("org_source"));
            entity.setCreatedTime(rs.getString("created_time"));
            return entity;
        }
    }
}
