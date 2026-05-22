package com.medkernel.adapter;

import com.medkernel.terminology.TerminologyService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ClinicalAdapterMockDataProvider extends AbstractAdapterMockDataProvider {

    public ClinicalAdapterMockDataProvider(TerminologyService terminologyService) {
        super(terminologyService);
    }

    @Override
    public boolean supports(String adapterCode, String queryCode) {
        if ("ECG_ADAPTER".equals(adapterCode) && "QUERY_ECG_REPORT".equals(queryCode)) return true;
        if ("LIS_ADAPTER".equals(adapterCode) && "QUERY_TROPONIN".equals(queryCode)) return true;
        if ("HIS_ADAPTER".equals(adapterCode) && "QUERY_DIAGNOSES".equals(queryCode)) return true;
        if ("EMR_ADAPTER".equals(adapterCode) && "QUERY_CHIEF_COMPLAINTS".equals(queryCode)) return true;
        if ("EMR_WS_ADAPTER".equals(adapterCode) && "QUERY_ADMISSION_NOTE".equals(queryCode)) return true;
        return false;
    }

    @Override
    public List<Map<String, Object>> provideRows(String adapterCode, String queryCode, Map<String, Object> params) {
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
        throw new UnsupportedOperationException("unsupported: " + adapterCode + "/" + queryCode);
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
}
