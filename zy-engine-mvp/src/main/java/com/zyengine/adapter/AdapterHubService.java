package com.zyengine.adapter;

import com.zyengine.common.TraceContext;
import com.zyengine.persistence.EnginePersistenceService;
import com.zyengine.terminology.TerminologyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdapterHubService {
    private final TerminologyService terminologyService;
    private final EnginePersistenceService persistenceService;
    private final Map<String, AdapterQueryDefinition> queryDefinitions =
            new ConcurrentHashMap<String, AdapterQueryDefinition>();

    public AdapterHubService(TerminologyService terminologyService, EnginePersistenceService persistenceService) {
        this.terminologyService = terminologyService;
        this.persistenceService = persistenceService;
        seedDefinitions();
    }

    public Map<String, Object> query(Map<String, Object> request) {
        long start = System.currentTimeMillis();
        String adapterCode = required(request, "adapter_code");
        String queryCode = required(request, "query_code");
        Map<String, Object> params = map(request.get("params"));
        AdapterQueryDefinition definition = queryDefinitions.get(key(adapterCode, queryCode));
        Map<String, Object> result = definition == null
                ? unsupported(adapterCode, queryCode, params)
                : success(definition, params, System.currentTimeMillis() - start);
        audit(adapterCode, queryCode, params, result);
        return result;
    }

    private Map<String, Object> success(AdapterQueryDefinition definition, Map<String, Object> params, long elapsedMs) {
        List<Map<String, Object>> rows = rows(definition, params);
        Map<String, Object> result = base(definition.adapterCode, definition.queryCode);
        result.put("status", "SUCCESS");
        result.put("adapter_type", definition.adapterType);
        result.put("source_system", definition.sourceSystem);
        result.put("query_name", definition.queryName);
        result.put("mock", true);
        result.put("message", definition.description);
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
        String adapterCode = definition.adapterCode;
        String queryCode = definition.queryCode;
        if ("ECG_ADAPTER".equals(adapterCode) && "QUERY_ECG_REPORT".equals(queryCode)) {
            return single(ecgReport(params));
        }
        if ("LIS_ADAPTER".equals(adapterCode) && "QUERY_TROPONIN".equals(queryCode)) {
            return single(troponinResult(params));
        }
        if ("HIS_ADAPTER".equals(adapterCode) && "QUERY_DIAGNOSES".equals(queryCode)) {
            return single(diagnosis(params));
        }
        if ("EMR_ADAPTER".equals(adapterCode) && "QUERY_CHIEF_COMPLAINTS".equals(queryCode)) {
            return single(chiefComplaint(params));
        }
        if ("EMR_WS_ADAPTER".equals(adapterCode) && "QUERY_ADMISSION_NOTE".equals(queryCode)) {
            return single(admissionNote(params));
        }
        return new ArrayList<Map<String, Object>>();
    }

    private Map<String, Object> ecgReport(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("report_id", "ECG_202605150001");
        row.put("exam_code", "ECG_12_LEAD");
        row.put("exam_name", "十二导联心电图");
        row.put("source_finding_code", "ST_ELEVATION");
        row.put("source_finding_name", "ST段抬高");
        row.put("finding_codes", Arrays.asList("ST_ELEVATION_CONTIGUOUS_LEADS"));
        row.put("report_text", "窦性心律，相邻导联ST段抬高，建议结合临床。");
        row.put("report_time", "2026-05-15T10:09:00+08:00");
        putNormalized(row, "ECG", "ST_ELEVATION", "ST段抬高", "FINDING", "finding");
        return row;
    }

    private Map<String, Object> troponinResult(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("lab_report_id", "LIS_202605150001");
        row.put("source_lab_code", "TNI");
        row.put("source_lab_name", "肌钙蛋白I");
        row.put("value", 1.62);
        row.put("unit", "ng/mL");
        row.put("reference_range", "0-0.04");
        row.put("abnormal_flag", "HIGH");
        row.put("report_time", "2026-05-15T10:31:00+08:00");
        putNormalized(row, "LIS", "TNI", "肌钙蛋白I", "LAB_ITEM", "lab");
        return row;
    }

    private Map<String, Object> diagnosis(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("diagnosis_id", "DIA_202605150001");
        row.put("source_diagnosis_code", "I21.3");
        row.put("source_diagnosis_name", "急性ST段抬高型心肌梗死");
        row.put("diagnosis_time", "2026-05-15T10:42:00+08:00");
        row.put("diagnosis_type", "ADMISSION");
        putNormalized(row, "HIS", "I21.3", "急性ST段抬高型心肌梗死", "DIAGNOSIS", "diagnosis");
        row.put("standard_code", row.get("diagnosis_standard_code"));
        row.put("standard_name", row.get("diagnosis_standard_name"));
        return row;
    }

    private Map<String, Object> chiefComplaint(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("complaint_id", "CC_202605150001");
        row.put("source_symptom_code", "CHEST_PAIN");
        row.put("source_symptom_name", "胸痛");
        row.put("text", "胸痛2小时，伴大汗。");
        row.put("record_time", "2026-05-15T10:04:00+08:00");
        putNormalized(row, "EMR", "CHEST_PAIN", "胸痛", "SYMPTOM", "symptom");
        return row;
    }

    private Map<String, Object> admissionNote(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("document_id", "DOC_202605150001");
        row.put("document_type", "ADMISSION_NOTE");
        row.put("document_title", "急诊入院记录");
        row.put("document_text", "患者因胸痛2小时入院，心电图提示相邻导联ST段抬高。");
        row.put("record_time", "2026-05-15T10:20:00+08:00");
        return row;
    }

    private Map<String, Object> patientRow(Map<String, Object> params) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("patient_id", string(params.get("patient_id"), "P_AMI_001"));
        row.put("encounter_id", string(params.get("encounter_id"), "E_AMI_001"));
        row.put("tenant_id", string(params.get("tenant_id"), "default"));
        row.put("org_code", string(params.get("org_code"), "ZYHOSPITAL"));
        return row;
    }

    private void putNormalized(Map<String, Object> row, String sourceSystem, String sourceCode,
                               String sourceName, String conceptType, String prefix) {
        Map<String, Object> normalized = terminologyService.normalizeCode(sourceSystem, sourceCode, sourceName, conceptType);
        row.put(prefix + "_standard_code", normalized.get("standard_code"));
        row.put(prefix + "_standard_name", normalized.get("standard_name"));
        row.put(prefix + "_mapping_status", normalized.get("mapping_status"));
        row.put(prefix + "_mapping_confidence", normalized.get("confidence"));
    }

    private void seedDefinitions() {
        register("ECG_ADAPTER", "ECG适配器", "REST", "ECG", "QUERY_ECG_REPORT",
                "查询心电图报告", "返回AMI样例患者十二导联心电图报告。",
                Arrays.asList("patient_id", "encounter_id", "exam_code", "finding_codes", "report_time"));
        register("LIS_ADAPTER", "LIS检验适配器", "SQL", "LIS", "QUERY_TROPONIN",
                "查询肌钙蛋白结果", "返回AMI样例患者肌钙蛋白结果。",
                Arrays.asList("patient_id", "encounter_id", "source_lab_code", "value", "unit", "report_time"));
        register("HIS_ADAPTER", "HIS诊断适配器", "REST", "HIS", "QUERY_DIAGNOSES",
                "查询诊断", "返回AMI样例患者HIS诊断。",
                Arrays.asList("patient_id", "encounter_id", "source_diagnosis_code", "standard_code"));
        register("EMR_ADAPTER", "EMR病历适配器", "REST", "EMR", "QUERY_CHIEF_COMPLAINTS",
                "查询主诉", "返回AMI样例患者主诉。",
                Arrays.asList("patient_id", "encounter_id", "source_symptom_code", "text"));
        register("EMR_WS_ADAPTER", "EMR WebService适配器", "WEBSERVICE", "EMR", "QUERY_ADMISSION_NOTE",
                "查询入院记录", "模拟老系统SOAP接口返回病历文书。",
                Arrays.asList("patient_id", "encounter_id", "document_id", "document_text"));
    }

    private void register(String adapterCode, String adapterName, String adapterType, String sourceSystem,
                          String queryCode, String queryName, String description, Collection<String> schema) {
        AdapterQueryDefinition definition = new AdapterQueryDefinition();
        definition.adapterCode = canonical(adapterCode);
        definition.adapterName = adapterName;
        definition.adapterType = canonical(adapterType);
        definition.sourceSystem = canonical(sourceSystem);
        definition.queryCode = canonical(queryCode);
        definition.queryName = queryName;
        definition.description = description;
        definition.schema = new ArrayList<String>(schema);
        queryDefinitions.put(key(definition.adapterCode, definition.queryCode), definition);
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

    private List<Map<String, Object>> single(Map<String, Object> row) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.add(row);
        return rows;
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
        } catch (RuntimeException ignored) {
            // 适配器Mock查询不能因为审计失败影响演示链路。
        }
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

    private String key(String adapterCode, String queryCode) {
        return canonical(adapterCode) + "::" + canonical(queryCode);
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

    private static class AdapterQueryDefinition {
        private String adapterCode;
        private String adapterName;
        private String adapterType;
        private String sourceSystem;
        private String queryCode;
        private String queryName;
        private String description;
        private List<String> schema;
    }
}
