package com.zyengine.terminology;

import com.zyengine.common.TraceContext;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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

    public List<Map<String, Object>> importMappings(Object request) {
        List<Map<String, Object>> entries = normalizeMappings(request);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("mappings list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<ConceptMapping> staged = new ArrayList<ConceptMapping>();
        for (int index = 0; index < entries.size(); index++) {
            Map<String, Object> entry = entries.get(index);
            try {
                staged.add(toMapping(entry));
            } catch (IllegalArgumentException ex) {
                errors.add("mappings[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            // 与路径配置导入一致，校验失败时整体回退，不污染已审核的字典映射数据。
            throw new IllegalArgumentException("terminology mappings invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (ConceptMapping mapping : staged) {
            mappings.put(key(mapping.sourceSystem, mapping.sourceCode, mapping.conceptType), mapping);
            imported.add(view(mapping));
        }
        return imported;
    }

    public List<Map<String, Object>> listMappings() {
        List<ConceptMapping> list = new ArrayList<ConceptMapping>(mappings.values());
        Collections.sort(list, new Comparator<ConceptMapping>() {
            @Override
            public int compare(ConceptMapping left, ConceptMapping right) {
                int bySystem = left.sourceSystem.compareTo(right.sourceSystem);
                if (bySystem != 0) {
                    return bySystem;
                }
                int byType = left.conceptType.compareTo(right.conceptType);
                return byType != 0 ? byType : left.sourceCode.compareTo(right.sourceCode);
            }
        });
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (ConceptMapping mapping : list) {
            result.add(view(mapping));
        }
        return result;
    }

    public Map<String, Object> getMapping(String sourceSystem, String sourceCode, String conceptType) {
        ConceptMapping mapping = mappings.get(key(canonical(sourceSystem), canonical(sourceCode), canonical(conceptType)));
        if (mapping == null) {
            throw new IllegalArgumentException(
                    "mapping not found: " + canonical(sourceSystem) + "/" + canonical(sourceCode) + "/" + canonical(conceptType));
        }
        return view(mapping);
    }

    private Map<String, Object> mapped(ConceptMapping mapping, String sourceName) {
        Map<String, Object> result = base(mapping.sourceSystem, mapping.sourceCode,
                string(sourceName, mapping.sourceName), mapping.conceptType);
        result.put("matched", true);
        result.put("standard_code", mapping.standardCode);
        result.put("standard_name", mapping.standardName);
        result.put("mapping_status", mapping.mappingStatus);
        result.put("confidence", mapping.confidence);
        result.put("mapping_source", mapping.mappingSource);
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

    private Map<String, Object> view(ConceptMapping mapping) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("source_system", mapping.sourceSystem);
        view.put("source_code", mapping.sourceCode);
        view.put("source_name", mapping.sourceName);
        view.put("concept_type", mapping.conceptType);
        view.put("standard_code", mapping.standardCode);
        view.put("standard_name", mapping.standardName);
        view.put("mapping_status", mapping.mappingStatus);
        view.put("confidence", mapping.confidence);
        view.put("mapping_source", mapping.mappingSource);
        return view;
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
                "AMI_STEMI", "急性ST段抬高型心肌梗死", "APPROVED", 0.98, "BUILT_IN_SAMPLE");
        register("HIS", "I21.3", "急性ST段抬高型心肌梗死", "DIAGNOSIS",
                "AMI_STEMI", "急性ST段抬高型心肌梗死", "APPROVED", 0.99, "BUILT_IN_SAMPLE");
        register("EMR", "CHEST_PAIN", "胸痛", "SYMPTOM",
                "CHEST_PAIN", "胸痛", "APPROVED", 0.97, "BUILT_IN_SAMPLE");
        register("ECG", "ST_ELEVATION", "ST段抬高", "FINDING",
                "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", "APPROVED", 0.96, "BUILT_IN_SAMPLE");
        register("ECG", "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", "FINDING",
                "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", "APPROVED", 1.00, "BUILT_IN_SAMPLE");
        register("LIS", "TNI", "肌钙蛋白I", "LAB_ITEM",
                "TROPONIN_I", "肌钙蛋白I", "APPROVED", 0.95, "BUILT_IN_SAMPLE");
        register("LIS", "CTNI", "肌钙蛋白I", "LAB_ITEM",
                "TROPONIN_I", "肌钙蛋白I", "APPROVED", 0.95, "BUILT_IN_SAMPLE");
        register("HIS", "ER", "急诊科", "DEPARTMENT",
                "ER", "急诊科", "APPROVED", 1.00, "BUILT_IN_SAMPLE");
    }

    private void register(String sourceSystem, String sourceCode, String sourceName, String conceptType,
                          String standardCode, String standardName, String mappingStatus,
                          double confidence, String mappingSource) {
        ConceptMapping mapping = new ConceptMapping();
        mapping.sourceSystem = canonical(sourceSystem);
        mapping.sourceCode = canonical(sourceCode);
        mapping.sourceName = sourceName;
        mapping.conceptType = canonical(conceptType);
        mapping.standardCode = standardCode;
        mapping.standardName = standardName;
        mapping.mappingStatus = mappingStatus;
        mapping.confidence = confidence;
        mapping.mappingSource = mappingSource;
        mappings.put(key(mapping.sourceSystem, mapping.sourceCode, mapping.conceptType), mapping);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeMappings(Object request) {
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
            Object nested = map.get("mappings");
            if (nested instanceof List) {
                return normalizeMappings(nested);
            }
            if (map.containsKey("source_system") || map.containsKey("source_code")) {
                list.add(map);
            }
        }
        return list;
    }

    private ConceptMapping toMapping(Map<String, Object> entry) {
        ConceptMapping mapping = new ConceptMapping();
        mapping.sourceSystem = canonical(requireField(entry, "source_system"));
        mapping.sourceCode = canonical(requireField(entry, "source_code"));
        mapping.sourceName = string(entry.get("source_name"), null);
        mapping.conceptType = canonical(requireField(entry, "concept_type"));
        mapping.standardCode = requireField(entry, "standard_code");
        mapping.standardName = string(entry.get("standard_name"), mapping.standardCode);
        mapping.mappingStatus = canonical(string(entry.get("mapping_status"), "APPROVED"));
        mapping.confidence = doubleValue(entry.get("confidence"), 1.00);
        mapping.mappingSource = string(entry.get("mapping_source"), "IMPORTED");
        return mapping;
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

    private String requireField(Map<String, Object> entry, String field) {
        String value = string(entry.get(field), null);
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

    private double doubleValue(Object value, double defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
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
        private String mappingSource;
    }
}
