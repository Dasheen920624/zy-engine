package com.medkernel.adapter;

import com.medkernel.common.OrgDefaults;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class AdapterDefinitionSeeder {

    public void seedDefinitions(Map<String, AdapterQueryDefinition> queryDefinitions) {
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "ECG_ADAPTER", "ECG适配器", "REST", "ECG", "QUERY_ECG_REPORT",
                "查询心电图报告", "返回AMI样例患者十二导联心电图报告。",
                java.util.Arrays.asList("patient_id", "encounter_id", "exam_code", "finding_codes", "report_time"),
                null);
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "LIS_ADAPTER", "LIS检验适配器", "SQL", "LIS", "QUERY_TROPONIN",
                "查询肌钙蛋白结果", "返回AMI样例患者肌钙蛋白结果。",
                java.util.Arrays.asList("patient_id", "encounter_id", "source_lab_code", "value", "unit", "report_time"),
                null);
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_ADAPTER", "HIS诊断适配器", "REST", "HIS", "QUERY_DIAGNOSES",
                "查询诊断", "返回AMI样例患者HIS诊断。",
                java.util.Arrays.asList("patient_id", "encounter_id", "source_diagnosis_code", "standard_code"),
                null);
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_ADAPTER", "EMR病历适配器", "REST", "EMR", "QUERY_CHIEF_COMPLAINTS",
                "查询主诉", "返回AMI样例患者主诉。",
                java.util.Arrays.asList("patient_id", "encounter_id", "source_symptom_code", "text"),
                null);
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_WS_ADAPTER", "EMR WebService适配器", "WEBSERVICE", "EMR", "QUERY_ADMISSION_NOTE",
                "查询入院记录", "模拟老系统SOAP接口返回病历文书。",
                java.util.Arrays.asList("patient_id", "encounter_id", "document_id", "document_text"),
                null);

        // SEC-006: 用户同步适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_ADAPTER", "HIS用户适配器", "REST", "HIS", "QUERY_HIS_USERS",
                "查询HIS用户列表", "返回HIS系统用户数据，用于院内用户同步。",
                java.util.Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status", "hire_date"),
                null);

        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_ADAPTER", "EMR用户适配器", "REST", "EMR", "QUERY_EMR_USERS",
                "查询EMR用户列表", "返回EMR系统用户数据，用于院内用户同步。",
                java.util.Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status"),
                null);

        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "OA_ADAPTER", "OA用户适配器", "REST", "OA", "QUERY_OA_USERS",
                "查询OA用户列表", "返回OA系统用户数据，用于院内用户同步。",
                java.util.Arrays.asList("user_id", "user_name", "display_name", "department_code",
                        "department_name", "position", "phone", "email", "status"),
                null);

        // INTEROP-001: 院内互联互通标准适配器
        // HIS FHIR适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_FHIR_ADAPTER", "HIS FHIR适配器", "REST", "HIS", "QUERY_FHIR_PATIENT",
                "查询FHIR患者信息", "通过FHIR Patient资源查询患者基本信息。",
                java.util.Arrays.asList("patient_id", "name", "gender", "birth_date", "identifier"),
                null);

        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "HIS_FHIR_ADAPTER", "HIS FHIR适配器", "REST", "HIS", "QUERY_FHIR_ENCOUNTER",
                "查询FHIR就诊信息", "通过FHIR Encounter资源查询患者就诊记录。",
                java.util.Arrays.asList("encounter_id", "patient_id", "status", "class_code", "period_start", "period_end"),
                null);

        // EMR CDA适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "EMR_CDA_ADAPTER", "EMR CDA文档适配器", "REST", "EMR", "QUERY_CDA_DOCUMENT",
                "查询CDA临床文档", "查询CDA格式的临床文档（入院记录、病程记录等）。",
                java.util.Arrays.asList("document_id", "patient_id", "document_type", "template_id", "content", "created_time"),
                null);

        // LIS HL7 v2适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "LIS_HL7V2_ADAPTER", "LIS HL7 v2适配器", "MLLP", "LIS", "QUERY_HL7_ORU",
                "查询HL7 ORU检验结果", "解析HL7 v2 ORU消息获取检验结果。",
                java.util.Arrays.asList("message_id", "patient_id", "order_code", "result_value", "unit", "reference_range", "abnormal_flag", "result_time"),
                null);

        // PACS DICOM适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "PACS_DICOM_ADAPTER", "PACS DICOM适配器", "DICOM", "PACS", "QUERY_DICOM_STUDY",
                "查询DICOM影像检查", "通过DICOM协议查询影像检查列表。",
                java.util.Arrays.asList("study_instance_uid", "patient_id", "modality", "study_date", "study_description", "series_count", "instance_count"),
                null);

        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "PACS_FHIR_ADAPTER", "PACS FHIR适配器", "REST", "PACS", "QUERY_FHIR_IMAGING_STUDY",
                "查询FHIR影像检查", "通过FHIR ImagingStudy资源查询影像检查。",
                java.util.Arrays.asList("imaging_study_id", "patient_id", "status", "modality", "started", "description"),
                null);

        // CDS Hooks适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "CDS_HOOKS_ADAPTER", "CDS Hooks适配器", "REST", "CDS", "TRIGGER_CDS_HOOK",
                "触发CDS Hook", "触发临床决策支持Hook获取卡片建议。",
                java.util.Arrays.asList("hook_id", "hook_type", "patient_id", "encounter_id", "cards", "suggestions"),
                null);

        // SMART on FHIR适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "SMART_ON_FHIR_ADAPTER", "SMART on FHIR适配器", "REST", "SMART", "SMART_LAUNCH",
                "SMART应用启动", "处理SMART on FHIR应用授权和上下文传递。",
                java.util.Arrays.asList("launch_id", "patient_id", "encounter_id", "user_id", "access_token", "token_type", "expires_in"),
                null);

        // IHE XDS适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "IHE_XDS_ADAPTER", "IHE XDS适配器", "SOAP", "IHE", "QUERY_XDS_DOCUMENTS",
                "查询XDS文档", "通过IHE XDS.b协议查询跨企业文档。",
                java.util.Arrays.asList("document_id", "patient_id", "document_type", "repository_unique_id", "hash", "size"),
                null);

        // 医保HL7 v2适配器
        register(queryDefinitions, OrgDefaults.DEFAULT_TENANT_ID, OrgDefaults.DEFAULT_HOSPITAL_CODE,
                "INSURANCE_HL7V2_ADAPTER", "医保HL7 v2适配器", "REST", "INSURANCE", "QUERY_INSURANCE_CLAIM",
                "查询医保结算", "查询医保结算记录和费用明细。",
                java.util.Arrays.asList("claim_id", "patient_id", "claim_type", "total_amount", "insurance_amount", "self_pay_amount", "settlement_time"),
                null);
    }

    private void register(Map<String, AdapterQueryDefinition> queryDefinitions,
                          String tenantId, String hospitalCode, String adapterCode, String adapterName, String adapterType, String sourceSystem,
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
