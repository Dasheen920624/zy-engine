package com.zyengine.terminology;

import com.zyengine.common.TraceContext;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TerminologyService {
    private final EnginePersistenceService persistenceService;
    private final Map<String, ConceptMapping> mappings = new ConcurrentHashMap<String, ConceptMapping>();

    public TerminologyService(EnginePersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        seedMappings();
    }

    public Map<String, Object> normalize(Map<String, Object> request) {
        String sourceSystem = required(request, "source_system");
        String sourceCode = required(request, "source_code");
        String sourceName = string(request.get("source_name"), null);
        String conceptType = required(request, "concept_type");
        Map<String, Object> result = normalizeCode(sourceSystem, sourceCode, sourceName, conceptType);
        audit(request, result);
        return result;
    }

    public Map<String, Object> normalizeCode(String sourceSystem, String sourceCode,
                                             String sourceName, String conceptType) {
        String normalizedSourceSystem = canonical(sourceSystem);
        String normalizedSourceCode = canonical(sourceCode);
        String normalizedConceptType = canonical(conceptType);
        ConceptMapping mapping = mappings.get(key(normalizedSourceSystem, normalizedSourceCode, normalizedConceptType));
        if (mapping == null) {
            return unmapped(normalizedSourceSystem, normalizedSourceCode, sourceName, normalizedConceptType);
        }
        return mapped(mapping, sourceName);
    }

    private Map<String, Object> mapped(ConceptMapping mapping, String sourceName) {
        Map<String, Object> result = base(mapping.sourceSystem, mapping.sourceCode,
                string(sourceName, mapping.sourceName), mapping.conceptType);
        result.put("matched", true);
        result.put("standard_code", mapping.standardCode);
        result.put("standard_name", mapping.standardName);
        result.put("mapping_status", mapping.mappingStatus);
        result.put("confidence", mapping.confidence);
        result.put("mapping_source", "BUILT_IN_SAMPLE");
        result.put("governance_status", "READY");
        result.put("message", "字典映射命中。");
        return result;
    }

    private Map<String, Object> unmapped(String sourceSystem, String sourceCode,
                                         String sourceName, String conceptType) {
        Map<String, Object> result = base(sourceSystem, sourceCode, sourceName, conceptType);
        result.put("matched", false);
        result.put("standard_code", null);
        result.put("standard_name", null);
        result.put("mapping_status", "UNMAPPED");
        result.put("confidence", 0);
        result.put("mapping_source", "MOCK_GOVERNANCE_QUEUE");
        result.put("governance_status", "PENDING_MAPPING");
        result.put("message", "未找到已审核映射，已标记为待字典治理。");
        return result;
    }

    private Map<String, Object> base(String sourceSystem, String sourceCode,
                                     String sourceName, String conceptType) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("source_system", sourceSystem);
        result.put("source_code", sourceCode);
        result.put("source_name", sourceName);
        result.put("concept_type", conceptType);
        result.put("trace_id", TraceContext.getTraceId());
        return result;
    }

    private void audit(Map<String, Object> request, Map<String, Object> result) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("source_system", result.get("source_system"));
        detail.put("source_code", result.get("source_code"));
        detail.put("concept_type", result.get("concept_type"));
        detail.put("standard_code", result.get("standard_code"));
        detail.put("matched", result.get("matched"));
        detail.put("mapping_status", result.get("mapping_status"));
        try {
            persistenceService.saveAuditLog("TERMINOLOGY", "NORMALIZE", "CONCEPT",
                    String.valueOf(result.get("source_code")),
                    string(request.get("patient_id"), null),
                    string(request.get("encounter_id"), null),
                    string(request.get("operator_id"), null),
                    detail);
        } catch (RuntimeException ignored) {
            // 字典标准化不应被审计落库失败打断。
        }
    }

    private void seedMappings() {
        register("HIS", "I21.0", "急性前壁ST段抬高型心肌梗死", "DIAGNOSIS",
                "AMI_STEMI", "急性ST段抬高型心肌梗死", 0.98);
        register("HIS", "I21.3", "急性ST段抬高型心肌梗死", "DIAGNOSIS",
                "AMI_STEMI", "急性ST段抬高型心肌梗死", 0.99);
        register("EMR", "CHEST_PAIN", "胸痛", "SYMPTOM",
                "CHEST_PAIN", "胸痛", 0.97);
        register("ECG", "ST_ELEVATION", "ST段抬高", "FINDING",
                "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", 0.96);
        register("ECG", "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", "FINDING",
                "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", 1.00);
        register("LIS", "TNI", "肌钙蛋白I", "LAB_ITEM",
                "TROPONIN_I", "肌钙蛋白I", 0.95);
        register("LIS", "CTNI", "肌钙蛋白I", "LAB_ITEM",
                "TROPONIN_I", "肌钙蛋白I", 0.95);
        register("HIS", "ER", "急诊科", "DEPARTMENT",
                "ER", "急诊科", 1.00);
    }

    private void register(String sourceSystem, String sourceCode, String sourceName, String conceptType,
                          String standardCode, String standardName, double confidence) {
        ConceptMapping mapping = new ConceptMapping();
        mapping.sourceSystem = canonical(sourceSystem);
        mapping.sourceCode = canonical(sourceCode);
        mapping.sourceName = sourceName;
        mapping.conceptType = canonical(conceptType);
        mapping.standardCode = standardCode;
        mapping.standardName = standardName;
        mapping.mappingStatus = "APPROVED";
        mapping.confidence = confidence;
        mappings.put(key(mapping.sourceSystem, mapping.sourceCode, mapping.conceptType), mapping);
    }

    private String key(String sourceSystem, String sourceCode, String conceptType) {
        return sourceSystem + "::" + sourceCode + "::" + conceptType;
    }

    private String required(Map<String, Object> request, String field) {
        String value = string(request.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
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

    private static class ConceptMapping {
        private String sourceSystem;
        private String sourceCode;
        private String sourceName;
        private String conceptType;
        private String standardCode;
        private String standardName;
        private String mappingStatus;
        private double confidence;
    }
}
