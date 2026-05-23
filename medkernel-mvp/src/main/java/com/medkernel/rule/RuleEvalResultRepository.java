package com.medkernel.rule;

import com.medkernel.common.dataclass.FieldEncryptionService;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 规则评估结果 Repository。
 *
 * <p>GA-DATA-01：透明 SM4 加密。{@link FieldEncryptionService} 在 save 前加密
 * 标 {@code @Encrypted} 的字段，read 后解密。Service 层无感知。
 *
 * <p>风格与 ConfigPackageRepository 一致：手写 Connection + PreparedStatement + try-with-resources。
 * 跨方言策略：
 *  - hit_flag 在 Oracle/DM/H2 用 NUMBER/INT (0/1)，在 PostgreSQL 用 SMALLINT (0/1) 而非 BOOLEAN；
 *  - actions/evidence/*_snapshot 在所有方言统一为 CLOB/TEXT，写入 JSON 字符串，避免 PG JSONB 类型差异。
 *  - save() 采用先 UPDATE 再 INSERT 的 upsert 策略，规避不同方言 MERGE/ON CONFLICT 语法差异，并兼容 UNIQUE(eval_id, rule_code)。
 */
@Repository
public class RuleEvalResultRepository {

    private static final Logger log = LoggerFactory.getLogger(RuleEvalResultRepository.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;
    private final FieldEncryptionService fieldEncryption;

    public RuleEvalResultRepository(EnginePersistenceProperties properties,
                                    DataSource dataSource,
                                    FieldEncryptionService fieldEncryption) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.fieldEncryption = fieldEncryption;
    }

    public boolean enabled() {
        return properties.isEnabled() && properties.hasRequiredCredentials();
    }

    public void save(RuleEvalResultEntity entity) {
        if (!enabled()) {
            return;
        }
        if (entity.getEvalId() == null || entity.getEvalId().isEmpty()) {
            entity.setEvalId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (entity.getId() == null) {
            entity.setId(Ids.next());
        }

        // GA-DATA-01：写入前加密 @Encrypted 字段
        fieldEncryption.encryptEntity(entity);
        try (Connection connection = connection()) {
        String updateSql = "UPDATE re_rule_eval_result SET " +
                "rule_version=?, patient_id=?, encounter_id=?, hit_flag=?, severity=?, message=?, " +
                "actions=?, evidence=?, input_snapshot=?, output_snapshot=?, elapsed_ms=?, " +
                "result_status=?, error_code=?, error_message=?, " +
                "tenant_id=?, group_code=?, hospital_code=?, campus_code=?, site_code=?, " +
                "department_code=?, scope_level=?, scope_code=?, org_source=? " +
                "WHERE eval_id=? AND rule_code=?";

            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, entity.getRuleVersion());
                ps.setString(i++, entity.getPatientId());
                ps.setString(i++, entity.getEncounterId());
                ps.setInt(i++, entity.isHit() ? 1 : 0);
                ps.setString(i++, entity.getSeverity());
                ps.setString(i++, entity.getMessage());
                ps.setString(i++, entity.getActionsJson());
                ps.setString(i++, entity.getEvidenceJson());
                ps.setString(i++, entity.getInputSnapshotJson());
                ps.setString(i++, entity.getOutputSnapshotJson());
                if (entity.getElapsedMs() == null) {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(i++, entity.getElapsedMs());
                }
                ps.setString(i++, entity.getResultStatus());
                ps.setString(i++, entity.getErrorCode());
                ps.setString(i++, entity.getErrorMessage());
                ps.setString(i++, entity.getTenantId());
                ps.setString(i++, entity.getGroupCode());
                ps.setString(i++, entity.getHospitalCode());
                ps.setString(i++, entity.getCampusCode());
                ps.setString(i++, entity.getSiteCode());
                ps.setString(i++, entity.getDepartmentCode());
                ps.setString(i++, entity.getScopeLevel());
                ps.setString(i++, entity.getScopeCode());
                ps.setString(i++, entity.getOrgSource());
                ps.setString(i++, entity.getEvalId());
                ps.setString(i++, entity.getRuleCode());
                updated = ps.executeUpdate();
            }
            if (updated > 0) {
                return;
            }

            // 2) UPDATE 受影响 0 行则 INSERT
            String insertSql = "INSERT INTO re_rule_eval_result (" +
                    "id, eval_id, rule_code, rule_version, patient_id, encounter_id, hit_flag, " +
                    "severity, message, actions, evidence, input_snapshot, output_snapshot, " +
                    "elapsed_ms, result_status, error_code, error_message, " +
                    "tenant_id, group_code, hospital_code, campus_code, site_code, " +
                    "department_code, scope_level, scope_code, org_source" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                int i = 1;
                ps.setLong(i++, entity.getId());
                ps.setString(i++, entity.getEvalId());
                ps.setString(i++, entity.getRuleCode());
                ps.setString(i++, entity.getRuleVersion());
                ps.setString(i++, entity.getPatientId());
                ps.setString(i++, entity.getEncounterId());
                ps.setInt(i++, entity.isHit() ? 1 : 0);
                ps.setString(i++, entity.getSeverity());
                ps.setString(i++, entity.getMessage());
                ps.setString(i++, entity.getActionsJson());
                ps.setString(i++, entity.getEvidenceJson());
                ps.setString(i++, entity.getInputSnapshotJson());
                ps.setString(i++, entity.getOutputSnapshotJson());
                if (entity.getElapsedMs() == null) {
                    ps.setNull(i++, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(i++, entity.getElapsedMs());
                }
                ps.setString(i++, entity.getResultStatus());
                ps.setString(i++, entity.getErrorCode());
                ps.setString(i++, entity.getErrorMessage());
                ps.setString(i++, entity.getTenantId());
                ps.setString(i++, entity.getGroupCode());
                ps.setString(i++, entity.getHospitalCode());
                ps.setString(i++, entity.getCampusCode());
                ps.setString(i++, entity.getSiteCode());
                ps.setString(i++, entity.getDepartmentCode());
                ps.setString(i++, entity.getScopeLevel());
                ps.setString(i++, entity.getScopeCode());
                ps.setString(i++, entity.getOrgSource());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            log.error("save rule eval result failed: evalId={}, ruleCode={}", entity.getEvalId(), entity.getRuleCode(), ex);
            throw new IllegalStateException("save rule eval result failed: " + ex.getMessage(), ex);
        } finally {
            // GA-DATA-01：写入完成后解密回明文，调用方拿到的 entity 始终是明文状态
            fieldEncryption.decryptEntity(entity);
        }
    }

