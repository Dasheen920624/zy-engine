package com.medkernel.adapter;

import com.medkernel.terminology.TerminologyService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InteropAdapterMockDataProvider extends AbstractAdapterMockDataProvider {

    public InteropAdapterMockDataProvider(TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public boolean supports(String adapterCode, String queryCode) {
        if ("HIS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_PATIENT".equals(queryCode)) return true;
        if ("HIS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_ENCOUNTER".equals(queryCode)) return true;
        if ("EMR_CDA_ADAPTER".equals(adapterCode) && "QUERY_CDA_DOCUMENT".equals(queryCode)) return true;
        if ("LIS_HL7V2_ADAPTER".equals(adapterCode) && "QUERY_HL7_ORU".equals(queryCode)) return true;
        if ("PACS_DICOM_ADAPTER".equals(adapterCode) && "QUERY_DICOM_STUDY".equals(queryCode)) return true;
        if ("PACS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_IMAGING_STUDY".equals(queryCode)) return true;
        if ("CDS_HOOKS_ADAPTER".equals(adapterCode) && "TRIGGER_CDS_HOOK".equals(queryCode)) return true;
        if ("SMART_ON_FHIR_ADAPTER".equals(adapterCode) && "SMART_LAUNCH".equals(queryCode)) return true;
        if ("IHE_XDS_ADAPTER".equals(adapterCode) && "QUERY_XDS_DOCUMENTS".equals(queryCode)) return true;
        if ("INSURANCE_HL7V2_ADAPTER".equals(adapterCode) && "QUERY_INSURANCE_CLAIM".equals(queryCode)) return true;
        return false;
    }

    @Override
    public List<Map<String, Object>> provideRows(String adapterCode, String queryCode, Map<String, Object> params) {
        if ("HIS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_PATIENT".equals(queryCode)) {
            return single(fhirPatient(params));
        }
        if ("HIS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_ENCOUNTER".equals(queryCode)) {
            return single(fhirEncounter(params));
        }
        if ("EMR_CDA_ADAPTER".equals(adapterCode) && "QUERY_CDA_DOCUMENT".equals(queryCode)) {
            return single(cdaDocument(params));
        }
        if ("LIS_HL7V2_ADAPTER".equals(adapterCode) && "QUERY_HL7_ORU".equals(queryCode)) {
            return single(hl7OruResult(params));
        }
        if ("PACS_DICOM_ADAPTER".equals(adapterCode) && "QUERY_DICOM_STUDY".equals(queryCode)) {
            return single(dicomStudy(params));
        }
        if ("PACS_FHIR_ADAPTER".equals(adapterCode) && "QUERY_FHIR_IMAGING_STUDY".equals(queryCode)) {
            return single(fhirImagingStudy(params));
        }
        if ("CDS_HOOKS_ADAPTER".equals(adapterCode) && "TRIGGER_CDS_HOOK".equals(queryCode)) {
            return single(cdsHookResult(params));
        }
        if ("SMART_ON_FHIR_ADAPTER".equals(adapterCode) && "SMART_LAUNCH".equals(queryCode)) {
            return single(smartLaunch(params));
        }
        if ("IHE_XDS_ADAPTER".equals(adapterCode) && "QUERY_XDS_DOCUMENTS".equals(queryCode)) {
            return single(iheXdsDocument(params));
        }
        if ("INSURANCE_HL7V2_ADAPTER".equals(adapterCode) && "QUERY_INSURANCE_CLAIM".equals(queryCode)) {
            return single(insuranceClaim(params));
        }
        throw new UnsupportedOperationException("unsupported: " + adapterCode + "/" + queryCode);
    }

    // INTEROP-001: FHIR Patient Mock数据
    private Map<String, Object> fhirPatient(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("resource_type", "Patient");
        row.put("name", "张三");
        row.put("gender", "male");
        row.put("birth_date", "1980-01-15");
        row.put("identifier", Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("system", "http://hospital.local/patient-id"); put("value", "P_AMI_001"); }},
                new LinkedHashMap<String, Object>() {{ put("system", "http://hospital.local/id-card"); put("value", "110101198001150001"); }}
        ));
        row.put("telecom", Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("system", "phone"); put("value", "13800138001"); }},
                new LinkedHashMap<String, Object>() {{ put("system", "email"); put("value", "zhangsan@hospital.com"); }}
        ));
        row.put("address", "北京市东城区XX路XX号");
        row.put("marital_status", "M");
        row.put("active", true);
        return row;
    }

    // INTEROP-001: FHIR Encounter Mock数据
    private Map<String, Object> fhirEncounter(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("resource_type", "Encounter");
        row.put("status", "in-progress");
        row.put("class_code", "IMP");
        row.put("class_display", "住院");
        row.put("type", Arrays.asList(new LinkedHashMap<String, Object>() {{ put("code", "EMER"); put("display", "急诊入院"); }}));
        row.put("period_start", "2026-05-15T10:00:00+08:00");
        row.put("period_end", null);
        row.put("length_value", 5);
        row.put("length_unit", "d");
        row.put("priority", new LinkedHashMap<String, Object>() {{ put("code", "URG"); put("display", "紧急"); }});
        row.put("hospitalization", new LinkedHashMap<String, Object>() {{
            put("admit_source", new LinkedHashMap<String, Object>() {{ put("code", "EMR"); put("display", "急诊转入住"); }});
            put("diet_preference", Arrays.asList(new LinkedHashMap<String, Object>() {{ put("code", "REGULAR"); put("display", "普通饮食"); }}));
        }});
        return row;
    }

    // INTEROP-001: CDA Document Mock数据
    private Map<String, Object> cdaDocument(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("document_id", "CDA_202605150001");
        row.put("document_type", "ADMISSION_NOTE");
        row.put("document_title", "入院记录");
        row.put("template_id", "2.16.840.1.113883.10.20.22.1.1");
        row.put("template_name", "CCD v1.0 入院记录");
        row.put("status", "final");
        row.put("confidentiality", "N");
        row.put("language", "zh-CN");
        row.put("created_time", "2026-05-15T10:20:00+08:00");
        row.put("author", "李四");
        row.put("custodian", "XX医院");
        row.put("content", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ClinicalDocument xmlns=\"urn:hl7-org:v3\"><title>入院记录</title>...</ClinicalDocument>");
        row.put("hash", "sha256:abc123def456");
        row.put("size_bytes", 15360);
        return row;
    }

    // INTEROP-001: HL7 v2 ORU Mock数据
    private Map<String, Object> hl7OruResult(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("message_id", "MSG_202605150001");
        row.put("message_type", "ORU^R01");
        row.put("sending_application", "LIS_SYSTEM");
        row.put("sending_facility", "XX医院检验科");
        row.put("receiving_application", "MEDKERNEL");
        row.put("receiving_facility", "MEDKERNEL_SERVER");
        row.put("message_time", "2026-05-15T10:31:00+08:00");
        row.put("order_code", "TNI");
        row.put("order_name", "肌钙蛋白I");
        row.put("result_value", "1.62");
        row.put("unit", "ng/mL");
        row.put("reference_range", "0-0.04");
        row.put("abnormal_flag", "H");
        row.put("result_status", "F");
        row.put("result_time", "2026-05-15T10:31:00+08:00");
        row.put("specimen_type", "BLD");
        row.put("specimen_source", "静脉血");
        return row;
    }

    // INTEROP-001: DICOM Study Mock数据
    private Map<String, Object> dicomStudy(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("study_instance_uid", "1.2.840.113619.2.384.12345");
        row.put("accession_number", "ACC_202605150001");
        row.put("modality", "CT");
        row.put("study_date", "2026-05-15");
        row.put("study_time", "11:00:00");
        row.put("study_description", "胸部CT平扫");
        row.put("referring_physician_name", "王五");
        row.put("performing_physician_name", "赵六");
        row.put("series_count", 3);
        row.put("instance_count", 150);
        row.put("body_part", "CHEST");
        row.put("institution_name", "XX医院放射科");
        row.put("station_name", "CT_SCANNER_01");
        return row;
    }

    // INTEROP-001: FHIR ImagingStudy Mock数据
    private Map<String, Object> fhirImagingStudy(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("resource_type", "ImagingStudy");
        row.put("imaging_study_id", "IMG_STUDY_001");
        row.put("status", "available");
        row.put("modality", Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("system", "http://dicom.nema.org/resources/ontology/DCM"); put("code", "CT"); put("display", "CT"); }}
        ));
        row.put("started", "2026-05-15T11:00:00+08:00");
        row.put("description", "胸部CT平扫");
        row.put("number_of_series", 3);
        row.put("number_of_instances", 150);
        row.put("procedure_code", Arrays.asList(
                new LinkedHashMap<String, Object>() {{ put("system", "http://www.ama-assn.org/go/cpt"); put("code", "71260"); put("display", "胸部CT平扫"); }}
        ));
        row.put("series", Arrays.asList(
                new LinkedHashMap<String, Object>() {{
                    put("uid", "1.2.840.113619.2.384.12345.1");
                    put("number", 1);
                    put("modality", "CT");
                    put("description", "胸部CT轴位");
                    put("number_of_instances", 50);
                }}
        ));
        return row;
    }

    // INTEROP-001: CDS Hooks Result Mock数据
    private Map<String, Object> cdsHookResult(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("hook_id", "patient-risk-assessment");
        row.put("hook_type", "patient-view");
        row.put("cards", Arrays.asList(
                new LinkedHashMap<String, Object>() {{
                    put("summary", "急性心肌梗死高风险患者");
                    put("detail", "患者心电图显示相邻导联ST段抬高，肌钙蛋白I升高，建议立即启动AMI诊疗路径。");
                    put("indicator", "critical");
                    put("source", new LinkedHashMap<String, Object>() {{ put("label", "AMI诊疗指南"); put("url", "http://guidelines.hospital.local/ami"); }});
                    put("suggestions", Arrays.asList(
                            new LinkedHashMap<String, Object>() {{ put("label", "启动AMI路径"); put("uuid", "SUGGESTION_001"); put("actions", Arrays.asList(
                                    new LinkedHashMap<String, Object>() {{ put("type", "create"); put("description", "创建AMI患者实例"); }}
                            )); }},
                            new LinkedHashMap<String, Object>() {{ put("label", "申请急诊PCI"); put("uuid", "SUGGESTION_002"); put("actions", Arrays.asList(
                                    new LinkedHashMap<String, Object>() {{ put("type", "order"); put("description", "下达PCI医嘱"); }}
                            )); }}
                    ));
                    put("links", Arrays.asList(
                            new LinkedHashMap<String, Object>() {{ put("label", "AMI诊疗路径"); put("url", "http://pathway.hospital.local/ami"); put("type", "absolute"); }}
                    ));
                }}
        ));
        row.put("system_actions", Arrays.asList());
        return row;
    }

    // INTEROP-001: SMART on FHIR Launch Mock数据
    private Map<String, Object> smartLaunch(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("launch_id", "LAUNCH_202605150001");
        row.put("client_id", "medkernel-smart");
        row.put("scope", "launch patient/Patient.read patient/Observation.read");
        row.put("access_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.mock_token");
        row.put("token_type", "Bearer");
        row.put("expires_in", 3600);
        row.put("refresh_token", "refresh_token_mock");
        row.put("patient_id", params.getOrDefault("patient_id", "P_AMI_001"));
        row.put("encounter_id", params.getOrDefault("encounter_id", "E_AMI_001"));
        row.put("user_id", "Practitioner/DR_001");
        row.put("need_patient_banner", true);
        row.put("smart_style_url", "http://smart-style.hospital.local/styles.json");
        row.put("tenant", "T_DEFAULT");
        row.put("id_token", "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.mock_id_token");
        return row;
    }

    // INTEROP-001: IHE XDS Document Mock数据
    private Map<String, Object> iheXdsDocument(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("document_id", "XDS_DOC_202605150001");
        row.put("document_entry_uuid", "urn:uuid:12345678-1234-1234-1234-123456789012");
        row.put("repository_unique_id", "1.3.6.1.4.1.21367.2010.1.2.1040");
        row.put("document_type", "ADMISSION_NOTE");
        row.put("class_code", "34133-9");
        row.put("class_code_display", "Summary of episode note");
        row.put("confidentiality_code", "N");
        row.put("format_code", "urn:hl7-org:ccd:xml:1.0");
        row.put("patient_id", params.getOrDefault("patient_id", "P_AMI_001"));
        row.put("unique_id", "1.2.840.113619.2.384.12345.20260515.102000.1");
        row.put("hash", "sha256:abc123def456789");
        row.put("size_bytes", 15360);
        row.put("creation_time", "2026-05-15T10:20:00+08:00");
        row.put("language_code", "zh-CN");
        row.put("title", "入院记录");
        row.put("status", "approved");
        row.put("availability_status", "Approved");
        return row;
    }

    // INTEROP-001: Insurance Claim Mock数据
    private Map<String, Object> insuranceClaim(Map<String, Object> params) {
        Map<String, Object> row = patientRow(params);
        row.put("claim_id", "CLAIM_202605150001");
        row.put("claim_type", "INPATIENT");
        row.put("claim_status", "SETTLED");
        row.put("admission_date", "2026-05-15");
        row.put("discharge_date", "2026-05-20");
        row.put("diagnosis_codes", Arrays.asList("I21.3", "I25.1"));
        row.put("diagnosis_names", Arrays.asList("急性ST段抬高型心肌梗死", "动脉粥样硬化性心脏病"));
        row.put("total_amount", 85000.00);
        row.put("insurance_amount", 68000.00);
        row.put("self_pay_amount", 17000.00);
        row.put("settlement_time", "2026-05-20T15:30:00+08:00");
        row.put("insurance_type", "城镇职工基本医疗保险");
        row.put("insurer_code", "BJ_MEDICAL_INSURANCE");
        row.put("insurer_name", "北京市医疗保险事务管理中心");
        row.put("settlement_status", "COMPLETED");
        return row;
    }
}
