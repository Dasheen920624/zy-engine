package com.medkernel.adapter;

import com.medkernel.common.OrgDefaults;
import com.medkernel.terminology.TerminologyService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAdapterMockDataProvider implements AdapterMockDataProvider {

    protected final TerminologyService terminologyService;

    protected AbstractAdapterMockDataProvider(TerminologyService terminologyService) {
        this.terminologyService = terminologyService;
    }

    protected Map<String, Object> patientRow(Map<String, Object> params) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("patient_id", string(params.get("patient_id"), "P_AMI_001"));
        row.put("encounter_id", string(params.get("encounter_id"), "E_AMI_001"));
        row.put("tenant_id", string(params.get("tenant_id"), OrgDefaults.DEFAULT_TENANT_ID));
        row.put("org_code", string(params.get("org_code"), OrgDefaults.DEFAULT_HOSPITAL_CODE));
        return row;
    }

    protected void putNormalized(Map<String, Object> row, String sourceSystem, String sourceCode,
                                 String sourceName, String conceptType, String prefix) {
        Map<String, Object> normalized = terminologyService.normalizeCode(sourceSystem, sourceCode, sourceName, conceptType);
        row.put(prefix + "_standard_code", normalized.get("standard_code"));
        row.put(prefix + "_standard_name", normalized.get("standard_name"));
        row.put(prefix + "_mapping_status", normalized.get("mapping_status"));
        row.put(prefix + "_mapping_confidence", normalized.get("confidence"));
    }

    protected List<Map<String, Object>> single(Map<String, Object> row) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.add(row);
        return rows;
    }

    protected List<Map<String, Object>> rowsWithPatientContext(List<Map<String, Object>> templateRows,
                                                               Map<String, Object> params) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> template : templateRows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>(patientRow(params));
            row.putAll(template);
            rows.add(row);
        }
        return rows;
    }

    protected String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }
}
