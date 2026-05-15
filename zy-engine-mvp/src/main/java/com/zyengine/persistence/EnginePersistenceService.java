package com.zyengine.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyengine.common.TraceContext;
import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.PatientTaskState;
import com.zyengine.dto.PathwayVariationRecord;
import com.zyengine.dto.RecommendationCard;
import com.zyengine.dto.RuleResult;
import com.zyengine.rule.RuleDefinition;
import com.zyengine.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

@Service
public class EnginePersistenceService {
    private final EnginePersistenceProperties properties;
    private final ObjectMapper objectMapper;

    public EnginePersistenceService(EnginePersistenceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public boolean enabled() {
        return properties.isEnabled() && properties.hasPassword();
    }

    public void savePathwayDraft(String pathwayCode, Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        String sql = "MERGE INTO pe_pathway_def t " +
                "USING (SELECT ? tenant_id, ? org_code, ? pathway_code FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.org_code=s.org_code AND t.pathway_code=s.pathway_code) " +
                "WHEN MATCHED THEN UPDATE SET t.pathway_name=?, t.specialty_code=?, t.disease_code=?, t.status='DRAFT', t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String tenantId = string(config.get("tenant_id"), "default");
            String orgCode = string(config.get("org_code"), "ZYHOSPITAL");
            String name = string(config.get("pathway_name"), pathwayCode);
            String specialty = string(config.get("specialty_code"), null);
            String disease = string(config.get("disease_code"), pathwayCode);
            long id = Ids.next();
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
            ps.setString(i++, pathwayCode);
            ps.setString(i++, name);
            ps.setString(i++, specialty);
            ps.setString(i++, disease);
            ps.setLong(i++, id);
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
            ps.setString(i++, pathwayCode);
            ps.setString(i++, name);
            ps.setString(i++, specialty);
            ps.setString(i++, disease);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    public void savePathwayVersion(String pathwayCode, String versionNo, String status, Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        String sql = "MERGE INTO pe_pathway_version t " +
                "USING (SELECT ? pathway_code, ? version_no FROM dual) s " +
                "ON (t.pathway_code=s.pathway_code AND t.version_no=s.version_no) " +
                "WHEN MATCHED THEN UPDATE SET t.status=?, t.config_json=? " +
                "WHEN NOT MATCHED THEN INSERT (id, pathway_code, version_no, status, config_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP)";
        String json = toJson(config);
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = Ids.next();
            int i = 1;
            ps.setString(i++, pathwayCode);
            ps.setString(i++, versionNo);
            ps.setString(i++, status);
            ps.setString(i++, json);
            ps.setLong(i++, id);
            ps.setString(i++, pathwayCode);
            ps.setString(i++, versionNo);
            ps.setString(i++, status);
            ps.setString(i++, json);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save pathway version failed: " + ex.getMessage(), ex);
        }
    }

    public void updatePathwayStatus(String pathwayCode, String status) {
        if (!enabled()) {
            return;
        }
        String sql = "UPDATE pe_pathway_def SET status=?, updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id='default' AND org_code='ZYHOSPITAL' AND pathway_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            // 路径主表用于配置中心筛选当前可用路径，发布版本时需要同步主表状态。
            ps.setString(1, status);
            ps.setString(2, pathwayCode);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update pathway status failed: " + ex.getMessage(), ex);
        }
    }

