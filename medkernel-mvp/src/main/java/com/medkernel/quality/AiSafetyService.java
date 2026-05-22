package com.medkernel.quality;

import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.UUID;

@Service
public class AiSafetyService {

    private static final Logger log = LoggerFactory.getLogger(AiSafetyService.class);

    private final EnginePersistenceProperties properties;
    private final DataSource dataSource;

    public AiSafetyService(EnginePersistenceProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    // =========================================================================
    // 红队场景管理
    // =========================================================================

    public RedTeamScenario createScenario(RedTeamScenario scenario) {
        if (!properties.isEnabled()) {
            return scenario;
        }
        scenario.setId(Ids.next());
        if (scenario.getStatus() == null) {
            scenario.setStatus("DRAFT");
        }
        if (scenario.getEnabled() == null) {
            scenario.setEnabled("Y");
        }
        scenario.setCreatedTime(LocalDateTime.now());

        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO ai_red_team_scenario "
                    + "(id, tenant_id, scenario_code, scenario_name, category, description, "
                    + "attack_prompt, expected_behavior, severity, status, enabled, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO ai_red_team_scenario "
                    + "(id, tenant_id, scenario_code, scenario_name, category, description, "
                    + "attack_prompt, expected_behavior, severity, status, enabled, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, scenario.getId());
            ps.setObject(i++, scenario.getTenantId());
            ps.setString(i++, scenario.getScenarioCode());
            ps.setString(i++, scenario.getScenarioName());
            ps.setString(i++, scenario.getCategory());
            ps.setString(i++, scenario.getDescription());
            ps.setString(i++, scenario.getAttackPrompt());
            ps.setString(i++, scenario.getExpectedBehavior());
            ps.setString(i++, scenario.getSeverity());
            ps.setString(i++, scenario.getStatus());
            ps.setString(i++, scenario.getEnabled());
            ps.setString(i++, scenario.getCreatedBy());
            ps.setString(i++, scenario.getUpdatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("创建红队场景失败: " + ex.getMessage(), ex);
        }
        return scenario;
    }

    public RedTeamScenario updateScenario(RedTeamScenario scenario) {
        if (!properties.isEnabled()) {
            return scenario;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE ai_red_team_scenario SET scenario_name=?, category=?, description=?, "
                    + "attack_prompt=?, expected_behavior=?, severity=?, status=?, enabled=?, "
                    + "updated_by=?, updated_time=CURRENT_TIMESTAMP "
                    + "WHERE id=? AND tenant_id=?";
        } else {
            sql = "UPDATE ai_red_team_scenario SET scenario_name=?, category=?, description=?, "
                    + "attack_prompt=?, expected_behavior=?, severity=?, status=?, enabled=?, "
                    + "updated_by=?, updated_time=SYSTIMESTAMP "
                    + "WHERE id=? AND tenant_id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, scenario.getScenarioName());
            ps.setString(i++, scenario.getCategory());
            ps.setString(i++, scenario.getDescription());
            ps.setString(i++, scenario.getAttackPrompt());
            ps.setString(i++, scenario.getExpectedBehavior());
            ps.setString(i++, scenario.getSeverity());
            ps.setString(i++, scenario.getStatus());
            ps.setString(i++, scenario.getEnabled());
            ps.setString(i++, scenario.getUpdatedBy());
            ps.setLong(i++, scenario.getId());
            ps.setObject(i++, scenario.getTenantId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Red team scenario not found: id=" + scenario.getId());
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("更新红队场景失败: " + ex.getMessage(), ex);
        }
        return scenario;
    }

    public List<RedTeamScenario> listScenarios(Long tenantId, String category, String status) {
        if (!properties.isEnabled()) {
            return new ArrayList<RedTeamScenario>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_red_team_scenario WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<RedTeamScenario> scenarios = new ArrayList<RedTeamScenario>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scenarios.add(mapScenario(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询红队场景失败: " + ex.getMessage(), ex);
        }
        return scenarios;
    }

    // =========================================================================
    // 红队测试执行
    // =========================================================================

    public RedTeamResult executeRedTeamTest(Long scenarioId, String modelCode, String modelVersion,
                                            String executedBy) {
        if (!properties.isEnabled()) {
            RedTeamResult result = new RedTeamResult();
            result.setScenarioId(scenarioId);
            result.setModelCode(modelCode);
            result.setModelVersion(modelVersion);
            result.setExecutedBy(executedBy);
            result.setVerdict("UNCERTAIN");
            return result;
        }
        // 1. 查询场景
        RedTeamScenario scenario = getScenario(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Red team scenario not found: id=" + scenarioId);
        }

        // 2. 构造测试结果
        RedTeamResult result = new RedTeamResult();
        result.setId(Ids.next());
        result.setTenantId(scenario.getTenantId());
        result.setResultCode("RT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        result.setScenarioId(scenarioId);
        result.setScenarioCode(scenario.getScenarioCode());
        result.setScenarioName(scenario.getScenarioName());
        result.setCategory(scenario.getCategory());
        result.setModelCode(modelCode);
        result.setModelVersion(modelVersion);
        result.setSeverity(scenario.getSeverity());
        result.setExecutedBy(executedBy);
        result.setExecutedTime(LocalDateTime.now());
        result.setCreatedTime(LocalDateTime.now());

        // 3. 保存测试结果
        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO ai_red_team_result "
                    + "(id, tenant_id, result_code, scenario_id, scenario_code, scenario_name, category, "
                    + "model_code, model_version, prompt_template_code, actual_response, verdict, "
                    + "vulnerability_type, vulnerability_detail, remediation, severity, "
                    + "executed_by, executed_time, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO ai_red_team_result "
                    + "(id, tenant_id, result_code, scenario_id, scenario_code, scenario_name, category, "
                    + "model_code, model_version, prompt_template_code, actual_response, verdict, "
                    + "vulnerability_type, vulnerability_detail, remediation, severity, "
                    + "executed_by, executed_time, created_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, result.getId());
            ps.setObject(i++, result.getTenantId());
            ps.setString(i++, result.getResultCode());
            ps.setLong(i++, result.getScenarioId());
            ps.setString(i++, result.getScenarioCode());
            ps.setString(i++, result.getScenarioName());
            ps.setString(i++, result.getCategory());
            ps.setString(i++, result.getModelCode());
            ps.setString(i++, result.getModelVersion());
            ps.setString(i++, result.getPromptTemplateCode());
            ps.setString(i++, result.getActualResponse());
            ps.setString(i++, result.getVerdict());
            ps.setString(i++, result.getVulnerabilityType());
            ps.setString(i++, result.getVulnerabilityDetail());
            ps.setString(i++, result.getRemediation());
            ps.setString(i++, result.getSeverity());
            ps.setString(i++, result.getExecutedBy());
            ps.setTimestamp(i++, result.getExecutedTime() != null ? Timestamp.valueOf(result.getExecutedTime()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("保存红队测试结果失败: " + ex.getMessage(), ex);
        }
        return result;
    }

    public List<RedTeamResult> listRedTeamResults(Long tenantId, String category, String verdict, String severity) {
        if (!properties.isEnabled()) {
            return new ArrayList<RedTeamResult>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_red_team_result WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (verdict != null && !verdict.isEmpty()) {
            sql.append(" AND verdict = ?");
            params.add(verdict);
        }
        if (severity != null && !severity.isEmpty()) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        sql.append(" ORDER BY created_time DESC");

        List<RedTeamResult> results = new ArrayList<RedTeamResult>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRedTeamResult(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询红队测试结果失败: " + ex.getMessage(), ex);
        }
        return results;
    }

    public Map<String, Object> getRedTeamSummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);

        if (!properties.isEnabled()) {
            summary.put("totalScenarios", 0);
            summary.put("totalResults", 0);
            summary.put("verdictDistribution", new LinkedHashMap<String, Integer>());
            summary.put("severityDistribution", new LinkedHashMap<String, Integer>());
            return summary;
        }

        // 场景统计
        String scenarioSql = "SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END) AS active_count "
                + "FROM ai_red_team_scenario WHERE tenant_id=?";
        int totalScenarios = 0;
        int activeScenarios = 0;
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(scenarioSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalScenarios = rs.getInt("total");
                    activeScenarios = rs.getInt("active_count");
                }
            }
        } catch (SQLException ex) {
            log.error("获取红队场景统计失败", ex);
        }
        summary.put("totalScenarios", totalScenarios);
        summary.put("activeScenarios", activeScenarios);

        // 测试结果统计
        String resultSql = "SELECT verdict, severity, COUNT(*) AS cnt FROM ai_red_team_result WHERE tenant_id=? "
                + "GROUP BY verdict, severity";
        Map<String, Integer> verdictDist = new LinkedHashMap<String, Integer>();
        verdictDist.put("PASS", 0);
        verdictDist.put("FAIL", 0);
        verdictDist.put("UNCERTAIN", 0);
        Map<String, Integer> severityDist = new LinkedHashMap<String, Integer>();
        severityDist.put("LOW", 0);
        severityDist.put("MEDIUM", 0);
        severityDist.put("HIGH", 0);
        severityDist.put("CRITICAL", 0);
        int totalResults = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(resultSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String verdict = rs.getString("verdict");
                    String severity = rs.getString("severity");
                    int cnt = rs.getInt("cnt");
                    totalResults += cnt;
                    if (verdict != null && verdictDist.containsKey(verdict)) {
                        verdictDist.put(verdict, verdictDist.get(verdict) + cnt);
                    }
                    if (severity != null && severityDist.containsKey(severity)) {
                        severityDist.put(severity, severityDist.get(severity) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("获取红队测试统计失败", ex);
        }

        summary.put("totalResults", totalResults);
        summary.put("verdictDistribution", verdictDist);
        summary.put("severityDistribution", severityDist);
        return summary;
    }

    // =========================================================================
    // 幻觉检测
    // =========================================================================

    public HallucinationDetection recordDetection(HallucinationDetection detection) {
        if (!properties.isEnabled()) {
            return detection;
        }
        detection.setId(Ids.next());
        if (detection.getDetectionCode() == null) {
            detection.setDetectionCode("HD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (detection.getStatus() == null) {
            detection.setStatus("DETECTED");
        }
        if (detection.getProtectionAction() == null) {
            detection.setProtectionAction(getProtectionAction(detection.getVerdict()));
        }
        detection.setCreatedTime(LocalDateTime.now());

        String sql;
        if (properties.localFileDatabase()) {
            sql = "INSERT INTO ai_hallucination_detection "
                    + "(id, tenant_id, detection_code, model_code, model_version, prompt_template_code, "
                    + "input_content, output_content, detection_type, confidence_score, verdict, evidence, "
                    + "protection_action, reviewer, review_time, review_note, status, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)";
        } else {
            sql = "INSERT INTO ai_hallucination_detection "
                    + "(id, tenant_id, detection_code, model_code, model_version, prompt_template_code, "
                    + "input_content, output_content, detection_type, confidence_score, verdict, evidence, "
                    + "protection_action, reviewer, review_time, review_note, status, "
                    + "created_by, created_time, updated_by, updated_time) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, SYSTIMESTAMP)";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, detection.getId());
            ps.setObject(i++, detection.getTenantId());
            ps.setString(i++, detection.getDetectionCode());
            ps.setString(i++, detection.getModelCode());
            ps.setString(i++, detection.getModelVersion());
            ps.setString(i++, detection.getPromptTemplateCode());
            ps.setString(i++, detection.getInputContent());
            ps.setString(i++, detection.getOutputContent());
            ps.setString(i++, detection.getDetectionType());
            ps.setObject(i++, detection.getConfidenceScore());
            ps.setString(i++, detection.getVerdict());
            ps.setString(i++, detection.getEvidence());
            ps.setString(i++, detection.getProtectionAction());
            ps.setString(i++, detection.getReviewer());
            ps.setTimestamp(i++, detection.getReviewTime() != null ? Timestamp.valueOf(detection.getReviewTime()) : null);
            ps.setString(i++, detection.getReviewNote());
            ps.setString(i++, detection.getStatus());
            ps.setString(i++, detection.getCreatedBy());
            ps.setString(i++, detection.getUpdatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("记录幻觉检测失败: " + ex.getMessage(), ex);
        }
        return detection;
    }

    public List<HallucinationDetection> listDetections(Long tenantId, String verdict, String status) {
        if (!properties.isEnabled()) {
            return new ArrayList<HallucinationDetection>();
        }
        StringBuilder sql = new StringBuilder("SELECT * FROM ai_hallucination_detection WHERE tenant_id = ?");
        List<Object> params = new ArrayList<Object>();
        params.add(tenantId);
        if (verdict != null && !verdict.isEmpty()) {
            sql.append(" AND verdict = ?");
            params.add(verdict);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY created_time DESC");

        List<HallucinationDetection> detections = new ArrayList<HallucinationDetection>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    detections.add(mapHallucinationDetection(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询幻觉检测记录失败: " + ex.getMessage(), ex);
        }
        return detections;
    }

    public HallucinationDetection reviewDetection(Long detectionId, String reviewer, String reviewNote, String status) {
        if (!properties.isEnabled()) {
            HallucinationDetection detection = new HallucinationDetection();
            detection.setId(detectionId);
            detection.setReviewer(reviewer);
            detection.setReviewNote(reviewNote);
            detection.setStatus(status);
            return detection;
        }
        String sql;
        if (properties.localFileDatabase()) {
            sql = "UPDATE ai_hallucination_detection SET reviewer=?, review_note=?, status=?, "
                    + "review_time=CURRENT_TIMESTAMP, updated_by=?, updated_time=CURRENT_TIMESTAMP "
                    + "WHERE id=?";
        } else {
            sql = "UPDATE ai_hallucination_detection SET reviewer=?, review_note=?, status=?, "
                    + "review_time=SYSTIMESTAMP, updated_by=?, updated_time=SYSTIMESTAMP "
                    + "WHERE id=?";
        }
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, reviewer);
            ps.setString(i++, reviewNote);
            ps.setString(i++, status);
            ps.setString(i++, reviewer);
            ps.setLong(i++, detectionId);
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Hallucination detection not found: id=" + detectionId);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("审核幻觉检测失败: " + ex.getMessage(), ex);
        }
        return getDetection(detectionId);
    }

    public Map<String, Object> getHallucinationSummary(Long tenantId) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("tenantId", tenantId);

        if (!properties.isEnabled()) {
            summary.put("totalDetections", 0);
            summary.put("verdictDistribution", new LinkedHashMap<String, Integer>());
            summary.put("statusDistribution", new LinkedHashMap<String, Integer>());
            return summary;
        }

        String resultSql = "SELECT verdict, status, COUNT(*) AS cnt FROM ai_hallucination_detection WHERE tenant_id=? "
                + "GROUP BY verdict, status";
        Map<String, Integer> verdictDist = new LinkedHashMap<String, Integer>();
        verdictDist.put("HALLUCINATION", 0);
        verdictDist.put("LIKELY_HALLUCINATION", 0);
        verdictDist.put("UNCERTAIN", 0);
        verdictDist.put("SAFE", 0);
        Map<String, Integer> statusDist = new LinkedHashMap<String, Integer>();
        statusDist.put("DETECTED", 0);
        statusDist.put("REVIEWING", 0);
        statusDist.put("RESOLVED", 0);
        statusDist.put("DISMISSED", 0);
        int totalDetections = 0;

        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(resultSql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String verdict = rs.getString("verdict");
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    totalDetections += cnt;
                    if (verdict != null && verdictDist.containsKey(verdict)) {
                        verdictDist.put(verdict, verdictDist.get(verdict) + cnt);
                    }
                    if (status != null && statusDist.containsKey(status)) {
                        statusDist.put(status, statusDist.get(status) + cnt);
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("获取幻觉检测统计失败", ex);
        }

        summary.put("totalDetections", totalDetections);
        summary.put("verdictDistribution", verdictDist);
        summary.put("statusDistribution", statusDist);
        return summary;
    }

    public String getProtectionAction(String verdict) {
        if (verdict == null) {
            return "PASS";
        }
        switch (verdict) {
            case "HALLUCINATION":
                return "BLOCK";
            case "LIKELY_HALLUCINATION":
                return "DEGRADE";
            case "UNCERTAIN":
                return "HUMAN_REVIEW";
            case "SAFE":
                return "PASS";
            default:
                return "PASS";
        }
    }

    // =========================================================================
    // 内部方法
    // =========================================================================

    private RedTeamScenario getScenario(Long scenarioId) {
        String sql = "SELECT * FROM ai_red_team_scenario WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, scenarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapScenario(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询红队场景失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private HallucinationDetection getDetection(Long detectionId) {
        String sql = "SELECT * FROM ai_hallucination_detection WHERE id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, detectionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapHallucinationDetection(rs);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("查询幻觉检测记录失败: " + ex.getMessage(), ex);
        }
        return null;
    }

    private RedTeamScenario mapScenario(ResultSet rs) throws SQLException {
        RedTeamScenario scenario = new RedTeamScenario();
        scenario.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        scenario.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        scenario.setScenarioCode(rs.getString("scenario_code"));
        scenario.setScenarioName(rs.getString("scenario_name"));
        scenario.setCategory(rs.getString("category"));
        scenario.setDescription(rs.getString("description"));
        scenario.setAttackPrompt(rs.getString("attack_prompt"));
        scenario.setExpectedBehavior(rs.getString("expected_behavior"));
        scenario.setSeverity(rs.getString("severity"));
        scenario.setStatus(rs.getString("status"));
        scenario.setEnabled(rs.getString("enabled"));
        scenario.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        scenario.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        scenario.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        scenario.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return scenario;
    }

    private RedTeamResult mapRedTeamResult(ResultSet rs) throws SQLException {
        RedTeamResult result = new RedTeamResult();
        result.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        result.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        result.setResultCode(rs.getString("result_code"));
        result.setScenarioId(rs.getLong("scenario_id"));
        result.setScenarioCode(rs.getString("scenario_code"));
        result.setScenarioName(rs.getString("scenario_name"));
        result.setCategory(rs.getString("category"));
        result.setModelCode(rs.getString("model_code"));
        result.setModelVersion(rs.getString("model_version"));
        result.setPromptTemplateCode(rs.getString("prompt_template_code"));
        result.setActualResponse(rs.getString("actual_response"));
        result.setVerdict(rs.getString("verdict"));
        result.setVulnerabilityType(rs.getString("vulnerability_type"));
        result.setVulnerabilityDetail(rs.getString("vulnerability_detail"));
        result.setRemediation(rs.getString("remediation"));
        result.setSeverity(rs.getString("severity"));
        result.setExecutedBy(rs.getString("executed_by"));
        Timestamp executedTime = rs.getTimestamp("executed_time");
        result.setExecutedTime(executedTime != null ? executedTime.toLocalDateTime() : null);
        Timestamp createdTime = rs.getTimestamp("created_time");
        result.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        return result;
    }

    private HallucinationDetection mapHallucinationDetection(ResultSet rs) throws SQLException {
        HallucinationDetection detection = new HallucinationDetection();
        detection.setId(rs.getLong("id"));
        Object tenantIdObj = rs.getObject("tenant_id");
        detection.setTenantId(tenantIdObj instanceof Long ? (Long) tenantIdObj : null);
        detection.setDetectionCode(rs.getString("detection_code"));
        detection.setModelCode(rs.getString("model_code"));
        detection.setModelVersion(rs.getString("model_version"));
        detection.setPromptTemplateCode(rs.getString("prompt_template_code"));
        detection.setInputContent(rs.getString("input_content"));
        detection.setOutputContent(rs.getString("output_content"));
        detection.setDetectionType(rs.getString("detection_type"));
        double confidenceScore = rs.getDouble("confidence_score");
        if (!rs.wasNull()) {
            detection.setConfidenceScore(confidenceScore);
        }
        detection.setVerdict(rs.getString("verdict"));
        detection.setEvidence(rs.getString("evidence"));
        detection.setProtectionAction(rs.getString("protection_action"));
        detection.setReviewer(rs.getString("reviewer"));
        Timestamp reviewTime = rs.getTimestamp("review_time");
        detection.setReviewTime(reviewTime != null ? reviewTime.toLocalDateTime() : null);
        detection.setReviewNote(rs.getString("review_note"));
        detection.setStatus(rs.getString("status"));
        detection.setCreatedBy(rs.getString("created_by"));
        Timestamp createdTime = rs.getTimestamp("created_time");
        detection.setCreatedTime(createdTime != null ? createdTime.toLocalDateTime() : null);
        detection.setUpdatedBy(rs.getString("updated_by"));
        Timestamp updatedTime = rs.getTimestamp("updated_time");
        detection.setUpdatedTime(updatedTime != null ? updatedTime.toLocalDateTime() : null);
        return detection;
    }

    private Connection connection() throws SQLException {
        return dataSource.getConnection();
    }
}
