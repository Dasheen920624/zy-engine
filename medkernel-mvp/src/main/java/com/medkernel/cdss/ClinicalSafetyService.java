package com.medkernel.cdss;

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
 * 临床安全风险管理服务（RISK-001）。
 *
 * <p>对标临床安全标准：
 * <ul>
 *   <li>危险日志 — 识别、分析、控制、接受、关闭全生命周期</li>
 *   <li>安全案例 — 结构化论证系统安全性（GSN 标准）</li>
 *   <li>风险评估 — 5×5 风险矩阵（可能性 × 严重性）</li>
 *   <li>阻断策略 — 根据风险等级映射阻断策略</li>
 * </ul>
 */
@Service
public class ClinicalSafetyService {

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public ClinicalSafetyService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // ─── HazardLog 管理 ──────────────────────────────────────────────

    /**
     * 创建危险日志。
     */
    public HazardLog createHazard(HazardLog hazard) {
        if (!properties.isEnabled()) {
            return hazard;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO hazard_log " +
                    "(id, tenant_id, hazard_code, hazard_name, hazard_category, hazard_description, " +
                    "affected_process, likelihood, severity, risk_level, control_measures, " +
                    "residual_risk, status, blocking_strategy, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO hazard_log " +
                    "(id, tenant_id, hazard_code, hazard_name, hazard_category, hazard_description, " +
                    "affected_process, likelihood, severity, risk_level, control_measures, " +
                    "residual_risk, status, blocking_strategy, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            hazard.setId(id);
            int i = 1;
            ps.setLong(i++, id);
            ps.setObject(i++, hazard.getTenantId());
            ps.setString(i++, hazard.getHazardCode());
            ps.setString(i++, hazard.getHazardName());
            ps.setString(i++, hazard.getHazardCategory());
            ps.setString(i++, hazard.getHazardDescription());
            ps.setString(i++, hazard.getAffectedProcess());
            ps.setString(i++, hazard.getLikelihood());
            ps.setString(i++, hazard.getSeverity());
            ps.setString(i++, hazard.getRiskLevel());
            ps.setString(i++, hazard.getControlMeasures());
            ps.setString(i++, hazard.getResidualRisk());
            ps.setString(i++, hazard.getStatus() != null ? hazard.getStatus() : "IDENTIFIED");
            ps.setString(i++, hazard.getBlockingStrategy());
            ps.setString(i++, hazard.getCreatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create hazard failed: " + ex.getMessage(), ex);
        }
        return hazard;
    }

    /**
     * 更新危险日志。
     */
    public HazardLog updateHazard(HazardLog hazard) {
        if (!properties.isEnabled()) {
            return hazard;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE hazard_log SET hazard_name=?, hazard_category=?, hazard_description=?, " +
                    "affected_process=?, likelihood=?, severity=?, risk_level=?, control_measures=?, " +
                    "residual_risk=?, status=?, blocking_strategy=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE hazard_log SET hazard_name=?, hazard_category=?, hazard_description=?, " +
                    "affected_process=?, likelihood=?, severity=?, risk_level=?, control_measures=?, " +
                    "residual_risk=?, status=?, blocking_strategy=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, hazard.getHazardName());
            ps.setString(i++, hazard.getHazardCategory());
            ps.setString(i++, hazard.getHazardDescription());
            ps.setString(i++, hazard.getAffectedProcess());
            ps.setString(i++, hazard.getLikelihood());
            ps.setString(i++, hazard.getSeverity());
            ps.setString(i++, hazard.getRiskLevel());
            ps.setString(i++, hazard.getControlMeasures());
            ps.setString(i++, hazard.getResidualRisk());
            ps.setString(i++, hazard.getStatus());
            ps.setString(i++, hazard.getBlockingStrategy());
            ps.setString(i++, hazard.getUpdatedBy());
            ps.setLong(i++, hazard.getId());
            ps.setObject(i++, hazard.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Hazard not found: id=" + hazard.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update hazard failed: " + ex.getMessage(), ex);
        }
        return hazard;
    }

    /**
     * 查询危险日志。
     */
    public List<HazardLog> listHazards(Long tenantId, String hazardCategory, String riskLevel, String status) {
        List<HazardLog> result = new ArrayList<HazardLog>();
        if (!properties.isEnabled()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, hazard_code, hazard_name, hazard_category, hazard_description, " +
                        "affected_process, likelihood, severity, risk_level, control_measures, " +
                        "residual_risk, status, accepted_by, accepted_time, acceptance_note, " +
                        "blocking_strategy, created_by, created_time, updated_by, updated_time " +
                        "FROM hazard_log WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (hazardCategory != null && !hazardCategory.isEmpty()) {
            sql.append(" AND hazard_category=?");
            params.add(hazardCategory);
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            sql.append(" AND risk_level=?");
            params.add(riskLevel);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status=?");
            params.add(status);
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
                    result.add(toHazardLog(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list hazards failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 院方风险接受。
     */
    public HazardLog acceptHazard(Long hazardId, String acceptedBy, String acceptanceNote) {
        if (!properties.isEnabled()) {
            HazardLog hazard = new HazardLog();
            hazard.setId(hazardId);
            hazard.setAcceptedBy(acceptedBy);
            hazard.setAcceptanceNote(acceptanceNote);
            return hazard;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE hazard_log SET status='ACCEPTED', accepted_by=?, accepted_time=CURRENT_TIMESTAMP, " +
                    "acceptance_note=?, updated_time=CURRENT_TIMESTAMP WHERE id=?";
        } else {
            sql = "UPDATE hazard_log SET status='ACCEPTED', accepted_by=?, accepted_time=SYSTIMESTAMP, " +
                    "acceptance_note=?, updated_time=SYSTIMESTAMP WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, acceptedBy);
            ps.setString(2, acceptanceNote);
            ps.setLong(3, hazardId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Hazard not found: id=" + hazardId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("accept hazard failed: " + ex.getMessage(), ex);
        }
        return getHazard(hazardId);
    }

    /**
     * 关闭危险日志。
     */
    public HazardLog closeHazard(Long hazardId) {
        if (!properties.isEnabled()) {
            HazardLog hazard = new HazardLog();
            hazard.setId(hazardId);
            hazard.setStatus("CLOSED");
            return hazard;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE hazard_log SET status='CLOSED', updated_time=CURRENT_TIMESTAMP WHERE id=?";
        } else {
            sql = "UPDATE hazard_log SET status='CLOSED', updated_time=SYSTIMESTAMP WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, hazardId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Hazard not found: id=" + hazardId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("close hazard failed: " + ex.getMessage(), ex);
        }
        return getHazard(hazardId);
    }

    // ─── SafetyCase 管理 ─────────────────────────────────────────────

    /**
     * 创建安全案例。
     */
    public SafetyCase createSafetyCase(SafetyCase safetyCase) {
        if (!properties.isEnabled()) {
            return safetyCase;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO safety_case " +
                    "(id, tenant_id, case_code, case_name, case_type, scope, goal, argument, " +
                    "evidence_refs, status, version, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO safety_case " +
                    "(id, tenant_id, case_code, case_name, case_type, scope, goal, argument, " +
                    "evidence_refs, status, version, created_by, created_time, updated_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            safetyCase.setId(id);
            int i = 1;
            ps.setLong(i++, id);
            ps.setObject(i++, safetyCase.getTenantId());
            ps.setString(i++, safetyCase.getCaseCode());
            ps.setString(i++, safetyCase.getCaseName());
            ps.setString(i++, safetyCase.getCaseType());
            ps.setString(i++, safetyCase.getScope());
            ps.setString(i++, safetyCase.getGoal());
            ps.setString(i++, safetyCase.getArgument());
            ps.setString(i++, safetyCase.getEvidenceRefs());
            ps.setString(i++, safetyCase.getStatus() != null ? safetyCase.getStatus() : "DRAFT");
            ps.setString(i++, safetyCase.getVersion());
            ps.setString(i++, safetyCase.getCreatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("create safety case failed: " + ex.getMessage(), ex);
        }
        return safetyCase;
    }

    /**
     * 更新安全案例。
     */
    public SafetyCase updateSafetyCase(SafetyCase safetyCase) {
        if (!properties.isEnabled()) {
            return safetyCase;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE safety_case SET case_name=?, case_type=?, scope=?, goal=?, argument=?, " +
                    "evidence_refs=?, status=?, version=?, updated_by=?, updated_time=CURRENT_TIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE safety_case SET case_name=?, case_type=?, scope=?, goal=?, argument=?, " +
                    "evidence_refs=?, status=?, version=?, updated_by=?, updated_time=SYSTIMESTAMP " +
                    "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, safetyCase.getCaseName());
            ps.setString(i++, safetyCase.getCaseType());
            ps.setString(i++, safetyCase.getScope());
            ps.setString(i++, safetyCase.getGoal());
            ps.setString(i++, safetyCase.getArgument());
            ps.setString(i++, safetyCase.getEvidenceRefs());
            ps.setString(i++, safetyCase.getStatus());
            ps.setString(i++, safetyCase.getVersion());
            ps.setString(i++, safetyCase.getUpdatedBy());
            ps.setLong(i++, safetyCase.getId());
            ps.setObject(i++, safetyCase.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Safety case not found: id=" + safetyCase.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("update safety case failed: " + ex.getMessage(), ex);
        }
        return safetyCase;
    }

    /**
     * 查询安全案例。
     */
    public List<SafetyCase> listSafetyCases(Long tenantId, String caseType, String status) {
        List<SafetyCase> result = new ArrayList<SafetyCase>();
        if (!properties.isEnabled()) {
            return result;
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, case_code, case_name, case_type, scope, goal, argument, " +
                        "evidence_refs, status, reviewed_by, reviewed_time, review_note, " +
                        "version, created_by, created_time, updated_by, updated_time " +
                        "FROM safety_case WHERE tenant_id=?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (caseType != null && !caseType.isEmpty()) {
            sql.append(" AND case_type=?");
            params.add(caseType);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status=?");
            params.add(status);
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
                    result.add(toSafetyCase(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list safety cases failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 审核安全案例。
     */
    public SafetyCase reviewSafetyCase(Long caseId, String reviewStatus, String reviewedBy, String reviewNote) {
        if (!properties.isEnabled()) {
            SafetyCase safetyCase = new SafetyCase();
            safetyCase.setId(caseId);
            safetyCase.setStatus(reviewStatus);
            safetyCase.setReviewedBy(reviewedBy);
            safetyCase.setReviewNote(reviewNote);
            return safetyCase;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE safety_case SET status=?, reviewed_by=?, reviewed_time=CURRENT_TIMESTAMP, " +
                    "review_note=?, updated_time=CURRENT_TIMESTAMP WHERE id=?";
        } else {
            sql = "UPDATE safety_case SET status=?, reviewed_by=?, reviewed_time=SYSTIMESTAMP, " +
                    "review_note=?, updated_time=SYSTIMESTAMP WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reviewStatus);
            ps.setString(2, reviewedBy);
            ps.setString(3, reviewNote);
            ps.setLong(4, caseId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Safety case not found: id=" + caseId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("review safety case failed: " + ex.getMessage(), ex);
        }
        return getSafetyCase(caseId);
    }

    // ─── 风险评估 ────────────────────────────────────────────────────

    /**
     * 根据可能性和严重性计算风险等级。
     * 5×5 风险矩阵：可能性（1-5）× 严重性（1-5）
     * LOW: 1-4, MEDIUM: 5-9, HIGH: 10-16, CRITICAL: 17-25
     */
    public String calculateRiskLevel(String likelihood, String severity) {
        int likelihoodScore = toLikelihoodScore(likelihood);
        int severityScore = toSeverityScore(severity);
        int score = likelihoodScore * severityScore;
        if (score >= 17) {
            return "CRITICAL";
        } else if (score >= 10) {
            return "HIGH";
        } else if (score >= 5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * 获取风险摘要统计。
     */
    public Map<String, Object> getRiskSummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenant_id", tenantId);
        summary.put("total_hazards", 0);
        summary.put("risk_level_distribution", new LinkedHashMap<String, Integer>());
        summary.put("status_distribution", new LinkedHashMap<String, Integer>());
        summary.put("total_safety_cases", 0);
        summary.put("safety_case_status_distribution", new LinkedHashMap<String, Integer>());

        if (!properties.isEnabled()) {
            return summary;
        }

        // 危险日志统计
        String hazardSql = "SELECT risk_level, status, COUNT(*) AS cnt FROM hazard_log WHERE tenant_id=? " +
                "GROUP BY risk_level, status";
        Map<String, Integer> riskDist = new LinkedHashMap<String, Integer>();
        riskDist.put("LOW", 0);
        riskDist.put("MEDIUM", 0);
        riskDist.put("HIGH", 0);
        riskDist.put("CRITICAL", 0);
        Map<String, Integer> statusDist = new LinkedHashMap<String, Integer>();
        statusDist.put("IDENTIFIED", 0);
        statusDist.put("ANALYZED", 0);
        statusDist.put("CONTROLLED", 0);
        statusDist.put("ACCEPTED", 0);
        statusDist.put("CLOSED", 0);
        int totalHazards = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(hazardSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String riskLevel = rs.getString("risk_level");
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    totalHazards += cnt;
                    if (riskLevel != null && riskDist.containsKey(riskLevel)) {
                        riskDist.put(riskLevel, riskDist.get(riskLevel) + cnt);
                    }
                    if (status != null && statusDist.containsKey(status)) {
                        statusDist.put(status, statusDist.get(status) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get risk summary failed: " + ex.getMessage(), ex);
        }

        summary.put("total_hazards", totalHazards);
        summary.put("risk_level_distribution", riskDist);
        summary.put("status_distribution", statusDist);

        // 安全案例统计
        String caseSql = "SELECT status, COUNT(*) AS cnt FROM safety_case WHERE tenant_id=? GROUP BY status";
        Map<String, Integer> caseStatusDist = new LinkedHashMap<String, Integer>();
        caseStatusDist.put("DRAFT", 0);
        caseStatusDist.put("UNDER_REVIEW", 0);
        caseStatusDist.put("APPROVED", 0);
        caseStatusDist.put("REVISED", 0);
        caseStatusDist.put("SUPERSEDED", 0);
        int totalCases = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(caseSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    totalCases += cnt;
                    if (status != null && caseStatusDist.containsKey(status)) {
                        caseStatusDist.put(status, caseStatusDist.get(status) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get risk summary failed: " + ex.getMessage(), ex);
        }

        summary.put("total_safety_cases", totalCases);
        summary.put("safety_case_status_distribution", caseStatusDist);
        return summary;
    }

    /**
     * 获取阻断策略。
     */
    public String getBlockingStrategy(String riskLevel) {
        if (riskLevel == null) {
            return "WARN";
        }
        switch (riskLevel) {
            case "CRITICAL":
                return "BLOCK";
            case "HIGH":
                return "ESCALATE";
            case "MEDIUM":
                return "REQUIRE_DUAL_CONFIRM";
            case "LOW":
            default:
                return "WARN";
        }
    }

    // ─── 内部方法 ────────────────────────────────────────────────────

    private HazardLog getHazard(Long hazardId) {
        String sql = "SELECT id, tenant_id, hazard_code, hazard_name, hazard_category, hazard_description, " +
                "affected_process, likelihood, severity, risk_level, control_measures, " +
                "residual_risk, status, accepted_by, accepted_time, acceptance_note, " +
                "blocking_strategy, created_by, created_time, updated_by, updated_time " +
                "FROM hazard_log WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, hazardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toHazardLog(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get hazard failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private SafetyCase getSafetyCase(Long caseId) {
        String sql = "SELECT id, tenant_id, case_code, case_name, case_type, scope, goal, argument, " +
                "evidence_refs, status, reviewed_by, reviewed_time, review_note, " +
                "version, created_by, created_time, updated_by, updated_time " +
                "FROM safety_case WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, caseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toSafetyCase(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("get safety case failed: " + ex.getMessage(), ex);
        }
        return null;
    }

    private HazardLog toHazardLog(ResultSet rs) throws SQLException {
        HazardLog hazard = new HazardLog();
        hazard.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        hazard.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        hazard.setHazardCode(rs.getString("hazard_code"));
        hazard.setHazardName(rs.getString("hazard_name"));
        hazard.setHazardCategory(rs.getString("hazard_category"));
        hazard.setHazardDescription(rs.getString("hazard_description"));
        hazard.setAffectedProcess(rs.getString("affected_process"));
        hazard.setLikelihood(rs.getString("likelihood"));
        hazard.setSeverity(rs.getString("severity"));
        hazard.setRiskLevel(rs.getString("risk_level"));
        hazard.setControlMeasures(rs.getString("control_measures"));
        hazard.setResidualRisk(rs.getString("residual_risk"));
        hazard.setStatus(rs.getString("status"));
        hazard.setAcceptedBy(rs.getString("accepted_by"));
        Timestamp acceptedTime = rs.getTimestamp("accepted_time");
        hazard.setAcceptedTime(acceptedTime != null ? acceptedTime.toLocalDateTime() : null);
        hazard.setAcceptanceNote(rs.getString("acceptance_note"));
        hazard.setBlockingStrategy(rs.getString("blocking_strategy"));
        hazard.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        hazard.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        hazard.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        hazard.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return hazard;
    }

    private SafetyCase toSafetyCase(ResultSet rs) throws SQLException {
        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        safetyCase.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        safetyCase.setCaseCode(rs.getString("case_code"));
        safetyCase.setCaseName(rs.getString("case_name"));
        safetyCase.setCaseType(rs.getString("case_type"));
        safetyCase.setScope(rs.getString("scope"));
        safetyCase.setGoal(rs.getString("goal"));
        safetyCase.setArgument(rs.getString("argument"));
        safetyCase.setEvidenceRefs(rs.getString("evidence_refs"));
        safetyCase.setStatus(rs.getString("status"));
        safetyCase.setReviewedBy(rs.getString("reviewed_by"));
        Timestamp reviewedTime = rs.getTimestamp("reviewed_time");
        safetyCase.setReviewedTime(reviewedTime != null ? reviewedTime.toLocalDateTime() : null);
        safetyCase.setReviewNote(rs.getString("review_note"));
        safetyCase.setVersion(rs.getString("version"));
        safetyCase.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        safetyCase.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        safetyCase.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        safetyCase.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return safetyCase;
    }

    private int toLikelihoodScore(String likelihood) {
        if (likelihood == null) return 1;
        switch (likelihood) {
            case "RARE": return 1;
            case "UNLIKELY": return 2;
            case "POSSIBLE": return 3;
            case "LIKELY": return 4;
            case "ALMOST_CERTAIN": return 5;
            default: return 1;
        }
    }

    private int toSeverityScore(String severity) {
        if (severity == null) return 1;
        switch (severity) {
            case "NEGLIGIBLE": return 1;
            case "MINOR": return 2;
            case "MODERATE": return 3;
            case "MAJOR": return 4;
            case "CATASTROPHIC": return 5;
            default: return 1;
        }
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
