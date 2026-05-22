package com.medkernel.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.common.TraceContext;
import com.medkernel.dto.RuleResult;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.PersistenceRepositorySupport;
import com.medkernel.util.ClinicalFactUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

@Repository
public class RuleExecLogRepository extends PersistenceRepositorySupport {

    public RuleExecLogRepository(EnginePersistenceProperties properties,
                                     ObjectMapper objectMapper,
                                     DataSource dataSource,
                                     IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    /** 保存规则执行日志（re_rule_exec_log），仅 INSERT。不含机构字段的重载版本。 */
    public void saveRuleExecLog(RuleResult result, String ruleVersion, Map<String, Object> patientContext,
                                long elapsedMs, String resultStatus, String errorCode, String errorMessage) {
        saveRuleExecLog(result, ruleVersion, patientContext, elapsedMs, resultStatus, errorCode, errorMessage, null);
    }

    /** 保存规则执行日志（re_rule_exec_log），仅 INSERT。支持机构字段。 */
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
        String tenantId = orgValue(orgFields, "tenant_id", "tenantId");
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, nextId(tenantId));
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
            ps.setString(i++, tenantId);
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

}
