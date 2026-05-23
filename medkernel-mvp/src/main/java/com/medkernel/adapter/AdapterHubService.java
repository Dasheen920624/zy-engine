package com.medkernel.adapter;

import com.medkernel.adapter.dto.SampleRow;
import com.medkernel.common.TraceContext;
import com.medkernel.dto.AdapterDefinitionImportRequest;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdapterHubService {
    private final EnginePersistenceService persistenceService;
    private final AdapterExecutionLogService executionLogService;
    private final List<AdapterMockDataProvider> mockDataProviders;
    private final AdapterDefinitionSeeder definitionSeeder;
    private final Map<String, AdapterQueryDefinition> queryDefinitions =
            new ConcurrentHashMap<String, AdapterQueryDefinition>();

    public AdapterHubService(EnginePersistenceService persistenceService,
                             AdapterExecutionLogService executionLogService,
                             List<AdapterMockDataProvider> mockDataProviders,
                             AdapterDefinitionSeeder definitionSeeder) {
        this.persistenceService = persistenceService;
        this.executionLogService = executionLogService;
        this.mockDataProviders = mockDataProviders;
        this.definitionSeeder = definitionSeeder;
        definitionSeeder.seedDefinitions(queryDefinitions);
    }

    public Map<String, Object> query(Map<String, Object> request, String tenantId, String hospitalCode) {
        long start = System.currentTimeMillis();
        String adapterCode = required(request, "adapter_code");
        String queryCode = required(request, "query_code");
        Map<String, Object> params = map(request.get("params"));
        AdapterQueryDefinition definition = queryDefinitions.get(key(tenantId, hospitalCode, adapterCode, queryCode));
        Map<String, Object> result = definition == null
                ? unsupported(adapterCode, queryCode, params)
                : success(definition, params, System.currentTimeMillis() - start);
        audit(adapterCode, queryCode, params, result);
        return result;
    }

    public List<Map<String, Object>> importDefinitions(AdapterDefinitionImportRequest request, String tenantId, String hospitalCode) {
        List<Map<String, Object>> entries = normalizeDefinitions(request);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("adapter definitions list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<AdapterQueryDefinition> staged = new ArrayList<AdapterQueryDefinition>();
        for (int index = 0; index < entries.size(); index++) {
            Map<String, Object> entry = entries.get(index);
            try {
                staged.add(toDefinition(entry));
            } catch (IllegalArgumentException ex) {
                errors.add("definitions[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            // 与路径配置导入一致，校验失败时整体回退，不污染已注册的适配器查询定义。
            throw new IllegalArgumentException("adapter definitions invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (AdapterQueryDefinition definition : staged) {
            queryDefinitions.put(key(tenantId, hospitalCode, definition.adapterCode, definition.queryCode), definition);
            imported.add(view(definition));
        }
        return imported;
    }

    public List<Map<String, Object>> listDefinitions(String tenantId, String hospitalCode) {
        String prefix = canonical(tenantId) + "::" + canonical(hospitalCode) + "::";
        List<AdapterQueryDefinition> list = new ArrayList<AdapterQueryDefinition>();
        for (Map.Entry<String, AdapterQueryDefinition> entry : queryDefinitions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                list.add(entry.getValue());
            }
        }
        Collections.sort(list, new Comparator<AdapterQueryDefinition>() {
            @Override
            public int compare(AdapterQueryDefinition left, AdapterQueryDefinition right) {
                int byAdapter = left.adapterCode.compareTo(right.adapterCode);
                return byAdapter != 0 ? byAdapter : left.queryCode.compareTo(right.queryCode);
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (AdapterQueryDefinition definition : list) {
            result.add(view(definition));
        }
        return result;
    }

    public Map<String, Object> getDefinition(String adapterCode, String queryCode, String tenantId, String hospitalCode) {
        AdapterQueryDefinition definition = queryDefinitions.get(key(tenantId, hospitalCode, adapterCode, queryCode));
        if (definition == null) {
            throw new IllegalArgumentException("adapter definition not found: "
                    + canonical(adapterCode) + "/" + canonical(queryCode));
        }
        return view(definition);
    }

    Map<String, AdapterQueryDefinition> getQueryDefinitions() {
        return queryDefinitions;
    }

    private Map<String, Object> success(AdapterQueryDefinition definition, Map<String, Object> params, long elapsedMs) {
        List<Map<String, Object>> rows = rows(definition, params);
        Map<String, Object> result = base(definition.adapterCode, definition.queryCode);
        result.put("status", "SUCCESS");
        result.put("adapter_type", definition.adapterType);
        result.put("source_system", definition.sourceSystem);
        result.put("query_name", definition.queryName);
        result.put("mock", true);
        result.put("message", rows.isEmpty()
                ? "适配器查询已注册但暂无 Mock 行数据，请在 import 时通过 sample_rows 或后续 SDK 接入真实取数。"
                : definition.description);
        result.put("elapsed_ms", elapsedMs);
        result.put("row_count", rows.size());
        result.put("rows", rows);
        result.put("schema", definition.schema);
        return result;
    }

    private Map<String, Object> unsupported(String adapterCode, String queryCode, Map<String, Object> params) {
        Map<String, Object> result = base(canonical(adapterCode), canonical(queryCode));
        result.put("status", "UNSUPPORTED");
        result.put("adapter_type", null);
        result.put("source_system", null);
        result.put("query_name", null);
        result.put("mock", true);
        result.put("message", "未配置该适配器查询 Mock。");
        result.put("elapsed_ms", 0);
        result.put("row_count", 0);
        result.put("rows", new ArrayList<Map<String, Object>>());
        result.put("params", params);
        result.put("supported_queries", supportedQueries());
        return result;
    }

    private Map<String, Object> base(String adapterCode, String queryCode) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("adapter_code", adapterCode);
        result.put("query_code", queryCode);
        result.put("trace_id", TraceContext.getTraceId());
        return result;
    }

    private List<Map<String, Object>> rows(AdapterQueryDefinition definition, Map<String, Object> params) {
        if (definition.sampleRows != null && !definition.sampleRows.isEmpty()) {
            // 导入定义时如果带 sample_rows，则优先用配置数据，便于规则/路径联调时控制Mock返回。
            return rowsWithPatientContext(definition.sampleRows, params);
        }
        String adapterCode = definition.adapterCode;
        String queryCode = definition.queryCode;
        for (AdapterMockDataProvider provider : mockDataProviders) {
            if (provider.supports(adapterCode, queryCode)) {
                return provider.provideRows(adapterCode, queryCode, params);
            }
        }
        return new ArrayList<Map<String, Object>>();
    }

    private List<Map<String, Object>> rowsWithPatientContext(List<Map<String, Object>> templateRows,
                                                              Map<String, Object> params) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        Map<String, Object> patientCtx = patientRow(params);
        for (Map<String, Object> template : templateRows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>(patientCtx);
            row.putAll(template);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> patientRow(Map<String, Object> params) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("patient_id", string(params.get("patient_id"), "P_AMI_001"));
        row.put("encounter_id", string(params.get("encounter_id"), "E_AMI_001"));
        row.put("tenant_id", string(params.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
        row.put("org_code", string(params.get("org_code"), com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE));
        return row;
    }

    private List<Map<String, Object>> supportedQueries() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (AdapterQueryDefinition definition : queryDefinitions.values()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("adapter_code", definition.adapterCode);
            item.put("adapter_name", definition.adapterName);
            item.put("adapter_type", definition.adapterType);
            item.put("query_code", definition.queryCode);
            item.put("query_name", definition.queryName);
            list.add(item);
        }
        return list;
    }

    private Map<String, Object> view(AdapterQueryDefinition definition) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("adapter_code", definition.adapterCode);
        view.put("adapter_name", definition.adapterName);
        view.put("adapter_type", definition.adapterType);
        view.put("source_system", definition.sourceSystem);
        view.put("query_code", definition.queryCode);
        view.put("query_name", definition.queryName);
        view.put("description", definition.description);
        view.put("schema", definition.schema);
        view.put("source", definition.source);
        view.put("has_sample_rows", definition.sampleRows != null && !definition.sampleRows.isEmpty());
        return view;
    }

    private void audit(String adapterCode, String queryCode, Map<String, Object> params, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("adapter_code", canonical(adapterCode));
        detail.put("query_code", canonical(queryCode));
        detail.put("status", result.get("status"));
        detail.put("mock", result.get("mock"));
        detail.put("row_count", result.get("row_count"));
        detail.put("elapsed_ms", result.get("elapsed_ms"));
        try {
            persistenceService.saveAuditLog("ADAPTER", "QUERY", "ADAPTER_QUERY",
                    canonical(adapterCode) + "." + canonical(queryCode),
                    string(params.get("patient_id"), null),
                    string(params.get("encounter_id"), null),
                    string(params.get("operator_id"), null),
                    detail);
        } catch (RuntimeException ex) {
            // 适配器Mock查询不能因为审计失败影响演示链路；但失败必须可见，便于运维诊断审计基础设施问题。
            org.slf4j.LoggerFactory.getLogger(AdapterHubService.class)
                    .warn("[traceId={}] adapter audit log persistence failed: {}",
                            com.medkernel.common.TraceContext.getTraceId(), ex.getMessage());
        }

        // 同时记录到适配器执行日志
        try {
            com.medkernel.adapter.entity.AdapterCallLogEntity logEntry = new com.medkernel.adapter.entity.AdapterCallLogEntity();
            logEntry.setTraceId(TraceContext.getTraceId());
            logEntry.setAdapterCode(canonical(adapterCode));
            logEntry.setQueryCode(canonical(queryCode));
            logEntry.setStatus(string(result.get("status"), "UNKNOWN"));
            logEntry.setElapsedMs(result.get("elapsed_ms") instanceof Number
                    ? ((Number) result.get("elapsed_ms")).longValue() : 0L);
            logEntry.setPatientId(string(params.get("patient_id"), null));
            logEntry.setEncounterId(string(params.get("encounter_id"), null));
            logEntry.setOperatorId(string(params.get("operator_id"), null));
            if (params != null) {
                try {
                    logEntry.setRequestParams(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(params));
                } catch (Exception ignore) { /* non-critical */ }
            }
            if (result != null) {
                try {
                    logEntry.setResponseData(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
                } catch (Exception ignore) { /* non-critical */ }
            }
            executionLogService.recordCallLog(logEntry);
        } catch (RuntimeException ex) {
            org.slf4j.LoggerFactory.getLogger(AdapterHubService.class)
                    .warn("[traceId={}] adapter execution log recording failed: {}",
                            TraceContext.getTraceId(), ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeDefinitions(AdapterDefinitionImportRequest request) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (request.getDefinitions() == null) {
            return list;
        }
        for (AdapterDefinitionImportRequest.AdapterDefinitionItem item : request.getDefinitions()) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            if (item.getAdapter_code() != null) entry.put("adapter_code", item.getAdapter_code());
            if (item.getAdapter_name() != null) entry.put("adapter_name", item.getAdapter_name());
            if (item.getAdapter_type() != null) entry.put("adapter_type", item.getAdapter_type());
            if (item.getSource_system() != null) entry.put("source_system", item.getSource_system());
            if (item.getQuery_code() != null) entry.put("query_code", item.getQuery_code());
            if (item.getQuery_name() != null) entry.put("query_name", item.getQuery_name());
            if (item.getDescription() != null) entry.put("description", item.getDescription());
            if (item.getSchema() != null) entry.put("schema", item.getSchema());
            if (item.getSample_rows() != null) {
                List<Map<String, Object>> sampleRows = new ArrayList<Map<String, Object>>();
                for (SampleRow row : item.getSample_rows()) {
                    Map<String, Object> rowMap = new LinkedHashMap<String, Object>();
                    if (row.getRowId() != null) rowMap.put("rowId", row.getRowId());
                    if (row.getColumns() != null) rowMap.putAll(row.getColumns());
                    sampleRows.add(rowMap);
                }
                entry.put("sample_rows", sampleRows);
            }
            if (item.getSource() != null) entry.put("source", item.getSource());
            list.add(entry);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private AdapterQueryDefinition toDefinition(Map<String, Object> entry) {
        AdapterQueryDefinition definition = new AdapterQueryDefinition();
        definition.adapterCode = canonical(requireField(entry, "adapter_code"));
        definition.adapterName = string(entry.get("adapter_name"), definition.adapterCode);
        definition.adapterType = canonical(string(entry.get("adapter_type"), "REST"));
        definition.sourceSystem = canonical(string(entry.get("source_system"), definition.adapterCode));
        definition.queryCode = canonical(requireField(entry, "query_code"));
        definition.queryName = string(entry.get("query_name"), definition.queryCode);
        definition.description = string(entry.get("description"), null);
        Object schema = entry.get("schema");
        List<String> schemaList = new ArrayList<String>();
        if (schema instanceof Collection) {
            for (Object item : (Collection<?>) schema) {
                if (item != null) {
                    schemaList.add(String.valueOf(item));
                }
            }
        }
        definition.schema = schemaList;
        Object sampleRows = entry.get("sample_rows");
        if (sampleRows instanceof Collection) {
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            for (Object row : (Collection<?>) sampleRows) {
                if (row instanceof Map) {
                    rows.add((Map<String, Object>) row);
                }
            }
            definition.sampleRows = rows;
        }
        definition.source = string(entry.get("source"), "IMPORTED");
        return definition;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<String, Object>();
    }

    private String required(Map<String, Object> request, String field) {
        String value = string(request.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String requireField(Map<String, Object> entry, String field) {
        String value = string(entry.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String key(String tenantId, String hospitalCode, String adapterCode, String queryCode) {
        return canonical(tenantId) + "::" + canonical(hospitalCode) + "::" + canonical(adapterCode) + "::" + canonical(queryCode);
    }

    private String canonical(String value) {
        return string(value, "").trim().toUpperCase();
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
