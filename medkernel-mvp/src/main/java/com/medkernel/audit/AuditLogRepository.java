package com.medkernel.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medkernel.common.IdAllocatorRepository;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.PersistenceRepositorySupport;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AuditLogRepository extends PersistenceRepositorySupport {
    private static final int MAX_AUDIT_RECORDS = 500;

    private final List<Map<String, Object>> auditRecords =
            Collections.synchronizedList(new ArrayList<Map<String, Object>>());

    public AuditLogRepository(EnginePersistenceProperties properties,
                                 ObjectMapper objectMapper,
                                 DataSource dataSource,
                                 IdAllocatorRepository idAllocatorRepository) {
        super(properties, objectMapper, dataSource, idAllocatorRepository);
    }

    /** 保存审计日志（engine_audit_log），仅 INSERT。同时写入内存缓冲区。 */
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
        String tenantId = orgValue(detail, "tenant_id", "tenantId");
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, nextId(tenantId));
            ps.setString(i++, traceId);
            ps.setString(i++, engineType);
            ps.setString(i++, actionType);
            ps.setString(i++, targetType);
            ps.setString(i++, targetCode);
            ps.setString(i++, patientId);
            ps.setString(i++, encounterId);
            ps.setString(i++, operatorId);
            ps.setString(i++, tenantId);
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

    /** 查询审计日志，支持多维度过滤（traceId/engineType/actionType/targetType/targetCode/patientId/encounterId/operatorId 及机构字段）。 */
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

    /** 按维度汇总审计日志，返回各维度的计数统计。 */
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

}
