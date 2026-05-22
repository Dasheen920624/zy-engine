package com.medkernel.pathway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.common.TraceContext;
import com.medkernel.dto.PatientPathwayInstance;
import com.medkernel.dto.PatientTaskState;
import com.medkernel.dto.PathwayVariationRecord;
import com.medkernel.dto.RecommendationCard;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.PersistenceRepositorySupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

@Repository
public class PathwayInstanceRepository extends PersistenceRepositorySupport {

    public PathwayInstanceRepository(EnginePersistenceProperties properties,
                                     ObjectMapper objectMapper,
                                     DataSource dataSource,
                                     IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    /** 保存推荐卡记录（pe_recommendation_record），仅 INSERT。 */
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
            ps.setLong(i++, nextId());
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

    /** 保存或更新患者路径实例（pe_patient_instance），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void savePatientInstance(PatientPathwayInstance instance, String admittedBy) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            savePatientInstanceLocal(instance, admittedBy);
            return;
        }
        String updateSql = "UPDATE pe_patient_instance SET current_node_code=?, updated_time=SYSTIMESTAMP " +
                "WHERE tenant_id=? AND org_code=? AND encounter_id=? AND pathway_code=? AND status='ACTIVE'";
        String insertSql = "INSERT INTO pe_patient_instance (id, tenant_id, org_code, patient_id, encounter_id, pathway_code, version_no, status, current_node_code, admitted_by, admission_time, created_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            long id = extractNumericId(instance.getInstanceId());
            String tenantId = string(instance.getTenantId(), "default");
            String orgCode = string(instance.getLegacyOrgCode(), string(instance.getHospitalCode(), "ZYHOSPITAL"));
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, instance.getCurrentNodeCode());
                ps.setString(i++, tenantId);
                ps.setString(i++, orgCode);
                ps.setString(i++, instance.getEncounterId());
                ps.setString(i++, instance.getPathwayCode());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, id);
                    ips.setString(i++, tenantId);
                    ips.setString(i++, orgCode);
                    ips.setString(i++, instance.getPatientId());
                    ips.setString(i++, instance.getEncounterId());
                    ips.setString(i++, instance.getPathwayCode());
                    ips.setString(i++, instance.getVersionNo());
                    ips.setString(i++, instance.getCurrentNodeCode());
                    ips.setString(i++, admittedBy);
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save patient instance failed: " + ex.getMessage(), ex);
        }
    }

    /** 更新节点状态（pe_patient_node_state），nodeName 默认取 nodeCode。 */
    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String status) {
        updateNodeState(instance, nodeCode, nodeCode, status);
    }

    /** 保存节点状态记录（pe_patient_node_state），仅 INSERT。 */
    public void updateNodeState(PatientPathwayInstance instance, String nodeCode, String nodeName, String status) {
        if (!enabled()) {
            return;
        }
        String sql = "INSERT INTO pe_patient_node_state (id, instance_id, node_code, node_name, status, enter_time, complete_time, timeout_flag, created_time) " +
                "VALUES (?, ?, ?, ?, ?, SYSTIMESTAMP, ?, 0, SYSTIMESTAMP)";
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, nextId(string(instance.getTenantId(), "default")));
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

    /** 保存或更新任务状态（pe_patient_task_state），使用 UPDATE+INSERT 两阶段实现 UPSERT。 */
    public void saveTaskState(PatientTaskState taskState) {
        if (!enabled()) {
            return;
        }
        if (properties.localFileDatabase()) {
            saveTaskStateLocal(taskState);
            return;
        }
        String updateSql = "UPDATE pe_patient_task_state SET task_name=?, task_type=?, status=?, result_json=?, updated_time=SYSTIMESTAMP " +
                "WHERE instance_id=? AND node_code=? AND task_code=?";
        String insertSql = "INSERT INTO pe_patient_task_state (id, instance_id, node_code, task_code, task_name, task_type, status, result_json, created_time, updated_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, SYSTIMESTAMP, SYSTIMESTAMP)";
        try (Connection connection = connection()) {
            long instanceId = extractNumericId(taskState.getInstanceId());
            String resultJson = toJson(taskState.getResult());
            int affected;
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                int i = 1;
                ps.setString(i++, taskState.getTaskName());
                ps.setString(i++, taskState.getTaskType());
                ps.setString(i++, taskState.getStatus());
                ps.setString(i++, resultJson);
                ps.setLong(i++, instanceId);
                ps.setString(i++, taskState.getNodeCode());
                ps.setString(i++, taskState.getTaskCode());
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                try (PreparedStatement ips = connection.prepareStatement(insertSql)) {
                    int i = 1;
                    ips.setLong(i++, nextId());
                    ips.setLong(i++, instanceId);
                    ips.setString(i++, taskState.getNodeCode());
                    ips.setString(i++, taskState.getTaskCode());
                    ips.setString(i++, taskState.getTaskName());
                    ips.setString(i++, taskState.getTaskType());
                    ips.setString(i++, taskState.getStatus());
                    ips.setString(i++, resultJson);
                    ips.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("save task state failed: " + ex.getMessage(), ex);
        }
    }

    /** 保存变异记录（pe_variation_record），仅 INSERT。 */
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
            ps.setLong(i++, nextId(tenantId));
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
                    ps.setLong(i++, nextId());
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

}
