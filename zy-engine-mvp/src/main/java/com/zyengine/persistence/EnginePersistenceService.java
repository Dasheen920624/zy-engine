package com.zyengine.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyengine.common.TraceContext;
import com.zyengine.dto.PatientPathwayInstance;
import com.zyengine.dto.RecommendationCard;
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
            ps.setString(i++, nodeCode);
            ps.setString(i++, status);
            ps.setTimestamp(i++, "COMPLETED".equals(status) ? new Timestamp(System.currentTimeMillis()) : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("save node state failed: " + ex.getMessage(), ex);
        }
    }

    private Connection connection() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Oracle JDBC driver not found", ex);
        }
        return DriverManager.getConnection(properties.getUrl(), properties.getUsername(), properties.getPassword());
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

