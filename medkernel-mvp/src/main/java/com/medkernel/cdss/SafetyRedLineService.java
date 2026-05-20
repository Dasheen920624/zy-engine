package com.medkernel.cdss;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 医疗安全红线扫描服务（CDSS-004）。
 *
 * <p>对标医疗安全标准：
 * <ul>
 *   <li>红线定义 — 不可违反的医疗安全规则（用药/诊断/操作/路径/AI输出）</li>
 *   <li>红线扫描 — 遍历启用红线规则，评估条件表达式，记录违反结果</li>
 *   <li>阻断策略 — WARN/BLOCK/ESCALATE/REQUIRE_DUAL_CONFIRM</li>
 *   <li>覆盖与解决 — 支持覆盖（需记录原因）和解决流程</li>
 * </ul>
 */
@Service
public class SafetyRedLineService {

    private final EnginePersistenceProperties properties;

    public SafetyRedLineService(EnginePersistenceProperties properties) {
        this.properties = properties;
    }

    // ─── 红线定义管理 ──────────────────────────────────────────────

    /**
     * 定义红线。
     */
    public SafetyRedLine defineRedLine(SafetyRedLine redLine) {
        if (!properties.isEnabled()) {
            return redLine;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO cdss_safety_red_line " +
                    "(id, tenant_id, red_line_code, red_line_name, category, description, " +
                    "condition_expression, blocking_action, severity, applicable_scenarios, " +
                    "enabled, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO cdss_safety_red_line " +
                    "(id, tenant_id, red_line_code, red_line_name, category, description, " +
                    "condition_expression, blocking_action, severity, applicable_scenarios, " +
                    "enabled, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            redLine.setId(id);
            int i = 1;
            ps.setLong(i++, id);
            ps.setObject(i++, redLine.getTenantId());
            ps.setString(i++, redLine.getRedLineCode());
            ps.setString(i++, redLine.getRedLineName());
            ps.setString(i++, redLine.getCategory());
            ps.setString(i++, redLine.getDescription());
            ps.setString(i++, redLine.getConditionExpression());
            ps.setString(i++, redLine.getBlockingAction());
            ps.setString(i++, redLine.getSeverity());
            ps.setString(i++, redLine.getApplicableScenarios());
            ps.setString(i++, redLine.getEnabled() != null ? redLine.getEnabled() : "Y");
            ps.setString(i++, redLine.getCreatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("define red line failed: " + ex.getMessage(), ex);
        }
        return redLine;
    }

    /**
     * 更新红线。
     */
    public SafetyRedLine updateRedLine(SafetyRedLine redLine) {
        if (!properties.isEnabled()) {
            return redLine;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE cdss_safety_red_line SET red_line_name=?, category=?, description=?, " +
                    "condition_expression=?, blocking_action=?, severity=?, applicable_scenarios=?, " +
                    "enabled=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE cdss_safety_red_line SET red_line_name=?, category=?, description=?, " +
                    "condition_expression=?, blocking_action=?, severity=?, applicable_scenarios=?, " +
                    "enabled=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, redLine.getRedLineName());
            ps.setString(i++, redLine.getCategory());
            ps.setString(i++, redLine.getDescription());
            ps.setString(i++, redLine.getConditionExpression());
            ps.setString(i++, redLine.getBlockingAction());
            ps.setString(i++, redLine.getSeverity());
            ps.setString(i++, redLine.getApplicableScenarios());
            ps.setString(i++, redLine.getEnabled());
            ps.setString(i++, redLine.getUpdatedBy());
            ps.setLong(i++, redLine.getId());
            ps.setObject(i++, redLine.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Red line not found: id=" + redLine.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update red line failed: " + ex.getMessage(), ex);
        }
        return redLine;
    }

    /**
     * 查询红线列表。
     */
    public List<SafetyRedLine> listRedLines(Long tenantId, String category, String enabled) {
        List<SafetyRedLine> result = new ArrayList<SafetyRedLine>();
        if (!properties.isEnabled()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, red_line_code, red_line_name, category, description, " +
                        "condition_expression, blocking_action, severity, applicable_scenarios, " +
                        "enabled, created_by, created_time, updated_by, updated_time " +
                        "FROM cdss_safety_red_line WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category=?");
            params.add(category);
        }
        if (enabled != null && !enabled.isEmpty()) {
            sql.append(" AND enabled=?");
            params.add(enabled);
        }
        sql.append(" ORDER BY created_time DESC");
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
                    result.add(toSafetyRedLine(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list red lines failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    // ─── 红线扫描 ──────────────────────────────────────────────────

    /**
     * 执行红线扫描。
     * 遍历所有启用的红线规则，评估条件表达式，记录违反结果。
     */
    public List<RedLineScanResult> scanRedLines(Long tenantId, String patientId,
                                                 String encounterId, String scanType) {
        List<RedLineScanResult> results = new ArrayList<RedLineScanResult>();
        if (!properties.isEnabled()) {
            return results;
        }

        // 获取所有启用的红线规则
        List<SafetyRedLine> redLines = listRedLines(tenantId, null, "Y");
        String scanCode = "SCAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        for (SafetyRedLine redLine : redLines) {
            // 评估条件表达式
            boolean violated = evaluateCondition(redLine.getConditionExpression(), patientId, encounterId);
            if (violated) {
                RedLineScanResult scanResult = new RedLineScanResult();
                scanResult.setTenantId(tenantId);
                scanResult.setScanCode(scanCode);
                scanResult.setScanType(scanType != null ? scanType : "MANUAL");
                scanResult.setRedLineCode(redLine.getRedLineCode());
                scanResult.setRedLineName(redLine.getRedLineName());
                scanResult.setCategory(redLine.getCategory());
                scanResult.setPatientId(patientId);
                scanResult.setEncounterId(encounterId);
                scanResult.setTriggerContext(buildTriggerContext(patientId, encounterId));
                scanResult.setViolationDetail("Red line violated: " + redLine.getRedLineName());
                scanResult.setSeverity(redLine.getSeverity());
                scanResult.setBlockingAction(redLine.getBlockingAction());
                scanResult.setStatus("BLOCKED".equals(redLine.getBlockingAction()) ? "BLOCKED" : "DETECTED");
                scanResult.setScanBy("SYSTEM");
                scanResult.setScanTime(now);

                // 保存扫描结果
                saveScanResult(scanResult);
                results.add(scanResult);
            }
        }
        return results;
    }

    /**
     * 查询扫描结果。
     */
    public List<RedLineScanResult> listScanResults(Long tenantId, String patientId,
                                                    String severity, String status) {
        List<RedLineScanResult> result = new ArrayList<RedLineScanResult>();
        if (!properties.isEnabled()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, scan_code, scan_type, red_line_code, red_line_name, category, " +
                        "patient_id, encounter_id, trigger_context, violation_detail, severity, " +
                        "blocking_action, status, overridden_by, override_reason, " +
                        "resolved_by, resolution_note, resolved_time, scan_by, scan_time, created_time " +
                        "FROM cdss_red_line_scan_result WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (patientId != null && !patientId.isEmpty()) {
            sql.append(" AND patient_id=?");
            params.add(patientId);
        }
        if (severity != null && !severity.isEmpty()) {
            sql.append(" AND severity=?");
            params.add(severity);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status=?");
            params.add(status);
        }
        sql.append(" ORDER BY scan_time DESC");
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
                    result.add(toRedLineScanResult(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list scan results failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 解决扫描结果。
     */
    public RedLineScanResult resolveScanResult(Long resultId, String resolvedBy, String resolutionNote) {
        if (!properties.isEnabled()) {
            RedLineScanResult scanResult = new RedLineScanResult();
            scanResult.setId(resultId);
            scanResult.setResolvedBy(resolvedBy);
            scanResult.setResolutionNote(resolutionNote);
            scanResult.setStatus("RESOLVED");
            return scanResult;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE cdss_red_line_scan_result SET status='RESOLVED', resolved_by=?, " +
                    "resolution_note=?, resolved_time=CURRENT_TIMESTAMP WHERE id=?";
        } else {
            sql = "UPDATE cdss_red_line_scan_result SET status='RESOLVED', resolved_by=?, " +
                    "resolution_note=?, resolved_time=SYSTIMESTAMP WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, resolvedBy);
            ps.setString(2, resolutionNote);
            ps.setLong(3, resultId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Scan result not found: id=" + resultId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("resolve scan result failed: " + ex.getMessage(), ex);
        }
        return getScanResult(resultId);
    }

    /**
     * 覆盖扫描结果。
     */
    public RedLineScanResult overrideScanResult(Long resultId, String overriddenBy, String overrideReason) {
        if (!properties.isEnabled()) {
            RedLineScanResult scanResult = new RedLineScanResult();
            scanResult.setId(resultId);
            scanResult.setOverriddenBy(overriddenBy);
            scanResult.setOverrideReason(overrideReason);
            scanResult.setStatus("OVERRIDDEN");
            return scanResult;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE cdss_red_line_scan_result SET status='OVERRIDDEN', overridden_by=?, " +
                    "override_reason=? WHERE id=?";
        } else {
            sql = "UPDATE cdss_red_line_scan_result SET status='OVERRIDDEN', overridden_by=?, " +
                    "override_reason=? WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, overriddenBy);
            ps.setString(2, overrideReason);
            ps.setLong(3, resultId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Scan result not found: id=" + resultId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("override scan result failed: " + ex.getMessage(), ex);
        }
        return getScanResult(resultId);
    }

    /**
     * 扫描统计。
     */
    public Map<String, Object> getScanSummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenant_id", tenantId);
        summary.put("total_red_lines", 0);
        summary.put("enabled_red_lines", 0);
        summary.put("total_scan_results", 0);
        summary.put("severity_distribution", new LinkedHashMap<String, Integer>());
        summary.put("status_distribution", new LinkedHashMap<String, Integer>());

        if (!properties.isEnabled()) {
            return summary;
        }

        // 红线统计
        String redLineSql = "SELECT enabled, COUNT(*) AS cnt FROM cdss_safety_red_line WHERE tenant_id=? GROUP BY enabled";
        int totalRedLines = 0;
        int enabledRedLines = 0;
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(redLineSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String enabled = rs.getString("enabled");
                    int cnt = rs.getInt("cnt");
                    totalRedLines += cnt;
                    if ("Y".equals(enabled)) {
                        enabledRedLines += cnt;
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get scan summary failed: " + ex.getMessage(), ex);
        }
        summary.put("total_red_lines", totalRedLines);
        summary.put("enabled_red_lines", enabledRedLines);

        // 扫描结果统计
        String resultSql = "SELECT severity, status, COUNT(*) AS cnt FROM cdss_red_line_scan_result WHERE tenant_id=? " +
                "GROUP BY severity, status";
        Map<String, Integer> severityDist = new LinkedHashMap<String, Integer>();
        severityDist.put("HIGH", 0);
        severityDist.put("CRITICAL", 0);
        Map<String, Integer> statusDist = new LinkedHashMap<String, Integer>();
        statusDist.put("DETECTED", 0);
        statusDist.put("BLOCKED", 0);
        statusDist.put("OVERRIDDEN", 0);
        statusDist.put("RESOLVED", 0);
        int totalResults = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(resultSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String severity = rs.getString("severity");
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    totalResults += cnt;
                    if (severity != null && severityDist.containsKey(severity)) {
                        severityDist.put(severity, severityDist.get(severity) + cnt);
                    }
                    if (status != null && statusDist.containsKey(status)) {
                        statusDist.put(status, statusDist.get(status) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get scan summary failed: " + ex.getMessage(), ex);
        }

        summary.put("total_scan_results", totalResults);
        summary.put("severity_distribution", severityDist);
        summary.put("status_distribution", statusDist);
        return summary;
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

    private void saveScanResult(RedLineScanResult scanResult) {
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO cdss_red_line_scan_result " +
                    "(id, tenant_id, scan_code, scan_type, red_line_code, red_line_name, category, " +
                    "patient_id, encounter_id, trigger_context, violation_detail, severity, " +
                    "blocking_action, status, overridden_by, override_reason, " +
                    "resolved_by, resolution_note, resolved_time, scan_by, scan_time, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO cdss_red_line_scan_result " +
                    "(id, tenant_id, scan_code, scan_type, red_line_code, red_line_name, category, " +
                    "patient_id, encounter_id, trigger_context, violation_detail, severity, " +
                    "blocking_action, status, overridden_by, override_reason, " +
                    "resolved_by, resolution_note, resolved_time, scan_by, scan_time, created_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            scanResult.setId(id);
            int i = 1;
            ps.setLong(i++, id);
            ps.setObject(i++, scanResult.getTenantId());
            ps.setString(i++, scanResult.getScanCode());
            ps.setString(i++, scanResult.getScanType());
            ps.setString(i++, scanResult.getRedLineCode());
            ps.setString(i++, scanResult.getRedLineName());
            ps.setString(i++, scanResult.getCategory());
            ps.setString(i++, scanResult.getPatientId());
            ps.setString(i++, scanResult.getEncounterId());
            ps.setString(i++, scanResult.getTriggerContext());
            ps.setString(i++, scanResult.getViolationDetail());
            ps.setString(i++, scanResult.getSeverity());
            ps.setString(i++, scanResult.getBlockingAction());
            ps.setString(i++, scanResult.getStatus());
            ps.setString(i++, scanResult.getOverriddenBy());
            ps.setString(i++, scanResult.getOverrideReason());
            ps.setString(i++, scanResult.getResolvedBy());
            ps.setString(i++, scanResult.getResolutionNote());
            ps.setTimestamp(i++, scanResult.getResolvedTime() != null ? Timestamp.valueOf(scanResult.getResolvedTime()) : null);
            ps.setString(i++, scanResult.getScanBy());
            ps.setTimestamp(i++, scanResult.getScanTime() != null ? Timestamp.valueOf(scanResult.getScanTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save scan result failed: " + ex.getMessage(), ex);
        }
    }

    private RedLineScanResult getScanResult(Long resultId) {
        String sql = "SELECT id, tenant_id, scan_code, scan_type, red_line_code, red_line_name, category, " +
                "patient_id, encounter_id, trigger_context, violation_detail, severity, " +
                "blocking_action, status, overridden_by, override_reason, " +
                "resolved_by, resolution_note, resolved_time, scan_by, scan_time, created_time " +
                "FROM cdss_red_line_scan_result WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, resultId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toRedLineScanResult(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get scan result failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private boolean evaluateCondition(String conditionExpression, String patientId, String encounterId) {
        // 条件表达式评估：当前为简化实现，基于表达式内容进行基本匹配
        // 生产环境应接入规则引擎进行完整评估
        if (conditionExpression == null || conditionExpression.isEmpty()) {
            return false;
        }
        // 简化评估：如果表达式不为空且患者ID不为空，则标记为需要检查
        // 实际项目中应解析表达式并与患者临床数据比对
        return patientId != null && !patientId.isEmpty();
    }

    private String buildTriggerContext(String patientId, String encounterId) {
        StringBuilder sb = new StringBuilder("{\"patientId\":\"");
        sb.append(patientId != null ? patientId : "");
        sb.append("\",\"encounterId\":\"");
        sb.append(encounterId != null ? encounterId : "");
        sb.append("\"}");
        return sb.toString();
    }

    private SafetyRedLine toSafetyRedLine(ResultSet rs) throws SQLException {
        SafetyRedLine redLine = new SafetyRedLine();
        redLine.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        redLine.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        redLine.setRedLineCode(rs.getString("red_line_code"));
        redLine.setRedLineName(rs.getString("red_line_name"));
        redLine.setCategory(rs.getString("category"));
        redLine.setDescription(rs.getString("description"));
        redLine.setConditionExpression(rs.getString("condition_expression"));
        redLine.setBlockingAction(rs.getString("blocking_action"));
        redLine.setSeverity(rs.getString("severity"));
        redLine.setApplicableScenarios(rs.getString("applicable_scenarios"));
        redLine.setEnabled(rs.getString("enabled"));
        redLine.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        redLine.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        redLine.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        redLine.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return redLine;
    }

    private RedLineScanResult toRedLineScanResult(ResultSet rs) throws SQLException {
        RedLineScanResult scanResult = new RedLineScanResult();
        scanResult.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        scanResult.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        scanResult.setScanCode(rs.getString("scan_code"));
        scanResult.setScanType(rs.getString("scan_type"));
        scanResult.setRedLineCode(rs.getString("red_line_code"));
        scanResult.setRedLineName(rs.getString("red_line_name"));
        scanResult.setCategory(rs.getString("category"));
        scanResult.setPatientId(rs.getString("patient_id"));
        scanResult.setEncounterId(rs.getString("encounter_id"));
        scanResult.setTriggerContext(rs.getString("trigger_context"));
        scanResult.setViolationDetail(rs.getString("violation_detail"));
        scanResult.setSeverity(rs.getString("severity"));
        scanResult.setBlockingAction(rs.getString("blocking_action"));
        scanResult.setStatus(rs.getString("status"));
        scanResult.setOverriddenBy(rs.getString("overridden_by"));
        scanResult.setOverrideReason(rs.getString("override_reason"));
        scanResult.setResolvedBy(rs.getString("resolved_by"));
        scanResult.setResolutionNote(rs.getString("resolution_note"));
        Timestamp resolvedTime = rs.getTimestamp("resolved_time");
        scanResult.setResolvedTime(resolvedTime != null ? resolvedTime.toLocalDateTime() : null);
        scanResult.setScanBy(rs.getString("scan_by"));
        Timestamp scanTime = rs.getTimestamp("scan_time");
        scanResult.setScanTime(scanTime != null ? scanTime.toLocalDateTime() : null);
        Timestamp createdTime = rs.getTimestamp("created_time");
        scanResult.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return scanResult;
    }

    private Connection connection() throws SQLException {
        loadDriver();
        SQLException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
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