    public void saveRecommendation(RecommendationCard card) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO pe_recommendation_record " +
                "(id, recommendation_id, patient_id, encounter_id, scenario, target_code, target_name, score, confidence, action_level, card_json, trace_id, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setString(i++, card.getRecommendationId());
            ps.setString(i++, card.getPatientId());
            ps.setString(i++, card.getEncounterId());
            ps.setString(i++, card.getScenario());
            ps.setString(i++, card.getTargetCode());
            ps.setString(i++, card.getTargetName());
            ps.setDouble(i++, card.getScore());
            ps.setString(i++, card.getConfidence());
            ps.setString(i++, card.getActionLevel());
            ps.setString(i++, toJson(card));
            ps.setString(i++, TraceContext.getTraceId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save recommendation failed: " + ex.getMessage(), ex);
        }
    }

    public void savePatientInstance(PatientPathwayInstance instance, String admittedBy) {
        if (!enabled()) {
            return;
        }
        String sql = "MERGE INTO pe_patient_instance t " +
                "USING (SELECT ? encounter_id, ? pathway_code, 'ACTIVE' status FROM dual) s " +
                "ON (t.encounter_id=s.encounter_id AND t.pathway_code=s.pathway_code AND t.status=s.status) " +
                "WHEN MATCHED THEN UPDATE SET t.current_node_code=?, t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, org_code, patient_id, encounter_id, pathway_code, version_no, status, current_node_code, admitted_by, admission_time, created_time) " +
                "VALUES (?, 'default', 'ZYHOSPITAL', ?, ?, ?, ?, 'ACTIVE', ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = extractNumericId(instance.getInstanceId());
            int i = 1;
            ps.setString(i++, instance.getEncounterId());
            ps.setString(i++, instance.getPathwayCode());
            ps.setString(i++, instance.getCurrentNodeCode());
            ps.setLong(i++, id);
            ps.setString(i++, instance.getPatientId());
            ps.setString(i++, instance.getEncounterId());
            ps.setString(i++, instance.getPathwayCode());
            ps.setString(i++, instance.getVersionNo());
            ps.setString(i++, instance.getCurrentNodeCode());
            ps.setString(i++, admittedBy);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save patient instance failed: " + ex.getMessage(), ex);
        }
    }

    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String status) {
        updateNodeState(instance, nodeCode, nodeCode, status);
    }

    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String nodeName, String status) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO pe_patient_node_state (id, instance_id, node_code, node_name, status, enter_time, complete_time, timeout_flag, created_time) " +
                "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, ?, 0, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setLong(i++, extractNumericId(instance.getInstanceId()));
            ps.setString(i++, nodeCode);
            ps.setString(i++, nodeName);
            ps.setString(i++, status);
            ps.setTimestamp(i++, "COMPLETED".equals(status) ? new Timestamp(System.currentTimeMillis()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save node state failed: " + ex.getMessage(), ex);
        }
    }

    public void saveTaskState(PatientTaskState taskState) {
        if (!enabled()) {
            return;
        }
        String sql = "MERGE INTO pe_patient_task_state t " +
                "USING (SELECT ? instance_id, ? node_code, ? task_code FROM dual) s " +
                "ON (t.instance_id=s.instance_id AND t.node_code=s.node_code AND t.task_code=s.task_code) " +
                "WHEN MATCHED THEN UPDATE SET t.task_name=?, t.task_type=?, t.status=?, t.result_json=?, t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, instance_id, node_code, task_code, task_name, task_type, status, result_json, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long instanceId = extractNumericId(taskState.getInstanceId());
            String resultJson = toJson(taskState.getResult());
            int i = 1;
            ps.setLong(i++, instanceId);
            ps.setString(i++, taskState.getNodeCode());
            ps.setString(i++, taskState.getTaskCode());
            ps.setString(i++, taskState.getTaskName());
            ps.setString(i++, taskState.getTaskType());
            ps.setString(i++, taskState.getStatus());
            ps.setString(i++, resultJson);
            ps.setLong(i++, Ids.next());
            ps.setLong(i++, instanceId);
            ps.setString(i++, taskState.getNodeCode());
            ps.setString(i++, taskState.getTaskCode());
            ps.setString(i++, taskState.getTaskName());
            ps.setString(i++, taskState.getTaskType());
            ps.setString(i++, taskState.getStatus());
            ps.setString(i++, resultJson);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save task state failed: " + ex.getMessage(), ex);
        }
    }

    public void saveVariationRecord(PathwayVariationRecord variation) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO pe_variation_record " +
                "(id, instance_id, patient_id, encounter_id, node_code, variation_type, reason, operator_id, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setLong(i++, extractNumericId(variation.getInstanceId()));
            ps.setString(i++, variation.getPatientId());
            ps.setString(i++, variation.getEncounterId());
            ps.setString(i++, variation.getNodeCode());
            ps.setString(i++, variation.getVariationType());
            ps.setString(i++, truncate(variation.getReason(), 1000));
            ps.setString(i++, variation.getOperatorId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save variation record failed: " + ex.getMessage(), ex);
        }
    }

    public void saveRuleDefinition(RuleDefinition definition, String approvedBy) {
        if (!enabled()) {
            return;
        }
        String sql = "MERGE INTO re_rule_def t " +
                "USING (SELECT 'default' tenant_id, 'ZYHOSPITAL' org_code, ? rule_code, ? version_no FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.org_code=s.org_code AND t.rule_code=s.rule_code AND t.version_no=s.version_no) " +
                "WHEN MATCHED THEN UPDATE SET t.rule_name=?, t.rule_type=?, t.status=?, t.severity=?, t.rule_json=?, t.approved_by=?, " +
                "t.approved_time=CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE t.approved_time END " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, org_code, rule_code, rule_name, rule_type, version_no, status, severity, rule_json, created_time, approved_by, approved_time) " +
                "VALUES (?, 'default', 'ZYHOSPITAL', ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE NULL END)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String json = toJson(definition.getRuleJson());
            int i = 1;
            ps.setString(i++, definition.getRuleCode());
            ps.setString(i++, definition.getVersionNo());
            ps.setString(i++, definition.getRuleName());
            ps.setString(i++, definition.getRuleType());
            ps.setString(i++, definition.getStatus());
            ps.setString(i++, definition.getSeverity());
            ps.setString(i++, json);
            ps.setString(i++, approvedBy);
            ps.setString(i++, definition.getStatus());
            ps.setLong(i++, Ids.next());
            ps.setString(i++, definition.getRuleCode());
            ps.setString(i++, definition.getRuleName());
            ps.setString(i++, definition.getRuleType());
            ps.setString(i++, definition.getVersionNo());
            ps.setString(i++, definition.getStatus());
            ps.setString(i++, definition.getSeverity());
            ps.setString(i++, json);
            ps.setString(i++, approvedBy);
            ps.setString(i++, definition.getStatus());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save rule definition failed: " + ex.getMessage(), ex);
        }
    }

    public void saveRuleExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                long elapsedMs, String resultStatus, String errorCode, String errorMessage) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO re_rule_exec_log " +
                "(id, trace_id, rule_code, rule_version, patient_id, encounter_id, event_id, hit_flag, severity, message, input_snapshot, output_snapshot, elapsed_ms, result_status, error_code, error_message, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setString(i++, TraceContext.getTraceId());
            ps.setString(i++, result.getRuleCode());
            ps.setString(i++, ruleVersion);
            ps.setString(i++, ClinicalFactUtils.patientId(patientContext));
            ps.setString(i++, ClinicalFactUtils.encounterId(patientContext));
            ps.setString(i++, null);
            ps.setInt(i++, result.isHit() ? 1 : 0);
            ps.setString(i++, result.getSeverity());
            ps.setString(i++, truncate(result.getMessage(), 1000));
            ps.setString(i++, toJson(patientContext));
            ps.setString(i++, toJson(result));
            ps.setLong(i++, elapsedMs);
            ps.setString(i++, resultStatus);
            ps.setString(i++, errorCode);
            ps.setString(i++, truncate(errorMessage, 1000));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save rule exec log failed: " + ex.getMessage(), ex);
        }
    }

    private Connection connection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Oracle JDBC driver not found", ex);
        }
        SQLException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
            } catch (SQLException ex) {
                last = ex;
                // Oracle监听偶发拒绝连接时先短暂重试，P2阶段会替换为正式连接池。
                if (!shouldRetryConnection(ex) || attempt == 3) {
                    throw ex;
                }
                sleepQuietly(500L * attempt);
            }
        }
        throw last;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON serialization failed", ex);
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
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

    private long extractNumericId(String text) {
        if (text == null) {
            return Ids.next();
        }
        String digits = text.replaceAll("\\D", "");
        if (digits.length() > 15) {
            digits = digits.substring(digits.length() - 15);
        }
        if (digits.isEmpty()) {
            return Ids.next();
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return Ids.next();
        }
    }
}
