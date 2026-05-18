package com.medkernel.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.TraceContext;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.provenance.SourceDocument;
import com.medkernel.dto.RuleResult;
import com.medkernel.rule.RuleDefinition;
import com.medkernel.util.ClinicalFactUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        return properties.isEnabled() && properties.hasRequiredCredentials();
    }

    public String providerName() {
        return enabled() ? properties.providerName() : "IN_MEMORY";
    }

    @PostConstruct
    public void initializeLocalSchema() {
        if (!enabled() || !properties.localFileDatabase() || !properties.isInitSchema()) {
            return;
        }
        List<String> statements = loadLocalSchemaStatements();
        try (Connection connection = connection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("initialize local database schema failed: " + ex.getMessage(), ex);
        }
    }

    public void savePathwayDraft(String pathwayCode, Map<String, Object> config) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            savePathwayDraftLocal(pathwayCode, config);
            return;
        }
        String sql = "MERGE INTO pe_pathway_def t " +
                "USING (SELECT ? AS tenant_id, ? AS org_code, ? AS pathway_code FROM dual) s " +
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
        if (properties.localFileDatabase()) {
            savePathwayVersionLocal(pathwayCode, versionNo, status, config);
            return;
        }
        String sql = "MERGE INTO pe_pathway_version t " +
                "USING (SELECT ? AS pathway_code, ? AS version_no FROM dual) s " +
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

    /**
     * 启动期/重启后用于从 DB 重建 PathwayService 的内存索引。
     * 返回所有路径主表条目，键为 pathway_code，值为最小化的 config Map（包含 tenant_id/org_code/pathway_name/status 等元数据）。
     * 配合 loadAllPathwayPublishedVersions 一起使用，避免重启导致已发布路径在 list/get 接口中"消失"。
     */
    public Map<String, Map<String, Object>> loadAllPathwayDrafts() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
        if (!enabled()) {
            return result;
        }
        String sql = "SELECT tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status " +
                "FROM pe_pathway_def";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> config = new LinkedHashMap<String, Object>();
                config.put("tenant_id", rs.getString("tenant_id"));
                config.put("org_code", rs.getString("org_code"));
                config.put("pathway_code", rs.getString("pathway_code"));
                config.put("pathway_name", rs.getString("pathway_name"));
                config.put("specialty_code", rs.getString("specialty_code"));
                config.put("disease_code", rs.getString("disease_code"));
                config.put("status", rs.getString("status"));
                result.put(rs.getString("pathway_code"), config);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load pathway drafts failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    /**
     * 加载所有路径版本，返回 List 以保留 (pathway_code, version_no) 顺序。
     * 每项包含 pathway_code/version_no/status/config（来自 config_json 反序列化）。
     */
    public List<Map<String, Object>> loadAllPathwayPublishedVersions() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (!enabled()) {
            return result;
        }
        String sql = "SELECT pathway_code, version_no, status, config_json FROM pe_pathway_version " +
                "ORDER BY pathway_code, created_time";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("pathway_code", rs.getString("pathway_code"));
                row.put("version_no", rs.getString("version_no"));
                row.put("status", rs.getString("status"));
                String configJson = rs.getString("config_json");
                if (configJson != null && !configJson.isEmpty()) {
                    try {
                        Map<String, Object> config = objectMapper.readValue(configJson, LinkedHashMap.class);
                        row.put("config", config);
                    } catch (IOException ex) {
                        // 单条解析失败不阻断整体重建，记录占位即可，业务侧 list 仍能看到版本号。
                        row.put("config", new LinkedHashMap<String, Object>());
                    }
                } else {
                    row.put("config", new LinkedHashMap<String, Object>());
                }
                result.add(row);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("load pathway versions failed: " + ex.getMessage(), ex);
        }
        return result;
    }

    public void updatePathwayStatus(String pathwayCode, String status) {
        updatePathwayStatus(pathwayCode, status, null, null);
    }

    public void updatePathwayStatus(String pathwayCode, String status, String tenantId, String orgCode) {
        if (!enabled()) {
            return;
        }
        String resolvedTenant = string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID);
        String resolvedOrg = string(orgCode, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE);
        String sql = "UPDATE pe_pathway_def SET status=?, updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND pathway_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            // 路径主表用于配置中心筛选当前可用路径，发布版本时需要同步主表状态。
            ps.setString(1, status);
            ps.setString(2, resolvedTenant);
            ps.setString(3, resolvedOrg);
            ps.setString(4, pathwayCode);
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
        if (properties.localFileDatabase()) {
            savePatientInstanceLocal(instance, admittedBy);
            return;
        }
        String sql = "MERGE INTO pe_patient_instance t " +
                "USING (SELECT ? AS tenant_id, ? AS org_code, ? AS encounter_id, ? AS pathway_code, 'ACTIVE' AS status FROM dual) s " +
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
        if (properties.localFileDatabase()) {
            saveTaskStateLocal(taskState);
            return;
        }
        String sql = "MERGE INTO pe_patient_task_state t " +
                "USING (SELECT ? AS instance_id, ? AS node_code, ? AS task_code FROM dual) s " +
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
                "(id, instance_id, patient_id, encounter_id, node_code, variation_type, reason, operator_id, " +
                "tenant_id, group_code, hospital_code, campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String tenantId = string(variation.getTenantId(), "default");
            String hospitalCode = string(variation.getHospitalCode(), string(variation.getLegacyOrgCode(), "ZYHOSPITAL"));
            String scopeLevel = string(variation.getScopeLevel(), "HOSPITAL");
            String scopeCode = string(variation.getScopeCode(), hospitalCode);
            int i = 1;
            ps.setLong(i++, Ids.next());
            ps.setLong(i++, extractNumericId(variation.getInstanceId()));
            ps.setString(i++, variation.getPatientId());
            ps.setString(i++, variation.getEncounterId());
            ps.setString(i++, variation.getNodeCode());
            ps.setString(i++, variation.getVariationType());
            ps.setString(i++, truncate(variation.getReason(), 1000));
            ps.setString(i++, variation.getOperatorId());
            ps.setString(i++, tenantId);
            ps.setString(i++, variation.getGroupCode());
            ps.setString(i++, hospitalCode);
            ps.setString(i++, variation.getCampusCode());
            ps.setString(i++, variation.getSiteCode());
            ps.setString(i++, variation.getDepartmentCode());
            ps.setString(i++, scopeLevel);
            ps.setString(i++, scopeCode);
            ps.setString(i++, variation.getOrgSource());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save variation record failed: " + ex.getMessage(), ex);
        }
    }

    public void saveRuleDefinition(RuleDefinition definition, String approvedBy) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveRuleDefinitionLocal(definition, approvedBy);
            return;
        }
        String sql = "MERGE INTO re_rule_def t " +
                "USING (SELECT ? AS tenant_id, ? AS org_code, ? AS rule_code, ? AS version_no FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.org_code=s.org_code AND t.rule_code=s.rule_code AND t.version_no=s.version_no) " +
                "WHEN MATCHED THEN UPDATE SET t.rule_name=?, t.rule_type=?, t.status=?, t.severity=?, t.rule_json=?, t.approved_by=?, " +
                "t.approved_time=CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE t.approved_time END " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, org_code, rule_code, rule_name, rule_type, version_no, status, severity, rule_json, created_time, approved_by, approved_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, ?, CASE WHEN ?='PUBLISHED' THEN SYSTIMESTAMP ELSE NULL END)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            String json = toJson(definition.getRuleJson());
            String tenantId = string(definition.getTenantId(), "default");
            String orgCode = string(definition.getLegacyOrgCode(), string(definition.getHospitalCode(), "ZYHOSPITAL"));
            int i = 1;
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
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
            ps.setString(i++, tenantId);
            ps.setString(i++, orgCode);
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
        saveRuleExecLog(result, ruleVersion, patientContext, elapsedMs, resultStatus, errorCode, errorMessage, null);
    }

    public void saveRuleExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                long elapsedMs, String resultStatus, String errorCode, String errorMessage,
                                Map<String, Object> orgFields) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO re_rule_exec_log " +
                "(id, trace_id, rule_code, rule_version, patient_id, encounter_id, event_id, hit_flag, severity, message, " +
                "input_snapshot, output_snapshot, elapsed_ms, result_status, error_code, error_message, " +
                "tenant_id, group_code, hospital_code, campus_code, site_code, department_code, scope_level, scope_code, org_source, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
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
            ps.setString(i++, orgValue(orgFields, "tenant_id", "tenantId"));
            ps.setString(i++, orgValue(orgFields, "group_code", "groupCode"));
            ps.setString(i++, orgValue(orgFields, "hospital_code", "hospitalCode"));
            ps.setString(i++, orgValue(orgFields, "campus_code", "campusCode"));
            ps.setString(i++, orgValue(orgFields, "site_code", "siteCode"));
            ps.setString(i++, orgValue(orgFields, "department_code", "departmentCode"));
            ps.setString(i++, orgValue(orgFields, "scope_level", "scopeLevel"));
            ps.setString(i++, orgValue(orgFields, "scope_code", "scopeCode"));
            ps.setString(i++, orgValue(orgFields, "org_source", "orgSource"));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save rule exec log failed: " + ex.getMessage(), ex);
        }
    }

    public void saveSourceDocument(SourceDocument document) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveSourceDocumentLocal(document);
            return;
        }
        String sql = "MERGE INTO src_document t " +
                "USING (SELECT ? AS tenant_id, ? AS document_code FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.document_code=s.document_code) " +
                "WHEN MATCHED THEN UPDATE SET t.title=?, t.source_type=?, t.source_uri=?, t.publisher=?, " +
                "t.effective_date=?, t.expiry_date=?, t.review_status=?, t.reviewed_by=?, t.reviewed_time=?, " +
                "t.content_hash=?, t.metadata_json=?, t.created_by=COALESCE(?, t.created_by), t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, document_code, title, source_type, source_uri, publisher, " +
                "effective_date, expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, " +
                "created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, string(document.getTenantId(), "default"));
            ps.setString(i++, document.getDocumentCode());
            setSourceDocumentUpdateValues(ps, document, i);
            i += 12;
            ps.setLong(i++, Ids.next());
            ps.setString(i++, string(document.getTenantId(), "default"));
            ps.setString(i++, document.getDocumentCode());
            ps.setString(i++, document.getTitle());
            ps.setString(i++, document.getSourceType());
            ps.setString(i++, document.getSourceUri());
            ps.setString(i++, document.getPublisher());
            ps.setDate(i++, parseSqlDate(document.getEffectiveDate()));
            ps.setDate(i++, parseSqlDate(document.getExpiryDate()));
            ps.setString(i++, document.getReviewStatus());
            ps.setString(i++, document.getReviewedBy());
            ps.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
            ps.setString(i++, document.getContentHash());
            ps.setString(i++, toJson(document.getMetadata()));
            ps.setString(i++, document.getCreatedBy());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save source document failed: " + ex.getMessage(), ex);
        }
    }

    public List<SourceDocument> listSourceDocuments() {
        if (!enabled()) {
            return new ArrayList<SourceDocument>();
        }
        String sql = "SELECT tenant_id, document_code, title, source_type, source_uri, publisher, effective_date, " +
                "expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, created_by, " +
                "created_time, updated_time FROM src_document ORDER BY tenant_id, document_code, updated_time";
        List<SourceDocument> documents = new ArrayList<SourceDocument>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                documents.add(toSourceDocument(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list source documents failed: " + ex.getMessage(), ex);
        }
        return documents;
    }

    public SourceDocument findSourceDocument(String tenantId, String documentCode) {
        if (!enabled()) {
            return null;
        }
        String sql = "SELECT tenant_id, document_code, title, source_type, source_uri, publisher, effective_date, " +
                "expiry_date, review_status, reviewed_by, reviewed_time, content_hash, metadata_json, created_by, " +
                "created_time, updated_time FROM src_document WHERE tenant_id=? AND document_code=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, string(tenantId, "default"));
            ps.setString(2, documentCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? toSourceDocument(rs) : null;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("find source document failed: " + ex.getMessage(), ex);
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
                "(id, trace_id, engine_type, action_type, target_type, target_code, patient_id, encounter_id, operator_id, " +
                "tenant_id, group_code, hospital_code, campus_code, site_code, department_code, scope_level, scope_code, org_source, detail_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP)";
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
            ps.setString(i++, orgValue(detail, "tenant_id", "tenantId"));
            ps.setString(i++, orgValue(detail, "group_code", "groupCode"));
            ps.setString(i++, orgValue(detail, "hospital_code", "hospitalCode"));
            ps.setString(i++, orgValue(detail, "campus_code", "campusCode"));
            ps.setString(i++, orgValue(detail, "site_code", "siteCode"));
            ps.setString(i++, orgValue(detail, "department_code", "departmentCode"));
            ps.setString(i++, orgValue(detail, "scope_level", "scopeLevel"));
            ps.setString(i++, orgValue(detail, "scope_code", "scopeCode"));
            ps.setString(i++, orgValue(detail, "org_source", "orgSource"));
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

    private void savePathwayDraftLocal(String pathwayCode, Map<String, Object> config) {
        String updateSql = "UPDATE pe_pathway_def SET pathway_name=?, specialty_code=?, disease_code=?, status='DRAFT', updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND pathway_code=?";
        String insertSql = "INSERT INTO pe_pathway_def (id, tenant_id, org_code, pathway_code, pathway_name, specialty_code, disease_code, status, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', CURRENT_TIMESTAMP)";
        String tenantId = string(config.get("tenant_id"), "default");
        String orgCode = string(config.get("org_code"), "ZYHOSPITAL");
        String name = string(config.get("pathway_name"), pathwayCode);
        String specialty = string(config.get("specialty_code"), null);
        String disease = string(config.get("disease_code"), pathwayCode);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, name);
                ps.setString(i++, specialty);
                ps.setString(i++, disease);
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, pathwayCode);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, pathwayCode);
                    ps.setString(i++, name);
                    ps.setString(i++, specialty);
                    ps.setString(i++, disease);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local pathway draft failed: " + ex.getMessage(), ex);
        }
    }

    private void savePathwayVersionLocal(String pathwayCode, String versionNo, String status, Map<String, Object> config) {
        String updateSql = "UPDATE pe_pathway_version SET status=?, config_json=? WHERE pathway_code=? AND version_no=?";
        String insertSql = "INSERT INTO pe_pathway_version (id, pathway_code, version_no, status, config_json, created_time) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        String json = toJson(config);
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, status);
                ps.setString(i++, json);
                ps.setString(i++, pathwayCode);
                ps.setString(i++, versionNo);
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, pathwayCode);
                    ps.setString(i++, versionNo);
                    ps.setString(i++, status);
                    ps.setString(i++, json);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local pathway version failed: " + ex.getMessage(), ex);
        }
    }

    private void savePatientInstanceLocal(PatientPathwayInstance instance, String admittedBy) {
        String updateSql = "UPDATE pe_patient_instance SET current_node_code=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND encounter_id=? AND pathway_code=? AND status='ACTIVE'";
        String insertSql = "INSERT INTO pe_patient_instance " +
                "(id, tenant_id, org_code, patient_id, encounter_id, pathway_code, version_no, status, current_node_code, admitted_by, admission_time, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        long id = extractNumericId(instance.getInstanceId());
        String tenantId = string(instance.getTenantId(), "default");
        String orgCode = string(instance.getLegacyOrgCode(), string(instance.getHospitalCode(), "ZYHOSPITAL"));
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, instance.getCurrentNodeCode());
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, instance.getEncounterId());
                ps.setString(i++, instance.getPathwayCode());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
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
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local patient instance failed: " + ex.getMessage(), ex);
        }
    }

    private void saveTaskStateLocal(PatientTaskState taskState) {
        String updateSql = "UPDATE pe_patient_task_state SET task_name=?, task_type=?, status=?, result_json=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE instance_id=? AND node_code=? AND task_code=?";
        String insertSql = "INSERT INTO pe_patient_task_state " +
                "(id, instance_id, node_code, task_code, task_name, task_type, status, result_json, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        long instanceId = extractNumericId(taskState.getInstanceId());
        String resultJson = toJson(taskState.getResult());
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, taskState.getTaskName());
                ps.setString(i++, taskState.getTaskType());
                ps.setString(i++, taskState.getStatus());
                ps.setString(i++, resultJson);
                ps.setLong(i++, instanceId);
                ps.setString(i++, taskState.getNodeCode());
                ps.setString(i++, taskState.getTaskCode());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setLong(i++, instanceId);
                    ps.setString(i++, taskState.getNodeCode());
                    ps.setString(i++, taskState.getTaskCode());
                    ps.setString(i++, taskState.getTaskName());
                    ps.setString(i++, taskState.getTaskType());
                    ps.setString(i++, taskState.getStatus());
                    ps.setString(i++, resultJson);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local task state failed: " + ex.getMessage(), ex);
        }
    }

    private void saveRuleDefinitionLocal(RuleDefinition definition, String approvedBy) {
        String updateSql = "UPDATE re_rule_def SET rule_name=?, rule_type=?, status=?, severity=?, rule_json=?, approved_by=?, " +
                "approved_time=? WHERE tenant_id=? AND org_code=? AND rule_code=? AND version_no=?";
        String insertSql = "INSERT INTO re_rule_def " +
                "(id, tenant_id, org_code, rule_code, rule_name, rule_type, version_no, status, severity, rule_json, created_time, approved_by, approved_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)";
        String json = toJson(definition.getRuleJson());
        String tenantId = string(definition.getTenantId(), "default");
        String orgCode = string(definition.getLegacyOrgCode(), string(definition.getHospitalCode(), "ZYHOSPITAL"));
        Timestamp approvedTime = "PUBLISHED".equals(definition.getStatus()) ? new Timestamp(System.currentTimeMillis()) : null;
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, definition.getRuleName());
                ps.setString(i++, definition.getRuleType());
                ps.setString(i++, definition.getStatus());
                ps.setString(i++, definition.getSeverity());
                ps.setString(i++, json);
                ps.setString(i++, approvedBy);
                ps.setTimestamp(i++, approvedTime);
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, definition.getRuleCode());
                ps.setString(i++, definition.getVersionNo());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, tenantId);
                    ps.setString(i++, orgCode);
                    ps.setString(i++, definition.getRuleCode());
                    ps.setString(i++, definition.getRuleName());
                    ps.setString(i++, definition.getRuleType());
                    ps.setString(i++, definition.getVersionNo());
                    ps.setString(i++, definition.getStatus());
                    ps.setString(i++, definition.getSeverity());
                    ps.setString(i++, json);
                    ps.setString(i++, approvedBy);
                    ps.setTimestamp(i++, approvedTime);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local rule definition failed: " + ex.getMessage(), ex);
        }
    }

    private void saveSourceDocumentLocal(SourceDocument document) {
        String updateSql = "UPDATE src_document SET title=?, source_type=?, source_uri=?, publisher=?, " +
                "effective_date=?, expiry_date=?, review_status=?, reviewed_by=?, reviewed_time=?, content_hash=?, " +
                "metadata_json=?, created_by=COALESCE(?, created_by), updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND document_code=?";
        String insertSql = "INSERT INTO src_document (id, tenant_id, document_code, title, source_type, source_uri, " +
                "publisher, effective_date, expiry_date, review_status, reviewed_by, reviewed_time, content_hash, " +
                "metadata_json, created_by, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        try (Connection connection = connection()) {
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                setSourceDocumentUpdateValues(ps, document, i);
                i += 12;
                ps.setString(i++, string(document.getTenantId(), "default"));
                ps.setString(i++, document.getDocumentCode());
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, string(document.getTenantId(), "default"));
                    ps.setString(i++, document.getDocumentCode());
                    ps.setString(i++, document.getTitle());
                    ps.setString(i++, document.getSourceType());
                    ps.setString(i++, document.getSourceUri());
                    ps.setString(i++, document.getPublisher());
                    ps.setDate(i++, parseSqlDate(document.getEffectiveDate()));
                    ps.setDate(i++, parseSqlDate(document.getExpiryDate()));
                    ps.setString(i++, document.getReviewStatus());
                    ps.setString(i++, document.getReviewedBy());
                    ps.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
                    ps.setString(i++, document.getContentHash());
                    ps.setString(i++, toJson(document.getMetadata()));
                    ps.setString(i++, document.getCreatedBy());
                    ps.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save local source document failed: " + ex.getMessage(), ex);
        }
    }

    private void setSourceDocumentUpdateValues(PreparedStatement ps, SourceDocument document, int startIndex)
            throws SQLException {
        int i = startIndex;
        ps.setString(i++, document.getTitle());
        ps.setString(i++, document.getSourceType());
        ps.setString(i++, document.getSourceUri());
        ps.setString(i++, document.getPublisher());
        ps.setDate(i++, parseSqlDate(document.getEffectiveDate()));
        ps.setDate(i++, parseSqlDate(document.getExpiryDate()));
        ps.setString(i++, document.getReviewStatus());
        ps.setString(i++, document.getReviewedBy());
        ps.setTimestamp(i++, parseTimestamp(document.getReviewedTime()));
        ps.setString(i++, document.getContentHash());
        ps.setString(i++, toJson(document.getMetadata()));
        ps.setString(i++, document.getCreatedBy());
    }

    @SuppressWarnings("unchecked")
    private SourceDocument toSourceDocument(ResultSet rs) throws SQLException {
        SourceDocument document = new SourceDocument();
        document.setTenantId(rs.getString("tenant_id"));
        document.setDocumentCode(rs.getString("document_code"));
        document.setTitle(rs.getString("title"));
        document.setSourceType(rs.getString("source_type"));
        document.setSourceUri(rs.getString("source_uri"));
        document.setPublisher(rs.getString("publisher"));
        document.setEffectiveDate(formatDate(rs.getDate("effective_date")));
        document.setExpiryDate(formatDate(rs.getDate("expiry_date")));
        document.setReviewStatus(rs.getString("review_status"));
        document.setReviewedBy(rs.getString("reviewed_by"));
        document.setReviewedTime(formatTimestamp(rs.getTimestamp("reviewed_time")));
        document.setContentHash(rs.getString("content_hash"));
        String metadataJson = rs.getString("metadata_json");
        if (metadataJson != null && !metadataJson.trim().isEmpty()) {
            try {
                document.setMetadata(objectMapper.readValue(metadataJson, LinkedHashMap.class));
            } catch (IOException ex) {
                document.setMetadata(new LinkedHashMap<String, Object>());
            }
        }
        document.setCreatedBy(rs.getString("created_by"));
        document.setCreatedTime(formatTimestamp(rs.getTimestamp("created_time")));
        document.setUpdatedTime(formatTimestamp(rs.getTimestamp("updated_time")));
        return document;
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
        loadDriver();
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

    private void loadDriver() throws SQLException {
        String driverClass = properties.localFileDatabase() ? "org.h2.Driver" : "oracle.jdbc.OracleDriver";
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException ex) {
            throw new SQLException(driverClass + " not found", ex);
        }
    }

    private List<String> loadLocalSchemaStatements() {
        // 按顺序加载本地开发库 DDL：核心表 + 业务追加表。新增 DDL 需要在此显式注册。
        String[] resources = new String[] {
                "/db/local/h2_core_ddl.sql",
                "/db/local/re_rule_eval_result_ddl.sql"
        };
        List<String> statements = new ArrayList<String>();
        for (String resource : resources) {
            statements.addAll(loadLocalSchemaResource(resource));
        }
        return statements;
    }

    private List<String> loadLocalSchemaResource(String resource) {
        InputStream stream = EnginePersistenceService.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("local database schema resource not found: " + resource);
        }
        StringBuilder sql = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("read local database schema failed: " + resource, ex);
        }
        List<String> statements = new ArrayList<String>();
        String[] parts = sql.toString().split(";");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                statements.add(part);
            }
        }
        return statements;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("JSON serialization failed", ex);
        }
    }

    /**
     * 提供给其他 Service 复用的安全 JSON 序列化：null 返回 null，序列化异常返回 null 不抛出，
     * 避免持久化层因输入异常打断业务主链路。
     */
    public String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    /**
     * 解析 Object 为 double，null 或不可解析时返回 defaultValue。
     * 用于 TERM-002 等场景下从 Map 提取置信度等浮点字段。
     */
    private double doubleValue(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
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

    private java.sql.Date parseSqlDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(LocalDate.parse(value.trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Timestamp parseTimestamp(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        try {
            return Timestamp.from(OffsetDateTime.parse(text).toInstant());
        } catch (RuntimeException ignored) {
            try {
                return Timestamp.valueOf(text.replace('T', ' '));
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }

    private String formatDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate().toString();
    }

    private String formatTimestamp(Timestamp value) {
        return value == null
                ? null
                : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.toInstant().atOffset(ZoneOffset.UTC));
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

    // =========================================================================
    // 术语治理队列持久化
    // =========================================================================

    /**
     * 保存或更新未映射治理队列记录。
     * 同一 tenant+sourceSystem+sourceCode+conceptType+governanceStatus 只保留一条，
     * 重复出现时 occurrence_count +1 并更新 last_occurrence_time。
     */
    public void saveUnmappedQueueEntry(Map<String, Object> entry) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveUnmappedQueueEntryLocal(entry);
            return;
        }
        String sql = "MERGE INTO tm_unmapped_queue t " +
                "USING (SELECT ? AS tenant_id, ? AS source_system, ? AS source_code, ? AS concept_type, " +
                "? AS governance_status FROM dual) s " +
                "ON (t.tenant_id=s.tenant_id AND t.source_system=s.source_system " +
                "AND t.source_code=s.source_code AND t.concept_type=s.concept_type " +
                "AND t.governance_status=s.governance_status) " +
                "WHEN MATCHED THEN UPDATE SET t.occurrence_count=t.occurrence_count+1, " +
                "t.last_occurrence_time=SYSTIMESTAMP, t.source_name=COALESCE(?, t.source_name), " +
                "t.updated_time=SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (id, tenant_id, queue_id, source_system, source_code, source_name, " +
                "concept_type, governance_status, proposed_standard_code, proposed_standard_name, " +
                "proposed_confidence, proposed_mapping_source, occurrence_count, last_occurrence_time, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, SYSTIMESTAMP, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            // ON 条件
            ps.setString(i++, string(entry.get("tenant_id"), "default"));
            ps.setString(i++, string(entry.get("source_system"), ""));
            ps.setString(i++, string(entry.get("source_code"), ""));
            ps.setString(i++, string(entry.get("concept_type"), ""));
            ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
            // MATCHED UPDATE
            ps.setString(i++, string(entry.get("source_name"), null));
            // NOT MATCHED INSERT
            ps.setLong(i++, Ids.next());
            ps.setString(i++, string(entry.get("tenant_id"), "default"));
            ps.setString(i++, string(entry.get("queue_id"), ""));
            ps.setString(i++, string(entry.get("source_system"), ""));
            ps.setString(i++, string(entry.get("source_code"), ""));
            ps.setString(i++, string(entry.get("source_name"), null));
            ps.setString(i++, string(entry.get("concept_type"), ""));
            ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
            ps.setString(i++, string(entry.get("proposed_standard_code"), null));
            ps.setString(i++, string(entry.get("proposed_standard_name"), null));
            ps.setDouble(i++, doubleValue(entry.get("proposed_confidence"), 0));
            ps.setString(i++, string(entry.get("proposed_mapping_source"), null));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save unmapped queue entry failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 查询未映射治理队列。
     * 支持按 governance_status / source_system / concept_type 过滤。
     */
    public List<Map<String, Object>> listUnmappedQueue(String tenantId, String governanceStatus,
                                                        String sourceSystem, String conceptType, int limit) {
        if (!enabled()) {
            return new ArrayList<Map<String, Object>>();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT id, tenant_id, queue_id, source_system, source_code, source_name, concept_type, " +
                "governance_status, proposed_standard_code, proposed_standard_name, proposed_confidence, " +
                "proposed_mapping_source, reviewed_by, reviewed_time, review_comment, " +
                "occurrence_count, last_occurrence_time, created_time, updated_time " +
                "FROM tm_unmapped_queue WHERE tenant_id=?");
        List<String> params = new ArrayList<String>();
        params.add(string(tenantId, "default"));
        if (governanceStatus != null && !governanceStatus.isEmpty()) {
            sql.append(" AND governance_status=?");
            params.add(governanceStatus);
        }
        if (sourceSystem != null && !sourceSystem.isEmpty()) {
            sql.append(" AND source_system=?");
            params.add(sourceSystem);
        }
        if (conceptType != null && !conceptType.isEmpty()) {
            sql.append(" AND concept_type=?");
            params.add(conceptType);
        }
        sql.append(" ORDER BY last_occurrence_time DESC");
        if (limit > 0) {
            sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
        }
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(toUnmappedQueueEntry(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("list unmapped queue failed: " + ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * 更新治理队列记录状态（审批/驳回）。
     */
    public void updateUnmappedQueueStatus(String queueId, String tenantId, String governanceStatus,
                                            String reviewedBy, String reviewComment) {
        if (!enabled()) {
            return;
        }
        String sql = "UPDATE tm_unmapped_queue SET governance_status=?, reviewed_by=?, " +
                "reviewed_time=CURRENT_TIMESTAMP, review_comment=?, updated_time=CURRENT_TIMESTAMP " +
                "WHERE queue_id=? AND tenant_id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, governanceStatus);
            ps.setString(2, reviewedBy);
            ps.setString(3, reviewComment);
            ps.setString(4, queueId);
            ps.setString(5, string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("update unmapped queue status failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * 删除未映射治理队列中的指定条目（用于 REJECTED 后清理，防止表膨胀）。
     */
    public void deleteUnmappedQueueEntry(String queueId, String tenantId) {
        if (!enabled()) {
            return;
        }
        String sql = "DELETE FROM tm_unmapped_queue WHERE queue_id=? AND tenant_id=?";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, queueId);
            ps.setString(2, string(tenantId, com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("delete unmapped queue entry failed: " + ex.getMessage(), ex);
        }
    }

    private void saveUnmappedQueueEntryLocal(Map<String, Object> entry) {
        // H2 不支持 Oracle MERGE 的 dual 写法，统一用 UPDATE + INSERT 两阶段。
        // 用事务包裹避免：UPDATE 受影响 0 行 → INSERT 期间另一线程已插入相同 (tenant,source,system,code,type)
        // 导致 INSERT 撞 UNIQUE 约束，或反过来 INSERT 成功后 UPDATE 计数丢失。
        String updateSql = "UPDATE tm_unmapped_queue SET occurrence_count=occurrence_count+1, " +
                "last_occurrence_time=CURRENT_TIMESTAMP, source_name=COALESCE(?, source_name), " +
                "updated_time=CURRENT_TIMESTAMP " +
                "WHERE tenant_id=? AND source_system=? AND source_code=? AND concept_type=? AND governance_status=?";
        String insertSql = "INSERT INTO tm_unmapped_queue (id, tenant_id, queue_id, source_system, source_code, " +
                "source_name, concept_type, governance_status, proposed_standard_code, proposed_standard_name, " +
                "proposed_confidence, proposed_mapping_source, occurrence_count, last_occurrence_time, " +
                "created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        Connection connection = null;
        boolean prevAutoCommit = true;
        try {
            connection = connection();
            prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            int updated;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, string(entry.get("source_name"), null));
                ps.setString(i++, string(entry.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
                ps.setString(i++, string(entry.get("source_system"), ""));
                ps.setString(i++, string(entry.get("source_code"), ""));
                ps.setString(i++, string(entry.get("concept_type"), ""));
                ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                updated = ps.executeUpdate();
            }
            if (updated == 0) {
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ps.setLong(i++, Ids.next());
                    ps.setString(i++, string(entry.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
                    ps.setString(i++, string(entry.get("queue_id"), ""));
                    ps.setString(i++, string(entry.get("source_system"), ""));
                    ps.setString(i++, string(entry.get("source_code"), ""));
                    ps.setString(i++, string(entry.get("source_name"), null));
                    ps.setString(i++, string(entry.get("concept_type"), ""));
                    ps.setString(i++, string(entry.get("governance_status"), "PENDING_MAPPING"));
                    ps.setString(i++, string(entry.get("proposed_standard_code"), null));
                    ps.setString(i++, string(entry.get("proposed_standard_name"), null));
                    ps.setDouble(i++, doubleValue(entry.get("proposed_confidence"), 0));
                    ps.setString(i++, string(entry.get("proposed_mapping_source"), null));
                    ps.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException rollbackEx) { /* ignore rollback failure */ }
            }
            throw new IllegalStateException("save unmapped queue entry local failed: " + ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(prevAutoCommit);
                    connection.close();
                } catch (SQLException closeEx) { /* ignore */ }
            }
        }
    }

    private Map<String, Object> toUnmappedQueueEntry(ResultSet rs) throws SQLException {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("id", rs.getLong("id"));
        entry.put("tenant_id", rs.getString("tenant_id"));
        entry.put("queue_id", rs.getString("queue_id"));
        entry.put("source_system", rs.getString("source_system"));
        entry.put("source_code", rs.getString("source_code"));
        entry.put("source_name", rs.getString("source_name"));
        entry.put("concept_type", rs.getString("concept_type"));
        entry.put("governance_status", rs.getString("governance_status"));
        entry.put("proposed_standard_code", rs.getString("proposed_standard_code"));
        entry.put("proposed_standard_name", rs.getString("proposed_standard_name"));
        entry.put("proposed_confidence", rs.getDouble("proposed_confidence"));
        entry.put("proposed_mapping_source", rs.getString("proposed_mapping_source"));
        entry.put("reviewed_by", rs.getString("reviewed_by"));
        entry.put("reviewed_time", formatTimestamp(rs.getTimestamp("reviewed_time")));
        entry.put("review_comment", rs.getString("review_comment"));
        entry.put("occurrence_count", rs.getInt("occurrence_count"));
        entry.put("last_occurrence_time", formatTimestamp(rs.getTimestamp("last_occurrence_time")));
        entry.put("created_time", formatTimestamp(rs.getTimestamp("created_time")));
        entry.put("updated_time", formatTimestamp(rs.getTimestamp("updated_time")));
        return entry;
    }
}
