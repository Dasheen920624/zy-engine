package com.medkernel.adapter;

import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * 互联互通标准适配器服务
 * 支持HL7 v2、FHIR、CDA、IHE、CDS Hooks、SMART on FHIR、DICOM等标准
 */
@Service
public class InteropAdapterService {
    private static final Logger logger = LoggerFactory.getLogger(InteropAdapterService.class);
    
    private final EnginePersistenceService persistenceService;
    private final Map<String, InteropAdapterDefinition> adapterDefinitions = new ConcurrentHashMap<>();
    private final Map<String, CdsHooksServiceDefinition> cdsHooksDefinitions = new ConcurrentHashMap<>();
    private final Map<String, SmartAppDefinition> smartAppDefinitions = new ConcurrentHashMap<>();
    
    public InteropAdapterService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        seedInteropDefinitions();
    }
    
    /**
     * 查询互联互通适配器
     */
    public Map<String, Object> query(Map<String, Object> request, String tenantId, String hospitalCode) {
        long start = System.currentTimeMillis();
        String adapterCode = required(request, "adapter_code");
        String queryCode = required(request, "query_code");
        Map<String, Object> params = map(request.get("params"));
        
        InteropAdapterDefinition definition = adapterDefinitions.get(
                interopKey(tenantId, hospitalCode, adapterCode, queryCode));
        
        Map<String, Object> result;
        if (definition == null) {
            result = unsupportedInterop(adapterCode, queryCode, params);
        } else {
            result = successInterop(definition, params, System.currentTimeMillis() - start);
        }
        
        auditInterop(adapterCode, queryCode, params, result);
        return result;
    }
    
    /**
     * 查询CDS Hooks服务
     */
    public Map<String, Object> queryCdsHooks(Map<String, Object> request, String tenantId, String hospitalCode) {
        long start = System.currentTimeMillis();
        String hookId = required(request, "hook_id");
        String hookType = required(request, "hook_type");
        Map<String, Object> context = map(request.get("context"));
        
        CdsHooksServiceDefinition definition = cdsHooksDefinitions.get(
                cdsHooksKey(tenantId, hospitalCode, hookId));
        
        Map<String, Object> result;
        if (definition == null) {
            result = unsupportedCdsHooks(hookId, hookType, context);
        } else {
            result = successCdsHooks(definition, context, System.currentTimeMillis() - start);
        }
        
        auditCdsHooks(hookId, hookType, context, result);
        return result;
    }
    
    /**
     * 查询SMART on FHIR应用
     */
    public Map<String, Object> querySmartApp(Map<String, Object> request, String tenantId, String hospitalCode) {
        long start = System.currentTimeMillis();
        String appId = required(request, "app_id");
        Map<String, Object> launchContext = map(request.get("launch_context"));
        
        SmartAppDefinition definition = smartAppDefinitions.get(
                smartAppKey(tenantId, hospitalCode, appId));
        
        Map<String, Object> result;
        if (definition == null) {
            result = unsupportedSmartApp(appId, launchContext);
        } else {
            result = successSmartApp(definition, launchContext, System.currentTimeMillis() - start);
        }
        
        auditSmartApp(appId, launchContext, result);
        return result;
    }
    
    /**
     * 获取所有支持的互联互通适配器列表
     */
    public List<Map<String, Object>> listInteropAdapters(String tenantId, String hospitalCode) {
        String prefix = canonical(tenantId) + "::" + canonical(hospitalCode) + "::";
        List<InteropAdapterDefinition> list = new ArrayList<>();
        for (Map.Entry<String, InteropAdapterDefinition> entry : adapterDefinitions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                list.add(entry.getValue());
            }
        }
        
        Collections.sort(list, new Comparator<InteropAdapterDefinition>() {
            @Override
            public int compare(InteropAdapterDefinition left, InteropAdapterDefinition right) {
                int byAdapter = left.adapterCode.compareTo(right.adapterCode);
                return byAdapter != 0 ? byAdapter : left.queryCode.compareTo(right.queryCode);
            }
        });
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (InteropAdapterDefinition definition : list) {
            result.add(viewInterop(definition));
        }
        return result;
    }
    
    /**
     * 获取所有CDS Hooks服务列表
     */
    public List<Map<String, Object>> listCdsHooksServices(String tenantId, String hospitalCode) {
        String prefix = canonical(tenantId) + "::" + canonical(hospitalCode) + "::";
        List<CdsHooksServiceDefinition> list = new ArrayList<>();
        for (Map.Entry<String, CdsHooksServiceDefinition> entry : cdsHooksDefinitions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                list.add(entry.getValue());
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdsHooksServiceDefinition definition : list) {
            result.add(viewCdsHooks(definition));
        }
        return result;
    }
    
    /**
     * 获取所有SMART on FHIR应用列表
     */
    public List<Map<String, Object>> listSmartApps(String tenantId, String hospitalCode) {
        String prefix = canonical(tenantId) + "::" + canonical(hospitalCode) + "::";
        List<SmartAppDefinition> list = new ArrayList<>();
        for (Map.Entry<String, SmartAppDefinition> entry : smartAppDefinitions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                list.add(entry.getValue());
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (SmartAppDefinition definition : list) {
            result.add(viewSmartApp(definition));
        }
        return result;
    }
    
    // ==================== 私有方法 ====================
    
    private void seedInteropDefinitions() {
        String tenant = com.medkernel.common.OrgDefaults.DEFAULT_TENANT_ID;
        String hospital = com.medkernel.common.OrgDefaults.DEFAULT_HOSPITAL_CODE;

        // HIS HL7 v2适配器
        registerInterop(tenant, hospital,
                "HIS_HL7_ADAPTER", "HIS HL7 v2适配器", "HL7", "HIS", "MLLP",
                "hl7://his-server:2575", "NONE", 30000, 3,
                "QUERY_PATIENT_ADT", "READ", "查询患者入出院信息",
                "通过HL7 v2 ADT消息获取患者入院、出院、转科信息",
                Arrays.asList("patient_id", "patient_name", "admission_date", "discharge_date", "department", "attending_doctor", "diagnosis"),
                null, null, "ADT^A01", null, null, null);

        // HIS FHIR适配器
        registerInterop(tenant, hospital,
                "HIS_FHIR_ADAPTER", "HIS FHIR适配器", "FHIR", "HIS", "HTTPS",
                "https://fhir.his-server.com/r4", "OAUTH2", 30000, 3,
                "QUERY_PATIENT_RESOURCE", "READ", "查询FHIR Patient资源",
                "通过FHIR RESTful API获取患者信息",
                Arrays.asList("patient_id", "identifier", "name", "gender", "birth_date", "address", "phone"),
                null, null, null, "Patient", null, null);

        // EMR CDA文档适配器
        registerInterop(tenant, hospital,
                "EMR_CDA_ADAPTER", "EMR CDA文档适配器", "CDA", "EMR", "SOAP",
                "soap://emr-server:8080/cda", "BASIC", 60000, 2,
                "QUERY_DISCHARGE_SUMMARY", "READ", "查询出院小结",
                "通过CDA文档交换获取出院小结文档",
                Arrays.asList("document_id", "patient_id", "document_type", "document_title", "author", "creation_time", "document_content"),
                null, null, null, null, null, null);

        // LIS HL7 v2适配器
        registerInterop(tenant, hospital,
                "LIS_HL7_ADAPTER", "LIS HL7 v2适配器", "HL7", "LIS", "MLLP",
                "hl7://lis-server:2575", "NONE", 30000, 3,
                "QUERY_LAB_RESULT", "READ", "查询检验结果",
                "通过HL7 v2 ORU消息获取检验结果",
                Arrays.asList("lab_report_id", "patient_id", "test_code", "test_name", "result_value", "unit", "reference_range", "abnormal_flag", "result_time"),
                null, null, "ORU^R01", null, null, null);

        // LIS FHIR适配器
        registerInterop(tenant, hospital,
                "LIS_FHIR_ADAPTER", "LIS FHIR适配器", "FHIR", "LIS", "HTTPS",
                "https://fhir.lis-server.com/r4", "OAUTH2", 30000, 3,
                "QUERY_DIAGNOSTIC_REPORT", "READ", "查询FHIR DiagnosticReport",
                "通过FHIR API获取检验报告",
                Arrays.asList("report_id", "patient_id", "status", "category", "code", "effective_date", "conclusion"),
                null, null, null, "DiagnosticReport", null, null);

        // PACS DICOM适配器
        registerInterop(tenant, hospital,
                "PACS_DICOM_ADAPTER", "PACS DICOM适配器", "DICOM", "PACS", "DICOM",
                "dicom://pacs-server:11112", "NONE", 60000, 3,
                "QUERY_CT_IMAGE", "READ", "查询CT影像",
                "通过DICOM协议获取CT影像",
                Arrays.asList("study_id", "patient_id", "modality", "study_date", "study_description", "series_count", "instance_count"),
                null, null, null, null, "CTImageStorage", null);

        // PACS IHE XDS-I适配器
        registerInterop(tenant, hospital,
                "PACS_IHE_ADAPTER", "PACS IHE XDS-I适配器", "IHE", "PACS", "HTTPS",
                "https://ihe.pacs-server.com/xds", "CERT", 60000, 2,
                "QUERY_IMAGING_STUDY", "READ", "查询IHE影像研究",
                "通过IHE XDS-I集成规范获取影像研究信息",
                Arrays.asList("study_uid", "patient_id", "accession_number", "modality", "study_date", "referring_physician", "image_url"),
                null, null, null, null, null, null);

        // 医保RESTful适配器
        registerInterop(tenant, hospital,
                "INSURANCE_REST_ADAPTER", "医保RESTful适配器", "REST", "INSURANCE", "HTTPS",
                "https://insurance-api.gov.cn/v1", "CERT", 30000, 3,
                "QUERY_INSURANCE_CATALOG", "READ", "查询医保目录",
                "通过RESTful API获取医保药品和诊疗项目目录",
                Arrays.asList("catalog_type", "item_code", "item_name", "specification", "unit_price", "reimbursement_ratio", "effective_date"),
                null, null, null, null, null, null);

        // OA工作流适配器
        registerInterop(tenant, hospital,
                "OA_WORKFLOW_ADAPTER", "OA工作流适配器", "REST", "OA", "HTTPS",
                "https://oa-server.company.com/api", "APIKEY", 15000, 2,
                "QUERY_APPROVAL_STATUS", "READ", "查询审批状态",
                "通过RESTful API获取OA系统审批流程状态",
                Arrays.asList("workflow_id", "workflow_type", "applicant", "apply_time", "current_step", "status", "approver"),
                null, null, null, null, null, null);

        // CDS Hooks服务
        registerCdsHooks(tenant, hospital,
                "HOOK_CDS_001", "order-sign", "ami-risk-assessment", "AMI风险评估",
                "在医生签署医嘱时提供AMI风险评估提醒",
                "需要患者基本信息和医嘱信息", null, null);

        // SMART on FHIR应用
        registerSmartApp(tenant, hospital,
                "SMART_APP_001", "心电图分析应用", "EHR_LAUNCH",
                "ecg-analysis-app", "https://ecg.example.com/callback",
                "patient/Observation.read patient/Patient.read",
                "https://ecg.example.com/launch");
    }
    
    private void registerInterop(String tenantId, String hospitalCode,
                                String adapterCode, String adapterName, String adapterType, String sourceSystem, String protocol,
                                String queryCode, String queryName, String description,
                                Collection<String> schema, String hl7MessageType, String fhirResourceType,
                                String dicomSopClass, List<Map<String, Object>> sampleRows) {
        registerInterop(tenantId, hospitalCode, adapterCode, adapterName, adapterType, sourceSystem, protocol,
                null, null, null, null, queryCode, null, queryName, description, schema,
                null, null, hl7MessageType, fhirResourceType, dicomSopClass, sampleRows);
    }
    
    private void registerInterop(String tenantId, String hospitalCode,
                                String adapterCode, String adapterName, String adapterType, String sourceSystem, String protocol,
                                String baseUrl, String authType, Integer timeoutMs, Integer retryCount,
                                String queryCode, String queryType, String queryName, String description,
                                Collection<String> schema, String requestTemplate, String responseMapping,
                                String hl7MessageType, String fhirResourceType, String dicomSopClass,
                                List<Map<String, Object>> sampleRows) {
        InteropAdapterDefinition definition = new InteropAdapterDefinition();
        definition.adapterCode = canonical(adapterCode);
        definition.adapterName = adapterName;
        definition.adapterType = canonical(adapterType);
        definition.sourceSystem = canonical(sourceSystem);
        definition.protocol = canonical(protocol);
        definition.baseUrl = baseUrl;
        definition.authType = authType;
        definition.timeoutMs = timeoutMs;
        definition.retryCount = retryCount;
        definition.queryCode = canonical(queryCode);
        definition.queryType = queryType;
        definition.queryName = queryName;
        definition.description = description;
        definition.schema = schema == null ? new ArrayList<>() : new ArrayList<>(schema);
        definition.requestTemplate = requestTemplate;
        definition.responseMapping = responseMapping;
        definition.hl7MessageType = hl7MessageType;
        definition.fhirResourceType = fhirResourceType;
        definition.dicomSopClass = dicomSopClass;
        definition.sampleRows = sampleRows;
        definition.source = "BUILT_IN_INTEROP";
        definition.updatedTime = java.time.LocalDateTime.now().toString();
        
        adapterDefinitions.put(interopKey(tenantId, hospitalCode, definition.adapterCode, definition.queryCode), definition);
    }
    
    private void registerCdsHooks(String tenantId, String hospitalCode,
                                 String hookId, String hookType, String serviceId, String serviceTitle,
                                 String description, String usageRequirements,
                                 Map<String, Object> prefetchData, Map<String, Object> responseTemplate) {
        CdsHooksServiceDefinition definition = new CdsHooksServiceDefinition();
        definition.hookId = hookId;
        definition.hookType = hookType;
        definition.serviceId = serviceId;
        definition.serviceTitle = serviceTitle;
        definition.description = description;
        definition.usageRequirements = usageRequirements;
        definition.prefetchData = prefetchData;
        definition.responseTemplate = responseTemplate;
        definition.source = "BUILT_IN_CDS_HOOKS";
        definition.updatedTime = java.time.LocalDateTime.now().toString();
        
        cdsHooksDefinitions.put(cdsHooksKey(tenantId, hospitalCode, hookId), definition);
    }
    
    private void registerSmartApp(String tenantId, String hospitalCode,
                                 String appId, String appName, String appType,
                                 String clientId, String redirectUri, String scope, String launchUrl) {
        SmartAppDefinition definition = new SmartAppDefinition();
        definition.appId = appId;
        definition.appName = appName;
        definition.appType = appType;
        definition.clientId = clientId;
        definition.redirectUri = redirectUri;
        definition.scope = scope;
        definition.launchUrl = launchUrl;
        definition.source = "BUILT_IN_SMART";
        definition.updatedTime = java.time.LocalDateTime.now().toString();
        
        smartAppDefinitions.put(smartAppKey(tenantId, hospitalCode, appId), definition);
    }
    
    private Map<String, Object> successInterop(InteropAdapterDefinition definition, Map<String, Object> params, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapter_code", definition.adapterCode);
        result.put("query_code", definition.queryCode);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "SUCCESS");
        result.put("adapter_type", definition.adapterType);
        result.put("source_system", definition.sourceSystem);
        result.put("protocol", definition.protocol);
        result.put("query_name", definition.queryName);
        result.put("mock", true);
        result.put("message", "互联互通适配器查询已注册，返回模拟数据。");
        result.put("elapsed_ms", elapsedMs);
        result.put("row_count", 1);
        result.put("rows", Arrays.asList(createSampleRow(definition, params)));
        result.put("schema", definition.schema);
        
        // 添加标准特定字段
        if (definition.hl7MessageType != null) {
            result.put("hl7_message_type", definition.hl7MessageType);
        }
        if (definition.fhirResourceType != null) {
            result.put("fhir_resource_type", definition.fhirResourceType);
        }
        if (definition.dicomSopClass != null) {
            result.put("dicom_sop_class", definition.dicomSopClass);
        }
        
        return result;
    }
    
    private Map<String, Object> unsupportedInterop(String adapterCode, String queryCode, Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("adapter_code", canonical(adapterCode));
        result.put("query_code", canonical(queryCode));
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "UNSUPPORTED");
        result.put("adapter_type", null);
        result.put("source_system", null);
        result.put("protocol", null);
        result.put("query_name", null);
        result.put("mock", true);
        result.put("message", "未配置该互联互通适配器查询。");
        result.put("elapsed_ms", 0);
        result.put("row_count", 0);
        result.put("rows", new ArrayList<>());
        result.put("params", params);
        result.put("supported_queries", supportedInteropQueries());
        return result;
    }
    
    private Map<String, Object> successCdsHooks(CdsHooksServiceDefinition definition, Map<String, Object> context, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hook_id", definition.hookId);
        result.put("hook_type", definition.hookType);
        result.put("service_id", definition.serviceId);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "SUCCESS");
        result.put("service_title", definition.serviceTitle);
        result.put("description", definition.description);
        result.put("mock", true);
        result.put("message", "CDS Hooks服务已注册，返回模拟决策支持卡片。");
        result.put("elapsed_ms", elapsedMs);
        result.put("cards", Arrays.asList(createSampleCdsCard(definition)));
        return result;
    }
    
    private Map<String, Object> unsupportedCdsHooks(String hookId, String hookType, Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hook_id", hookId);
        result.put("hook_type", hookType);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "UNSUPPORTED");
        result.put("service_title", null);
        result.put("description", null);
        result.put("mock", true);
        result.put("message", "未配置该CDS Hooks服务。");
        result.put("elapsed_ms", 0);
        result.put("cards", new ArrayList<>());
        result.put("context", context);
        return result;
    }
    
    private Map<String, Object> successSmartApp(SmartAppDefinition definition, Map<String, Object> launchContext, long elapsedMs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("app_id", definition.appId);
        result.put("app_name", definition.appName);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "SUCCESS");
        result.put("app_type", definition.appType);
        result.put("client_id", definition.clientId);
        result.put("redirect_uri", definition.redirectUri);
        result.put("scope", definition.scope);
        result.put("launch_url", definition.launchUrl);
        result.put("mock", true);
        result.put("message", "SMART on FHIR应用已注册，返回模拟启动信息。");
        result.put("elapsed_ms", elapsedMs);
        return result;
    }
    
    private Map<String, Object> unsupportedSmartApp(String appId, Map<String, Object> launchContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("app_id", appId);
        result.put("trace_id", TraceContext.getTraceId());
        result.put("status", "UNSUPPORTED");
        result.put("app_name", null);
        result.put("app_type", null);
        result.put("mock", true);
        result.put("message", "未配置该SMART on FHIR应用。");
        result.put("elapsed_ms", 0);
        result.put("launch_context", launchContext);
        return result;
    }
    
    private Map<String, Object> createSampleRow(InteropAdapterDefinition definition, Map<String, Object> params) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("patient_id", string(params.get("patient_id"), "P_INTEROP_001"));
        row.put("encounter_id", string(params.get("encounter_id"), "E_INTEROP_001"));
        row.put("tenant_id", string(params.get("tenant_id"), "default"));
        row.put("org_code", string(params.get("org_code"), "DEFAULT_HOSPITAL"));
        
        // 根据适配器类型添加特定字段
        switch (definition.adapterType) {
            case "HL7":
                row.put("message_type", definition.hl7MessageType);
                row.put("message_control_id", "MSG_" + System.currentTimeMillis());
                row.put("processing_id", "P");
                break;
            case "FHIR":
                row.put("resource_type", definition.fhirResourceType);
                row.put("resource_id", "FHIR_" + System.currentTimeMillis());
                row.put("version_id", "1");
                break;
            case "CDA":
                row.put("document_id", "CDA_" + System.currentTimeMillis());
                row.put("document_type", "ClinicalDocument");
                row.put("template_id", "2.16.840.1.113883.10.20.22.1.1");
                break;
            case "DICOM":
                row.put("sop_class_uid", definition.dicomSopClass);
                row.put("sop_instance_uid", "1.2.840.113619.2.55.3.604688119." + System.currentTimeMillis());
                row.put("study_instance_uid", "1.2.840.113619.2.55.3.604688119.969." + System.currentTimeMillis());
                break;
            case "IHE":
                row.put("entry_uuid", "urn:uuid:" + java.util.UUID.randomUUID());
                row.put("repository_unique_id", "1.2.840.113619.2.55.3.604688119");
                break;
            default:
                row.put("request_id", "REQ_" + System.currentTimeMillis());
                break;
        }
        
        return row;
    }
    
    private Map<String, Object> createSampleCdsCard(CdsHooksServiceDefinition definition) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("uuid", "card_" + System.currentTimeMillis());
        card.put("summary", definition.serviceTitle + " - 示例提醒");
        card.put("detail", "这是一个示例CDS Hooks决策支持卡片，用于演示集成效果。");
        card.put("indicator", "warning");
        card.put("source", new LinkedHashMap<String, Object>() {{
            put("label", "MedKernel CDSS");
            put("url", "https://medkernel.example.com/cds");
        }});
        card.put("suggestions", Arrays.asList(
                new LinkedHashMap<String, Object>() {{
                    put("uuid", "suggestion_1");
                    put("label", "查看详情");
                    put("actions", Arrays.asList(
                            new LinkedHashMap<String, Object>() {{
                                put("type", "create");
                                put("description", "查看患者详细信息");
                            }}
                    ));
                }}
        ));
        return card;
    }
    
    private Map<String, Object> viewInterop(InteropAdapterDefinition definition) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("adapter_code", definition.adapterCode);
        view.put("adapter_name", definition.adapterName);
        view.put("adapter_type", definition.adapterType);
        view.put("source_system", definition.sourceSystem);
        view.put("protocol", definition.protocol);
        view.put("query_code", definition.queryCode);
        view.put("query_name", definition.queryName);
        view.put("description", definition.description);
        view.put("schema", definition.schema);
        view.put("source", definition.source);
        view.put("has_sample_rows", definition.sampleRows != null && !definition.sampleRows.isEmpty());
        
        if (definition.hl7MessageType != null) {
            view.put("hl7_message_type", definition.hl7MessageType);
        }
        if (definition.fhirResourceType != null) {
            view.put("fhir_resource_type", definition.fhirResourceType);
        }
        if (definition.dicomSopClass != null) {
            view.put("dicom_sop_class", definition.dicomSopClass);
        }
        
        return view;
    }
    
    private Map<String, Object> viewCdsHooks(CdsHooksServiceDefinition definition) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("hook_id", definition.hookId);
        view.put("hook_type", definition.hookType);
        view.put("service_id", definition.serviceId);
        view.put("service_title", definition.serviceTitle);
        view.put("description", definition.description);
        view.put("usage_requirements", definition.usageRequirements);
        view.put("source", definition.source);
        return view;
    }
    
    private Map<String, Object> viewSmartApp(SmartAppDefinition definition) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("app_id", definition.appId);
        view.put("app_name", definition.appName);
        view.put("app_type", definition.appType);
        view.put("client_id", definition.clientId);
        view.put("redirect_uri", definition.redirectUri);
        view.put("scope", definition.scope);
        view.put("launch_url", definition.launchUrl);
        view.put("source", definition.source);
        return view;
    }
    
    private List<Map<String, Object>> supportedInteropQueries() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (InteropAdapterDefinition definition : adapterDefinitions.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("adapter_code", definition.adapterCode);
            item.put("adapter_name", definition.adapterName);
            item.put("adapter_type", definition.adapterType);
            item.put("source_system", definition.sourceSystem);
            item.put("protocol", definition.protocol);
            item.put("query_code", definition.queryCode);
            item.put("query_name", definition.queryName);
            list.add(item);
        }
        return list;
    }
    
    private void auditInterop(String adapterCode, String queryCode, Map<String, Object> params, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("adapter_code", canonical(adapterCode));
        detail.put("query_code", canonical(queryCode));
        detail.put("status", result.get("status"));
        detail.put("mock", result.get("mock"));
        detail.put("row_count", result.get("row_count"));
        detail.put("elapsed_ms", result.get("elapsed_ms"));
        detail.put("adapter_type", result.get("adapter_type"));
        detail.put("protocol", result.get("protocol"));
        
        try {
            persistenceService.saveAuditLog("INTEROP_ADAPTER", "QUERY", "INTEROP_ADAPTER_QUERY",
                    canonical(adapterCode) + "." + canonical(queryCode),
                    string(params.get("patient_id"), null),
                    string(params.get("encounter_id"), null),
                    string(params.get("operator_id"), null),
                    detail);
        } catch (RuntimeException ex) {
            logger.warn("[traceId={}] interop adapter audit log persistence failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }
    
    private void auditCdsHooks(String hookId, String hookType, Map<String, Object> context, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("hook_id", hookId);
        detail.put("hook_type", hookType);
        detail.put("status", result.get("status"));
        detail.put("mock", result.get("mock"));
        detail.put("cards_count", result.get("cards") != null ? ((List<?>) result.get("cards")).size() : 0);
        
        try {
            persistenceService.saveAuditLog("CDS_HOOKS", "QUERY", "CDS_HOOKS_QUERY",
                    hookId,
                    string(context.get("patient_id"), null),
                    string(context.get("encounter_id"), null),
                    string(context.get("user_id"), null),
                    detail);
        } catch (RuntimeException ex) {
            logger.warn("[traceId={}] CDS Hooks audit log persistence failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }
    
    private void auditSmartApp(String appId, Map<String, Object> launchContext, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("app_id", appId);
        detail.put("status", result.get("status"));
        detail.put("mock", result.get("mock"));
        detail.put("app_type", result.get("app_type"));
        
        try {
            persistenceService.saveAuditLog("SMART_ON_FHIR", "LAUNCH", "SMART_APP_LAUNCH",
                    appId,
                    string(launchContext.get("patient_id"), null),
                    string(launchContext.get("encounter_id"), null),
                    string(launchContext.get("user_id"), null),
                    detail);
        } catch (RuntimeException ex) {
            logger.warn("[traceId={}] SMART on FHIR audit log persistence failed: {}",
                    TraceContext.getTraceId(), ex.getMessage());
        }
    }
    
    private String interopKey(String tenantId, String hospitalCode, String adapterCode, String queryCode) {
        return canonical(tenantId) + "::" + canonical(hospitalCode) + "::" + canonical(adapterCode) + "::" + canonical(queryCode);
    }
    
    private String cdsHooksKey(String tenantId, String hospitalCode, String hookId) {
        return canonical(tenantId) + "::" + canonical(hospitalCode) + "::" + canonical(hookId);
    }
    
    private String smartAppKey(String tenantId, String hospitalCode, String appId) {
        return canonical(tenantId) + "::" + canonical(hospitalCode) + "::" + canonical(appId);
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
    
    private String required(Map<String, Object> request, String field) {
        String value = string(request.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }
    
    // ==================== 内部类 ====================
    
    private static class InteropAdapterDefinition {
        private String adapterCode;
        private String adapterName;
        private String adapterType;
        private String sourceSystem;
        private String protocol;
        private String baseUrl;
        private String authType;
        private Integer timeoutMs;
        private Integer retryCount;
        private String queryCode;
        private String queryName;
        private String queryType;
        private String description;
        private List<String> schema;
        private String requestTemplate;
        private String responseMapping;
        private String hl7MessageType;
        private String fhirResourceType;
        private String dicomSopClass;
        private List<Map<String, Object>> sampleRows;
        private String source;
        private String updatedTime;
    }
    
    private static class CdsHooksServiceDefinition {
        private String hookId;
        private String hookType;
        private String serviceId;
        private String serviceTitle;
        private String description;
        private String usageRequirements;
        private Map<String, Object> prefetchData;
        private Map<String, Object> responseTemplate;
        private String source;
        private String updatedTime;
    }
    
    private static class SmartAppDefinition {
        private String appId;
        private String appName;
        private String appType;
        private String clientId;
        private String redirectUri;
        private String scope;
        private String launchUrl;
        private String source;
        private String updatedTime;
    }
}
