package com.medkernel.adapter;

import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.terminology.TerminologyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

    public List<Map<String, Object>> importDefinitions(Object request, String tenantId, String hospitalCode) {
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
        // SEC-006: 用户同步适配器Mock数据
        if ("HIS_ADAPTER".equals(adapterCode) && "QUERY_HIS_USERS".equals(queryCode)) {
            return hisUsers(params);
        }
        if ("EMR_ADAPTER".equals(adapterCode) && "QUERY_EMR_USERS".equals(queryCode)) {
            return emrUsers(params);
        }
        if ("OA_ADAPTER".equals(adapterCode) && "QUERY_OA_USERS".equals(queryCode)) {
            return oaUsers(params);
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

    // SEC-006: 用户同步适配器Mock数据生成方法
    private List<Map<String, Object>> hisUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // 用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "HIS_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        user1.put("hire_date", "2015-06-01");
        users.add(user1);
        // 用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "HIS_USER_002");
        user2.put("user_name", "lisi");
        user2.put("display_name", "李四");
        user2.put("department_code", "DEPT_EMERGENCY");
        user2.put("department_name", "急诊科");
        user2.put("position", "副主任医师");
        user2.put("phone", "13800138002");
        user2.put("email", "lisi@hospital.com");
        user2.put("status", "ACTIVE");
        user2.put("hire_date", "2018-03-15");
        users.add(user2);
        // 用户3
        Map<String, Object> user3 = new LinkedHashMap<String, Object>();
        user3.put("user_id", "HIS_USER_003");
        user3.put("user_name", "wangwu");
        user3.put("display_name", "王五");
        user3.put("department_code", "DEPT_RADIOLOGY");
        user3.put("department_name", "放射科");
        user3.put("position", "主治医师");
        user3.put("phone", "13800138003");
        user3.put("email", "wangwu@hospital.com");
        user3.put("status", "INACTIVE");
        user3.put("hire_date", "2020-09-01");
        users.add(user3);
        return users;
    }

    private List<Map<String, Object>> emrUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // EMR用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "EMR_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        users.add(user1);
        // EMR用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "EMR_USER_004");
        user2.put("user_name", "zhaoliu");
        user2.put("display_name", "赵六");
        user2.put("department_code", "DEPT_NURSING");
        user2.put("department_name", "护理部");
        user2.put("position", "护士长");
        user2.put("phone", "13800138004");
        user2.put("email", "zhaoliu@hospital.com");
        user2.put("status", "ACTIVE");
        users.add(user2);
        return users;
    }

    private List<Map<String, Object>> oaUsers(Map<String, Object> params) {
        List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
        // OA用户1
        Map<String, Object> user1 = new LinkedHashMap<String, Object>();
        user1.put("user_id", "OA_USER_001");
        user1.put("user_name", "zhangsan");
        user1.put("display_name", "张三");
        user1.put("department_code", "DEPT_CARDIO");
        user1.put("department_name", "心血管内科");
        user1.put("position", "主任医师");
        user1.put("phone", "13800138001");
        user1.put("email", "zhangsan@hospital.com");
        user1.put("status", "ACTIVE");
        users.add(user1);
        // OA用户2
        Map<String, Object> user2 = new LinkedHashMap<String, Object>();
        user2.put("user_id", "OA_USER_005");
        user2.put("user_name", "sunqi");
        user2.put("display_name", "孙七");
        user2.put("department_code", "DEPT_ADMIN");
        user2.put("department_name", "院办");
        user2.put("position", "行政人员");
        user2.put("phone", "13800138005");
        user2.put("email", "sunqi@hospital.com");
        user2.put("status", "ACTIVE");
        users.add(user2);
        // OA用户3
        Map<String, Object> user3 = new LinkedHashMap<String, Object>();
        user3.put("user_id", "OA_USER_006");
        user3.put("user_name", "zhouba");
        user3.put("display_name", "周八");
        user3.put("department_code", "DEPT_FINANCE");
        user3.put("department_name", "财务科");
        user3.put("position", "会计");
        user3.put("phone", "13800138006");
        user3.put("email", "zhouba@hospital.com");
        user3.put("status", "INACTIVE");
        users.add(user3);
        return users;
    }

    private Map<String, Object> patientRow(Map<String, Object> params) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("patient_id", string(params.get("patient_id"), "P_AMI_001"));
        row.put("encounter_id", string(params.get("encounter_id"), "E_AMI_001"));
        // 统一引用 OrgDefaults 集中常量，避免字面量散落在多个 Service。
        row.put("tenant_id", string(params.get("tenant_id"), com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID));
        row.put("org_code", string(params.get("org_code"), com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE));
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

    private List<Map<String, Object>> rowsWithPatientContext(List<Map<String, Object>> templateRows,
                                                              Map<String, Object> params) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> template : templateRows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>(patientRow(params));
            row.putAll(template);
            rows.add(row);
        }
        return rows;
    }

    private void seedDefinitions() {
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "ECG_ADAPTER", "ECG适配器", "REST", "ECG", "QUERY_ECG_REPORT",
                "查询心电图报告", "返回AMI样例患者十二导联心电图报告。",
                Arrays.asList("patient_id", "encounter_id", "exam_code", "finding_codes", "report_time"),
                null);
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "LIS_ADAPTER", "LIS检验适配器", "SQL", "LIS", "QUERY_TROPONIN",
                "查询肌钙蛋白结果", "返回AMI样例患者肌钙蛋白结果。",
                Arrays.asList("patient_id", "encounter_id", "source_lab_code", "value", "unit", "report_time"),
                null);
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_ADAPTER", "HIS诊断适配器", "REST", "HIS", "QUERY_DIAGNOSES",
                "查询诊断", "返回AMI样例患者HIS诊断。",
                Arrays.asList("patient_id", "encounter_id", "source_diagnosis_code", "standard_code"),
                null);
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_ADAPTER", "EMR病历适配器", "REST", "EMR", "QUERY_CHIEF_COMPLAINTS",
                "查询主诉", "返回AMI样例患者主诉。",
                Arrays.asList("patient_id", "encounter_id", "source_symptom_code", "text"),
                null);
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_WS_ADAPTER", "EMR WebService适配器", "WEBSERVICE", "EMR", "QUERY_ADMISSION_NOTE",
                "查询入院记录", "模拟老系统SOAP接口返回病历文书。",
                Arrays.asList("patient_id", "encounter_id", "document_id", "document_text"),
                null);

        // SEC-006: 用户同步适配器
        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_ADAPTER", "HIS用户适配器", "REST", "HIS", "QUERY_HIS_USERS",
                "查询HIS用户列表", "返回HIS系统用户数据，用于院内用户同步。",
                Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status", "hire_date"),
                null);

        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_ADAPTER", "EMR用户适配器", "REST", "EMR", "QUERY_EMR_USERS",
                "查询EMR用户列表", "返回EMR系统用户数据，用于院内用户同步。",
                Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status"),
                null);

        register(com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID, com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "OA_ADAPTER", "OA用户适配器", "REST", "OA", "QUERY_OA_USERS",
                "查询OA用户列表", "返回OA系统用户数据，用于院内用户同步。",
                Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status"),
                null);
    }

    private void register(String tenantId, String hospitalCode, String adapterCode, String adapterName, String adapterType, String sourceSystem,
                          String queryCode, String queryName, String description,
                          Collection<String> schema, List<Map<String, Object>> sampleRows) {
        AdapterQueryDefinition definition = new AdapterQueryDefinition();
        definition.adapterCode = canonical(adapterCode);
        definition.adapterName = adapterName;
        definition.adapterType = canonical(adapterType);
        definition.sourceSystem = canonical(sourceSystem);
        definition.queryCode = canonical(queryCode);
        definition.queryName = queryName;
        definition.description = description;
        definition.schema = schema == null ? new ArrayList<String>() : new ArrayList<String>(schema);
        definition.sampleRows = sampleRows;
        definition.source = "BUILT_IN_SAMPLE";
        queryDefinitions.put(key(tenantId, hospitalCode, definition.adapterCode, definition.queryCode), definition);
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
        } catch (RuntimeException ex) {
            // 适配器Mock查询不能因为审计失败影响演示链路；但失败必须可见，便于运维诊断审计基础设施问题。
            org.slf4j.LoggerFactory.getLogger(AdapterHubService.class)
                    .warn("[traceId={}] adapter audit log persistence failed: {}",
                            com.medkernel.common.TraceContext.getTraceId(), ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeDefinitions(Object request) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if (request instanceof List) {
            for (Object item : (List<?>) request) {
                if (item instanceof Map) {
                    list.add((Map<String, Object>) item);
                }
            }
            return list;
        }
        if (request instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) request;
            Object nested = map.get("definitions");
            if (nested instanceof List) {
                return normalizeDefinitions(nested);
            }
            Object alternative = map.get("queries");
            if (alternative instanceof List) {
                return normalizeDefinitions(alternative);
            }
            if (map.containsKey("adapter_code") || map.containsKey("query_code")) {
                list.add(map);
            }
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

    private static class AdapterQueryDefinition {
        private String adapterCode;
        private String adapterName;
        private String adapterType;
        private String sourceSystem;
        private String queryCode;
        private String queryName;
        private String description;
        private List<String> schema;
        private List<Map<String, Object>> sampleRows;
        private String source;
    }
}
