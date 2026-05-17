package com.zyengine.terminology;

import com.zyengine.common.TraceContext;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TerminologyService {
    private static final List<String> SUPPORTED_GOVERNANCE_STATUSES = java.util.Arrays.asList(
            "PENDING_MAPPING", "APPROVED", "REJECTED", "CONFLICT");

    private final EnginePersistenceService persistenceService;
    private final Map<String, ConceptMapping> mappings = new ConcurrentHashMap<String, ConceptMapping>();
    /**
     * 内存治理队列，key 为 sourceSystem::sourceCode::conceptType。
     * 持久化后仍保留内存镜像，便于快速查询。
     */
    private final Map<String, Map<String, Object>> governanceQueue = new ConcurrentHashMap<String, Map<String, Object>>();

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
            // 不回显用户输入的原始值，避免攻击者通过尝试不同值并观察 4xx vs 5xx 来枚举字典；
            // 详细 source_system/source_code 进入服务端日志便于运维定位。
            org.slf4j.LoggerFactory.getLogger(TerminologyService.class)
                    .info("[traceId={}] terminology mapping not found: sourceSystem={}, sourceCode={}, conceptType={}",
                            com.zyengine.common.TraceContext.getTraceId(),
                            canonical(sourceSystem), canonical(sourceCode), canonical(conceptType));
            throw new IllegalArgumentException("mapping not found");
        }
        return view(mapping);
    }

    // =========================================================================
    // 治理队列
    // =========================================================================

    /**
     * 查询未映射治理队列。
     * 优先从持久化层读取；若不可用则返回内存镜像。
     */
    public List<Map<String, Object>> listPendingMappings(Map<String, String> filters) {
        String tenantId = string(filters.get("tenantId"), "default");
        String governanceStatus = filters.get("governanceStatus");
        String sourceSystem = filters.get("sourceSystem");
        String conceptType = filters.get("conceptType");
        int limit = intValue(filters.get("limit"), 100);

        if (persistenceService.enabled()) {
            return persistenceService.listUnmappedQueue(tenantId, governanceStatus, sourceSystem, conceptType, limit);
        }
        // 内存回退
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : governanceQueue.values()) {
            if (governanceStatus != null && !governanceStatus.equals(string(entry.get("governance_status"), null))) {
                continue;
            }
            if (sourceSystem != null && !sourceSystem.equals(string(entry.get("source_system"), null))) {
                continue;
            }
            if (conceptType != null && !conceptType.equals(string(entry.get("concept_type"), null))) {
                continue;
            }
            results.add(entry);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    /**
     * 审批映射：将治理队列中的 PENDING_MAPPING 记录标记为 APPROVED，
     * 并将建议的标准码写入映射缓存。
     */
    public Map<String, Object> approvePendingMapping(String queueId, Map<String, Object> request) {
        Map<String, Object> entry = findQueueEntry(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("queue entry not found: " + queueId);
        }
        if (!"PENDING_MAPPING".equals(string(entry.get("governance_status"), null))) {
            throw new IllegalArgumentException("queue entry is not in PENDING_MAPPING status: " + queueId);
        }

        String reviewedBy = string(request.get("reviewed_by"), "SYSTEM");
        String reviewComment = string(request.get("review_comment"), null);
        // 审批时可覆盖建议标准码
        String approvedStandardCode = string(request.get("standard_code"),
                string(entry.get("proposed_standard_code"), null));
        String approvedStandardName = string(request.get("standard_name"),
                string(entry.get("proposed_standard_name"), approvedStandardCode));

        if (approvedStandardCode == null || approvedStandardCode.isEmpty()) {
            throw new IllegalArgumentException("standard_code is required for approval");
        }

        // 更新治理队列状态
        if (persistenceService.enabled()) {
            persistenceService.updateUnmappedQueueStatus(queueId,
                    string(entry.get("tenant_id"), "default"), "APPROVED", reviewedBy, reviewComment);
        }
        // 更新内存镜像
        entry.put("governance_status", "APPROVED");
        entry.put("reviewed_by", reviewedBy);
        entry.put("reviewed_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        entry.put("review_comment", reviewComment);

        // 将审批后的映射写入映射缓存
        String sourceSystem = string(entry.get("source_system"), "");
        String sourceCode = string(entry.get("source_code"), "");
        String sourceName = string(entry.get("source_name"), "");
        String conceptType = string(entry.get("concept_type"), "");
        register(sourceSystem, sourceCode, sourceName, conceptType,
                approvedStandardCode, approvedStandardName, "APPROVED", 0.90, "MANUAL_REVIEW");

        // 从 PENDING_MAPPING 内存队列中移除（已改为 APPROVED）
        String govKey = governanceKey(sourceSystem, sourceCode, conceptType, "PENDING_MAPPING");
        governanceQueue.remove(govKey);

        Map<String, Object> result = new LinkedHashMap<String, Object>(entry);
        result.put("standard_code", approvedStandardCode);
        result.put("standard_name", approvedStandardName);
        return result;
    }

    /**
     * 驳回映射：将治理队列中的 PENDING_MAPPING 记录标记为 REJECTED。
     */
    public Map<String, Object> rejectPendingMapping(String queueId, Map<String, Object> request) {
        Map<String, Object> entry = findQueueEntry(queueId);
        if (entry == null) {
            throw new IllegalArgumentException("queue entry not found: " + queueId);
        }
        if (!"PENDING_MAPPING".equals(string(entry.get("governance_status"), null))) {
            throw new IllegalArgumentException("queue entry is not in PENDING_MAPPING status: " + queueId);
        }

        String reviewedBy = string(request.get("reviewed_by"), "SYSTEM");
        String reviewComment = string(request.get("review_comment"), null);

        if (persistenceService.enabled()) {
            persistenceService.updateUnmappedQueueStatus(queueId,
                    string(entry.get("tenant_id"), "default"), "REJECTED", reviewedBy, reviewComment);
        }

        entry.put("governance_status", "REJECTED");
        entry.put("reviewed_by", reviewedBy);
        entry.put("reviewed_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        entry.put("review_comment", reviewComment);

        String sourceSystem = string(entry.get("source_system"), "");
        String sourceCode = string(entry.get("source_code"), "");
        String conceptType = string(entry.get("concept_type"), "");
        String govKey = governanceKey(sourceSystem, sourceCode, conceptType, "PENDING_MAPPING");
        governanceQueue.remove(govKey);

        return new LinkedHashMap<String, Object>(entry);
    }

    private Map<String, Object> findQueueEntry(String queueId) {
        for (Map<String, Object> entry : governanceQueue.values()) {
            if (queueId.equals(string(entry.get("queue_id"), null))) {
                return entry;
            }
        }
        return null;
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
        // 写入治理队列（持久化 + 内存镜像）
        String queueId = "TQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> queueEntry = new LinkedHashMap<String, Object>();
        queueEntry.put("tenant_id", "default");
        queueEntry.put("queue_id", queueId);
        queueEntry.put("source_system", sourceSystem);
        queueEntry.put("source_code", sourceCode);
        queueEntry.put("source_name", sourceName);
        queueEntry.put("concept_type", conceptType);
        queueEntry.put("governance_status", "PENDING_MAPPING");
        queueEntry.put("proposed_standard_code", null);
        queueEntry.put("proposed_standard_name", null);
        queueEntry.put("proposed_confidence", 0);
        queueEntry.put("proposed_mapping_source", null);
        queueEntry.put("occurrence_count", 1);
        queueEntry.put("last_occurrence_time", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        String govKey = governanceKey(sourceSystem, sourceCode, conceptType, "PENDING_MAPPING");
        Map<String, Object> existing = governanceQueue.get(govKey);
        if (existing != null) {
            // 已存在则增加计数
            int count = intValue(existing.get("occurrence_count"), 1) + 1;
            existing.put("occurrence_count", count);
            existing.put("last_occurrence_time",
                    OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            queueEntry = existing;
        } else {
            governanceQueue.put(govKey, queueEntry);
        }

        // 持久化
        try {
            persistenceService.saveUnmappedQueueEntry(queueEntry);
        } catch (RuntimeException ignored) {
            // 治理队列落库失败不阻断标准化主流程。
        }

        Map<String, Object> result = base(sourceSystem, sourceCode, sourceName, conceptType);
        result.put("matched", false);
        result.put("standard_code", null);
        result.put("standard_name", null);
        result.put("mapping_status", "UNMAPPED");
        result.put("confidence", 0);
        result.put("mapping_source", "GOVERNANCE_QUEUE");
        result.put("governance_status", "PENDING_MAPPING");
        result.put("queue_id", queueId);
        result.put("message", "未找到已审核映射，已进入待字典治理队列。");
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

    private String governanceKey(String sourceSystem, String sourceCode, String conceptType, String status) {
        return sourceSystem + "::" + sourceCode + "::" + conceptType + "::" + status;
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

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
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