    public List<RuleEvalResultEntity> findByEvalId(String evalId) {
        if (!enabled()) {
            return new ArrayList<RuleEvalResultEntity>();
        }
        String sql = "SELECT id, eval_id, rule_code, rule_version, patient_id, encounter_id, hit_flag, " +
                "severity, message, actions, evidence, input_snapshot, output_snapshot, elapsed_ms, " +
                "result_status, error_code, error_message, tenant_id, group_code, hospital_code, " +
                "campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time " +
                "FROM re_rule_eval_result WHERE eval_id=?";
        return query(sql, evalId);
    }

    public RuleEvalResultEntity findByEvalIdAndRuleCode(String evalId, String ruleCode) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT id, eval_id, rule_code, rule_version, patient_id, encounter_id, hit_flag, " +
                "severity, message, actions, evidence, input_snapshot, output_snapshot, elapsed_ms, " +
                "result_status, error_code, error_message, tenant_id, group_code, hospital_code, " +
                "campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time " +
                "FROM re_rule_eval_result WHERE eval_id=? AND rule_code=?";
        List<RuleEvalResultEntity> list = query(sql, evalId, ruleCode);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<RuleEvalResultEntity> findByPatientAndEncounter(String patientId, String encounterId) {
        if (!enabled()) {
            return new ArrayList<RuleEvalResultEntity>();
        }
        String sql = "SELECT id, eval_id, rule_code, rule_version, patient_id, encounter_id, hit_flag, " +
                "severity, message, actions, evidence, input_snapshot, output_snapshot, elapsed_ms, " +
                "result_status, error_code, error_message, tenant_id, group_code, hospital_code, " +
                "campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time " +
                "FROM re_rule_eval_result WHERE patient_id=? AND encounter_id=? ORDER BY created_time DESC";
        return query(sql, patientId, encounterId);
    }

    public List<RuleEvalResultEntity> findByOrgContext(String tenantId, String hospitalCode,
                                                        String scopeLevel, String scopeCode) {
        if (!enabled()) {
            return new ArrayList<RuleEvalResultEntity>();
        }
        String sql = "SELECT id, eval_id, rule_code, rule_version, patient_id, encounter_id, hit_flag, " +
                "severity, message, actions, evidence, input_snapshot, output_snapshot, elapsed_ms, " +
                "result_status, error_code, error_message, tenant_id, group_code, hospital_code, " +
                "campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time " +
                "FROM re_rule_eval_result WHERE tenant_id=? AND hospital_code=? AND scope_level=? AND scope_code=? " +
                "ORDER BY created_time DESC";
        return query(sql, tenantId, hospitalCode, scopeLevel, scopeCode);
    }

    private List<RuleEvalResultEntity> query(String sql, Object... params) {
        List<RuleEvalResultEntity> results = new ArrayList<RuleEvalResultEntity>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            log.error("query rule eval result failed", ex);
            throw new IllegalStateException("query rule eval result failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    private RuleEvalResultEntity mapRow(ResultSet rs) throws SQLException {
        RuleEvalResultEntity entity = new RuleEvalResultEntity();
        entity.setId(rs.getLong("id"));
        entity.setEvalId(rs.getString("eval_id"));
        entity.setRuleCode(rs.getString("rule_code"));
        entity.setRuleVersion(rs.getString("rule_version"));
        entity.setPatientId(rs.getString("patient_id"));
        entity.setEncounterId(rs.getString("encounter_id"));
        // hit_flag 在所有方言下都是数字（0/1）。
        entity.setHit(rs.getInt("hit_flag") == 1);
        entity.setSeverity(rs.getString("severity"));
        entity.setMessage(rs.getString("message"));
        entity.setActionsJson(rs.getString("actions"));
        entity.setEvidenceJson(rs.getString("evidence"));
        entity.setInputSnapshotJson(rs.getString("input_snapshot"));
        entity.setOutputSnapshotJson(rs.getString("output_snapshot"));
        long elapsed = rs.getLong("elapsed_ms");
        entity.setElapsedMs(rs.wasNull() ? null : elapsed);
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
        java.sql.Timestamp ts = rs.getTimestamp("created_time");
        entity.setCreatedTime(ts == null ? null : ts.toString());
        // GA-DATA-01：从数据库读出的 @Encrypted 字段为密文，统一解密回明文。
        fieldEncryption.decryptEntity(entity);
        return entity;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: use the shared HikariCP DataSource from EngineDataSourceConfig.
        return dataSource.getConnection();
    }

    private void loadDriver() throws SQLException {
        String driverClass = properties.localFileDatabase() ? "org.h2.Driver" : "oracle.jdbc.OracleDriver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(driverClass + " not found", ex);
        }
    }
}
