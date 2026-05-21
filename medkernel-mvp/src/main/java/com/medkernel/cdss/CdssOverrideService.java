package com.medkernel.cdss;

import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CDSS 覆盖治理服务。
 *
 * <p>对标 CDSS 标准：
 * <ul>
 *   <li>覆盖审计 — 每次医生覆盖/确认操作记录到 cdss_override_log 表</li>
 *   <li>疲劳治理 — 当医生在时间窗口内覆盖次数超过阈值时触发抑制</li>
 *   <li>疲劳配置 — 支持按规则/科室配置疲劳策略</li>
 * </ul>
 */
@Service
public class CdssOverrideService {

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public CdssOverrideService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    /**
     * 记录覆盖日志到 cdss_override_log 表。
     */
    public CdssOverrideLog recordOverride(CdssOverrideLog log) {
        if (!properties.isEnabled()) {
            return log;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO cdss_override_log " +
                    "(id, tenant_id, alert_id, trigger_code, rule_code, risk_level, alert_level, " +
                    "override_type, override_reason, override_category, supervisor_name, confirmed_by, " +
                    "patient_id, encounter_id, operator_id, department_code, is_audit_red_line, " +
                    "fatigue_suppressed, override_time, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO cdss_override_log " +
                    "(id, tenant_id, alert_id, trigger_code, rule_code, risk_level, alert_level, " +
                    "override_type, override_reason, override_category, supervisor_name, confirmed_by, " +
                    "patient_id, encounter_id, operator_id, department_code, is_audit_red_line, " +
                    "fatigue_suppressed, override_time, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            log.setId(id);
            int i = 1;
            ps.setLong(i++, id);
            ps.setObject(i++, log.getTenantId());
            ps.setString(i++, log.getAlertId());
            ps.setString(i++, log.getTriggerCode());
            ps.setString(i++, log.getRuleCode());
            ps.setString(i++, log.getRiskLevel());
            ps.setString(i++, log.getAlertLevel());
            ps.setString(i++, log.getOverrideType());
            ps.setString(i++, log.getOverrideReason());
            ps.setString(i++, log.getOverrideCategory());
            ps.setString(i++, log.getSupervisorName());
            ps.setString(i++, log.getConfirmedBy());
            ps.setString(i++, log.getPatientId());
            ps.setString(i++, log.getEncounterId());
            ps.setString(i++, log.getOperatorId());
            ps.setString(i++, log.getDepartmentCode());
            ps.setString(i++, log.getIsAuditRedLine());
            ps.setString(i++, log.getFatigueSuppressed());
            ps.setTimestamp(i++, log.getOverrideTime() != null
                    ? Timestamp.valueOf(log.getOverrideTime()) : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("record override log failed: " + ex.getMessage(), ex);
        }
        return log;
    }

    /**
     * 查询覆盖日志。
     *
     * @param tenantId   租户ID
     * @param patientId  患者ID（可选）
     * @param operatorId 操作人ID（可选）
     * @param limit      返回条数上限
     * @return 覆盖日志列表
     */
    public List<CdssOverrideLog> listOverrides(Long tenantId, String patientId, String operatorId, int limit) {
        List<CdssOverrideLog> result = new ArrayList<CdssOverrideLog>();
        if (!properties.isEnabled()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, alert_id, trigger_code, rule_code, risk_level, alert_level, " +
                        "override_type, override_reason, override_category, supervisor_name, confirmed_by, " +
                        "patient_id, encounter_id, operator_id, department_code, is_audit_red_line, " +
                        "fatigue_suppressed, override_time, created_time " +
                        "FROM cdss_override_log WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (patientId != null && !patientId.isEmpty()) {
            sql.append(" AND patient_id=?");
            params.add(patientId);
        }
        if (operatorId != null && !operatorId.isEmpty()) {
            sql.append(" AND operator_id=?");
            params.add(operatorId);
        }
        sql.append(" ORDER BY created_time DESC");
        if (limit > 0) {
            sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int idx = 0; idx < params.size(); idx++) {
                Object param = params.get(idx);
                if (param instanceof Long) {
                    ps.setLong(idx + 1, (Long) param);
                } else {
                    ps.setString(idx + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(toOverrideLog(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list overrides failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 统计时间窗口内覆盖次数。
     *
     * @param tenantId   租户ID
     * @param operatorId 操作人ID
     * @param ruleCode   规则编码（可选）
     * @param hours      时间窗口（小时）
     * @return 统计结果
     */
    public Map<String, Object> getOverrideStatistics(Long tenantId, String operatorId, String ruleCode, int hours) {
        Map<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("tenant_id", tenantId);
        stats.put("operator_id", operatorId);
        stats.put("rule_code", ruleCode);
        stats.put("time_window_hours", hours);
        stats.put("override_count", 0);

        if (!properties.isEnabled()) {
            return stats;
        }

        String timeExpr = properties.localFileDatabase()
                ? "CURRENT_TIMESTAMP - INTERVAL '" + hours + " HOUR'"
                : "SYSTIMESTAMP - INTERVAL '" + hours + "' HOUR";

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) AS cnt FROM cdss_override_log " +
                        "WHERE tenant_id=? AND override_time >= " + timeExpr);
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (operatorId != null && !operatorId.isEmpty()) {
            sql.append(" AND operator_id=?");
            params.add(operatorId);
        }
        if (ruleCode != null && !ruleCode.isEmpty()) {
            sql.append(" AND rule_code=?");
            params.add(ruleCode);
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int idx = 0; idx < params.size(); idx++) {
                Object param = params.get(idx);
                if (param instanceof Long) {
                    ps.setLong(idx + 1, (Long) param);
                } else {
                    ps.setString(idx + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("override_count", rs.getInt("cnt"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get override statistics failed: " + ex.getMessage(), ex);
        }
        return stats;
    }

    /**
     * 检查是否触发疲劳抑制。
     * 根据疲劳配置，判断指定操作人对指定规则的覆盖是否超过阈值。
     *
     * @param tenantId   租户ID
     * @param operatorId 操作人ID
     * @param ruleCode   规则编码
     * @return 疲劳检查结果，包含是否抑制、匹配的配置、当前计数等
     */
    public Map<String, Object> checkFatigue(Long tenantId, String operatorId, String ruleCode) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("fatigue_triggered", false);
        result.put("operator_id", operatorId);
        result.put("rule_code", ruleCode);

        if (!properties.isEnabled()) {
            return result;
        }

        // 查找匹配的疲劳配置：优先规则+科室匹配，其次规则匹配，再次全局配置
        CdssFatigueConfig config = findFatigueConfig(tenantId, ruleCode, null);
        if (config == null) {
            return result;
        }

        if (!"TRUE".equalsIgnoreCase(config.getEnabled())) {
            return result;
        }

        // 统计时间窗口内覆盖次数
        Map<String, Object> stats = getOverrideStatistics(tenantId, operatorId, ruleCode,
                config.getTimeWindowHours());
        int overrideCount = (Integer) stats.get("override_count");

        boolean fatigueTriggered = overrideCount >= config.getOverrideThreshold();
        result.put("fatigue_triggered", fatigueTriggered);
        result.put("override_count", overrideCount);
        result.put("threshold", config.getOverrideThreshold());
        result.put("time_window_hours", config.getTimeWindowHours());
        result.put("suppress_action", config.getSuppressAction());
        result.put("suppress_level", config.getSuppressLevel());
        result.put("config_code", config.getConfigCode());

        return result;
    }

    /**
     * 保存疲劳配置。
     */
    public CdssFatigueConfig saveFatigueConfig(CdssFatigueConfig config) {
        if (!properties.isEnabled()) {
            return config;
        }
        String updateSql;
        String insertSql;
        if (properties.localFileDatabase()) {
            updateSql = "UPDATE cdss_fatigue_config SET config_name=?, rule_code=?, department_code=?, " +
                    "time_window_hours=?, override_threshold=?, suppress_action=?, suppress_level=?, " +
                    "enabled=?, description=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE tenant_id=? AND config_code=?";
            insertSql = "INSERT INTO cdss_fatigue_config " +
                    "(id, tenant_id, config_code, config_name, rule_code, department_code, " +
                    "time_window_hours, override_threshold, suppress_action, suppress_level, " +
                    "enabled, description, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        } else {
            updateSql = "UPDATE cdss_fatigue_config SET config_name=?, rule_code=?, department_code=?, " +
                    "time_window_hours=?, override_threshold=?, suppress_action=?, suppress_level=?, " +
                    "enabled=?, description=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE tenant_id=? AND config_code=?";
            insertSql = "INSERT INTO cdss_fatigue_config " +
                    "(id, tenant_id, config_code, config_name, rule_code, department_code, " +
                    "time_window_hours, override_threshold, suppress_action, suppress_level, " +
                    "enabled, description, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        }
        try (Connection connection = connection()) {
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, config.getConfigName());
                ps.setString(i++, config.getRuleCode());
                ps.setString(i++, config.getDepartmentCode());
                ps.setInt(i++, config.getTimeWindowHours());
                ps.setInt(i++, config.getOverrideThreshold());
                ps.setString(i++, config.getSuppressAction());
                ps.setString(i++, config.getSuppressLevel());
                ps.setString(i++, config.getEnabled());
                ps.setString(i++, config.getDescription());
                ps.setString(i++, config.getUpdatedBy());
                ps.setObject(i++, config.getTenantId());
                ps.setString(i++, config.getConfigCode());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                long id = Ids.next();
                config.setId(id);
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, id);
                    ips.setObject(i++, config.getTenantId());
                    ips.setString(i++, config.getConfigCode());
                    ips.setString(i++, config.getConfigName());
                    ips.setString(i++, config.getRuleCode());
                    ips.setString(i++, config.getDepartmentCode());
                    ips.setInt(i++, config.getTimeWindowHours());
                    ips.setInt(i++, config.getOverrideThreshold());
                    ips.setString(i++, config.getSuppressAction());
                    ips.setString(i++, config.getSuppressLevel());
                    ips.setString(i++, config.getEnabled());
                    ips.setString(i++, config.getDescription());
                    ips.setString(i++, config.getCreatedBy());
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save fatigue config failed: " + ex.getMessage(), ex);
        }
        return config;
    }

    /**
     * 查询疲劳配置列表。
     */
    public List<CdssFatigueConfig> listFatigueConfigs(Long tenantId) {
        List<CdssFatigueConfig> result = new ArrayList<CdssFatigueConfig>();
        if (!properties.isEnabled()) {
            return result;
        }
        String sql = "SELECT id, tenant_id, config_code, config_name, rule_code, department_code, " +
                "time_window_hours, override_threshold, suppress_action, suppress_level, " +
                "enabled, description, created_by, created_time, updated_by, updated_time " +
                "FROM cdss_fatigue_config WHERE tenant_id=? ORDER BY created_time";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(toFatigueConfig(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list fatigue configs failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 更新疲劳配置。
     */
    public CdssFatigueConfig updateFatigueConfig(CdssFatigueConfig config) {
        if (!properties.isEnabled()) {
            return config;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE cdss_fatigue_config SET config_name=?, rule_code=?, department_code=?, " +
                    "time_window_hours=?, override_threshold=?, suppress_action=?, suppress_level=?, " +
                    "enabled=?, description=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE cdss_fatigue_config SET config_name=?, rule_code=?, department_code=?, " +
                    "time_window_hours=?, override_threshold=?, suppress_action=?, suppress_level=?, " +
                    "enabled=?, description=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, config.getConfigName());
            ps.setString(i++, config.getRuleCode());
            ps.setString(i++, config.getDepartmentCode());
            ps.setInt(i++, config.getTimeWindowHours());
            ps.setInt(i++, config.getOverrideThreshold());
            ps.setString(i++, config.getSuppressAction());
            ps.setString(i++, config.getSuppressLevel());
            ps.setString(i++, config.getEnabled());
            ps.setString(i++, config.getDescription());
            ps.setString(i++, config.getUpdatedBy());
            ps.setLong(i++, config.getId());
            ps.setObject(i++, config.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Fatigue config not found: id=" + config.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update fatigue config failed: " + ex.getMessage(), ex);
        }
        return config;
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

    /**
     * 查找匹配的疲劳配置。
     * 优先级：规则+科室 > 规则 > 全局
     */
    private CdssFatigueConfig findFatigueConfig(Long tenantId, String ruleCode, String departmentCode) {
        List<CdssFatigueConfig> configs = listFatigueConfigs(tenantId);

        // 优先匹配：规则+科室
        if (ruleCode != null && departmentCode != null) {
            for (CdssFatigueConfig config : configs) {
                if (ruleCode.equals(config.getRuleCode())
                        && departmentCode.equals(config.getDepartmentCode())
                        && "TRUE".equalsIgnoreCase(config.getEnabled())) {
                    return config;
                }
            }
        }

        // 其次匹配：规则
        if (ruleCode != null) {
            for (CdssFatigueConfig config : configs) {
                if (ruleCode.equals(config.getRuleCode())
                        && (config.getDepartmentCode() == null || config.getDepartmentCode().isEmpty())
                        && "TRUE".equalsIgnoreCase(config.getEnabled())) {
                    return config;
                }
            }
        }

        // 最后匹配：全局配置
        for (CdssFatigueConfig config : configs) {
            if ((config.getRuleCode() == null || config.getRuleCode().isEmpty())
                    && (config.getDepartmentCode() == null || config.getDepartmentCode().isEmpty())
                    && "TRUE".equalsIgnoreCase(config.getEnabled())) {
                return config;
            }
        }

        return null;
    }

    private CdssOverrideLog toOverrideLog(ResultSet rs) throws SQLException {
        CdssOverrideLog log = new CdssOverrideLog();
        log.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        log.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        log.setAlertId(rs.getString("alert_id"));
        log.setTriggerCode(rs.getString("trigger_code"));
        log.setRuleCode(rs.getString("rule_code"));
        log.setRiskLevel(rs.getString("risk_level"));
        log.setAlertLevel(rs.getString("alert_level"));
        log.setOverrideType(rs.getString("override_type"));
        log.setOverrideReason(rs.getString("override_reason"));
        log.setOverrideCategory(rs.getString("override_category"));
        log.setSupervisorName(rs.getString("supervisor_name"));
        log.setConfirmedBy(rs.getString("confirmed_by"));
        log.setPatientId(rs.getString("patient_id"));
        log.setEncounterId(rs.getString("encounter_id"));
        log.setOperatorId(rs.getString("operator_id"));
        log.setDepartmentCode(rs.getString("department_code"));
        log.setIsAuditRedLine(rs.getString("is_audit_red_line"));
        log.setFatigueSuppressed(rs.getString("fatigue_suppressed"));
        Timestamp overrideTime = rs.getTimestamp("override_time");
        log.setOverrideTime(overrideTime != null ? overrideTime.toLocalDateTime() : null);
        Timestamp createdTime = rs.getTimestamp("created_time");
        log.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return log;
    }

    private CdssFatigueConfig toFatigueConfig(ResultSet rs) throws SQLException {
        CdssFatigueConfig config = new CdssFatigueConfig();
        config.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        config.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        config.setConfigCode(rs.getString("config_code"));
        config.setConfigName(rs.getString("config_name"));
        config.setRuleCode(rs.getString("rule_code"));
        config.setDepartmentCode(rs.getString("department_code"));
        config.setTimeWindowHours(rs.getInt("time_window_hours"));
        config.setOverrideThreshold(rs.getInt("override_threshold"));
        config.setSuppressAction(rs.getString("suppress_action"));
        config.setSuppressLevel(rs.getString("suppress_level"));
        config.setEnabled(rs.getString("enabled"));
        config.setDescription(rs.getString("description"));
        config.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        config.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        config.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        config.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return config;
    }

    private Connection connection() throws SQLException {
        // PR-FINAL-15b: 璧?HikariCP 杩炴帴姹狅紙EngineDataSourceConfig 鏆撮湶鐨?DataSource锛夈€?        return dataSource.getConnection();
    } catch (SQLException ex) {
                last = ex;
                if (!shouldRetryConnection(ex) || attempt == 3) {
                    throw ex;
                }
                sleepQuietly(500L * attempt);
            }
        }
        throw last;
    }

    private void loadDriver() throws SQLException {
        String driverClass = properties.localFileDatabase() ? "org.h2.Driver" : "oracle.jdbc.OracleDriver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(driverClass + " not found", ex);
        }
    }

    private boolean shouldRetryConnection(SQLException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("ORA-12518") || message.contains("Listener refused"));
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
