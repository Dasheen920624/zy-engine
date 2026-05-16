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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnginePersistenceService {
    private static final int MAX_AUDIT_RECORDS = 500;

    private final EnginePersistenceProperties properties;
    private final ObjectMapper objectMapper;
    private final List<Map<String, Object>> auditRecords =
            Collections.synchronizedList(new ArrayList<Map<String, Object>>());

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
                "USING (SELECT ? tenant_id, ? org_code, ? encounter_id, ? pathway_code, 'ACTIVE' status FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.org_code=s.org_code AND t.encounter_id=s.encounter_id AND t.pathway_code=s.pathway_code AND t.status=s.status) " +
                "WHEN MATCHED THEN UPDATE SET t.current_node_code=?, t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, org_code, patient_id, encounter_id, pathway_code, version_no, status, current_node_code, admitted_by, admission_time, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            long id = extractNumericId(instance.getInstanceId());
            String tenantId = string(instance.getTenantId(), "default");
            String orgCode = string(instance.getLegacyOrgCode(), string(instance.getHospitalCode(), "ZYHOSPITAL"));
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
            ps.setString(i++, instance.getEncounterId());
            ps.setString(i++, instance.getPathwayCode());
            ps.setString(i++, instance.getCurrentNodeCode());
            ps.setLong(i++, id);
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
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

    public void saveAuditLog(String engineType, String actionType, String targetType, String targetCode,
                             String patientId, String encounterId, String operatorId, Map<String, Object> detail) {
        String traceId = TraceContext.getTraceId();
        recordAuditLog(traceId, engineType, actionType, targetType, targetCode,
                patientId, encounterId, operatorId, detail);
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO engine_audit_log " +
                "(id, trace_id, engine_type, action_type, target_type, target_code, patient_id, encounter_id, operator_id, detail_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setString(i++, traceId);
            ps.setString(i++, engineType);
            ps.setString(i++, actionType);
            ps.setString(i++, targetType);
            ps.setString(i++, targetCode);
            ps.setString(i++, patientId);
            ps.setString(i++, encounterId);
            ps.setString(i++, operatorId);
            ps.setString(i++, toJson(detail));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save audit log failed: " + ex.getMessage(), ex);
        }
    }

    public List<Map<String, Object>> listAuditLogs(Map<String, String> filters) {
        String traceId = filterValue(filters, "traceId");
        String engineType = filterValue(filters, "engineType");
        String actionType = filterValue(filters, "actionType");
        String targetType = filterValue(filters, "targetType");
        String targetCode = filterValue(filters, "targetCode");
        String patientId = filterValue(filters, "patientId");
        String encounterId = filterValue(filters, "encounterId");
        String operatorId = filterValue(filters, "operatorId");
        String tenantId = filterValue(filters, "tenantId");
        String groupCode = filterValue(filters, "groupCode");
        String hospitalCode = filterValue(filters, "hospitalCode");
        String campusCode = filterValue(filters, "campusCode");
        String siteCode = filterValue(filters, "siteCode");
        String departmentCode = filterValue(filters, "departmentCode");
        String scopeLevel = filterValue(filters, "scopeLevel");
        String scopeCode = filterValue(filters, "scopeCode");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<Map<String, Object>> snapshot;
        synchronized (auditRecords) {
            snapshot = new ArrayList<Map<String, Object>>(auditRecords);
        }
        Collections.reverse(snapshot);

        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> record : snapshot) {
            if (traceId != null && !traceId.equals(string(record.get("trace_id"), null))) {
                continue;
            }
            if (engineType != null && !engineType.equalsIgnoreCase(string(record.get("engine_type"), null))) {
                continue;
            }
            if (actionType != null && !actionType.equalsIgnoreCase(string(record.get("action_type"), null))) {
                continue;
            }
            if (targetType != null && !targetType.equalsIgnoreCase(string(record.get("target_type"), null))) {
                continue;
            }
            if (targetCode != null && !targetCode.equalsIgnoreCase(string(record.get("target_code"), null))) {
                continue;
            }
            if (patientId != null && !patientId.equals(string(record.get("patient_id"), null))) {
                continue;
            }
            if (encounterId != null && !encounterId.equals(string(record.get("encounter_id"), null))) {
                continue;
            }
            if (operatorId != null && !operatorId.equals(string(record.get("operator_id"), null))) {
                continue;
            }
            if (tenantId != null && !tenantId.equals(string(record.get("tenant_id"), null))) {
                continue;
            }
            if (groupCode != null && !groupCode.equals(string(record.get("group_code"), null))) {
                continue;
            }
            if (hospitalCode != null && !hospitalCode.equals(string(record.get("hospital_code"), null))) {
                continue;
            }
            if (campusCode != null && !campusCode.equals(string(record.get("campus_code"), null))) {
                continue;
            }
            if (siteCode != null && !siteCode.equals(string(record.get("site_code"), null))) {
                continue;
            }
            if (departmentCode != null && !departmentCode.equals(string(record.get("department_code"), null))) {
                continue;
            }
            if (scopeLevel != null && !scopeLevel.equalsIgnoreCase(string(record.get("scope_level"), null))) {
                continue;
            }
            if (scopeCode != null && !scopeCode.equals(string(record.get("scope_code"), null))) {
                continue;
            }
            matched.add(new LinkedHashMap<String, Object>(record));
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public Map<String, Object> summarizeAuditLogs(Map<String, String> filters) {
        Map<String, String> merged = new LinkedHashMap<String, String>();
        if (filters != null) {
            merged.putAll(filters);
        }
        merged.put("limit", String.valueOf(Integer.MAX_VALUE));
        List<Map<String, Object>> records = listAuditLogs(merged);
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("total", records.size());
        summary.put("by_engine_type", aggregateAudit(records, "engine_type"));
        summary.put("by_action_type", aggregateAudit(records, "action_type"));
        summary.put("by_target_type", aggregateAudit(records, "target_type"));
        summary.put("by_hospital_code", aggregateAudit(records, "hospital_code"));
        summary.put("by_scope_level", aggregateAudit(records, "scope_level"));
        return summary;
    }

    private void recordAuditLog(String traceId, String engineType, String actionType, String targetType, String targetCode,
                                String patientId, String encounterId, String operatorId, Map<String, Object> detail) {
        Map<String, Object> record = new LinkedHashMap<String, Object>();
        record.put("trace_id", traceId);
        record.put("engine_type", engineType);
        record.put("action_type", actionType);
        record.put("target_type", targetType);
        record.put("target_code", targetCode);
        record.put("patient_id", patientId);
        record.put("encounter_id", encounterId);
        record.put("operator_id", operatorId);
        record.put("detail", detail == null
                ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(detail));
        record.put("tenant_id", orgValue(detail, "tenant_id", "tenantId"));
        record.put("group_code", orgValue(detail, "group_code", "groupCode"));
        record.put("hospital_code", orgValue(detail, "hospital_code", "hospitalCode"));
        record.put("campus_code", orgValue(detail, "campus_code", "campusCode"));
        record.put("site_code", orgValue(detail, "site_code", "siteCode"));
        record.put("department_code", orgValue(detail, "department_code", "departmentCode"));
        record.put("scope_level", orgValue(detail, "scope_level", "scopeLevel"));
        record.put("scope_code", orgValue(detail, "scope_code", "scopeCode"));
        record.put("org_source", orgValue(detail, "org_source", "orgSource"));
        record.put("created_time", nowText());
        synchronized (auditRecords) {
            auditRecords.add(record);
            while (auditRecords.size() > MAX_AUDIT_RECORDS) {
                auditRecords.remove(0);
            }
        }
    }

    private String orgValue(Map<String, Object> detail, String snakeKey, String camelKey) {
        if (detail == null) {
            return null;
        }
        String value = string(detail.get(snakeKey), null);
        if (value == null) {
            value = string(detail.get(camelKey), null);
        }
        return value;
    }

    private List<Map<String, Object>> aggregateAudit(List<Map<String, Object>> records, String dimension) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (Map<String, Object> record : records) {
            String key = string(record.get(dimension), "UNKNOWN");
            Integer count = counts.get(key);
            counts.put(key, count == null ? 1 : count + 1);
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> left, Map.Entry<String, Integer> right) {
                int byCount = right.getValue().compareTo(left.getValue());
                return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : entries) {
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put(dimension, entry.getKey());
            bucket.put("count", entry.getValue());
            result.add(bucket);
        }
        return result;
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

    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
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
