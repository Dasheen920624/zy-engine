package com.medkernel.graph;

import com.medkernel.audit.PublishGateService;
import com.medkernel.common.exception.MissingSourceException;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GraphService {
    private final GraphProperties properties;
    private final EnginePersistenceService persistenceService;
    private final GraphQueryService graphQueryService;
    private final GraphVersionService graphVersionService;
    private final Map<String, Map<String, Object>> graphVersions = new ConcurrentHashMap<String, Map<String, Object>>();
    // 图谱版本激活/回滚的串行化锁。多版本切换 ACTIVE→RETIRED 是一个 read-modify-write 序列，
    // 必须互斥防止并发时出现"多个版本同时 ACTIVE"或前一个 active 漏置 RETIRED。
    private final java.util.concurrent.locks.ReentrantLock graphVersionLock = new java.util.concurrent.locks.ReentrantLock();
    private final Map<String, Map<String, Object>> graphEvidences = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> graphNodes = new ConcurrentHashMap<String, Map<String, Object>>();
    private final List<Map<String, Object>> graphEdges = Collections.synchronizedList(new ArrayList<Map<String, Object>>());

    public GraphService(GraphProperties properties, EnginePersistenceService persistenceService,
                        GraphQueryService graphQueryService,
                        GraphVersionService graphVersionService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.graphQueryService = graphQueryService;
        this.graphVersionService = graphVersionService;
    }

    public List<Map<String, Object>> importGraphVersions(Object request) {
        return graphVersionService.importGraphVersions(request, graphVersions);
    }

    public List<Map<String, Object>> listGraphVersions() {
        return graphVersionService.listGraphVersions(graphVersions);
    }

    public Map<String, Object> getGraphVersion(String graphVersion) {
        return graphVersionService.getGraphVersion(graphVersion, graphVersions);
    }

    public Map<String, Object> activateGraphVersion(String graphVersion, Map<String, Object> request) {
        return graphVersionService.activateGraphVersion(graphVersion, request, graphVersions, graphVersionLock);
    }

    public Map<String, Object> rollbackVersion(String graphVersion, Map<String, Object> request) {
        return graphVersionService.rollbackVersion(graphVersion, request, graphVersions, graphVersionLock);
    }

    public List<Map<String, Object>> importGraphEvidences(Object request) {
        List<Map<String, Object>> entries = normalize(request, "evidences", "evidence_id");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("graph evidences list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<Map<String, Object>> staged = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                staged.add(toEvidenceEntry(entries.get(index)));
            } catch (IllegalArgumentException ex) {
                errors.add("evidences[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("graph evidences invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : staged) {
            graphEvidences.put((String) entry.get("evidence_id"), entry);
            imported.add(entry);
        }
        return imported;
    }

    public List<Map<String, Object>> listGraphEvidences(Map<String, String> filters) {
        String graphVersion = filterValue(filters, "graphVersion");
        String targetCode = filterValue(filters, "targetCode");
        String targetType = filterValue(filters, "targetType");
        String evidenceType = filterValue(filters, "evidenceType");
        int limit = filterInt(filters, "limit", 100);
        if (limit <= 0) {
            limit = 100;
        }

        List<Map<String, Object>> all = new ArrayList<Map<String, Object>>(graphEvidences.values());
        Collections.sort(all, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return String.valueOf(left.get("evidence_id")).compareTo(String.valueOf(right.get("evidence_id")));
            }
        });

        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : all) {
            if (graphVersion != null && !graphVersion.equalsIgnoreCase(String.valueOf(entry.get("graph_version")))) {
                continue;
            }
            if (targetCode != null && !targetCode.equalsIgnoreCase(String.valueOf(entry.get("target_code")))) {
                continue;
            }
            if (targetType != null && !targetType.equalsIgnoreCase(String.valueOf(entry.get("target_type")))) {
                continue;
            }
            if (evidenceType != null && !evidenceType.equalsIgnoreCase(String.valueOf(entry.get("evidence_type")))) {
                continue;
            }
            matched.add(entry);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public Map<String, Object> getGraphEvidence(String evidenceId) {
        Map<String, Object> entry = graphEvidences.get(evidenceId);
        if (entry == null) {
            throw new IllegalArgumentException("graph evidence not found: " + evidenceId);
        }
        return entry;
    }

    public List<Map<String, Object>> importGraphNodes(Object request) {
        List<Map<String, Object>> entries = normalize(request, "nodes", "code");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("graph nodes list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<Map<String, Object>> staged = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                staged.add(toNodeEntry(entries.get(index)));
            } catch (IllegalArgumentException ex) {
                errors.add("nodes[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("graph nodes invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : staged) {
            graphNodes.put(nodeKey((String) entry.get("graph_version"), (String) entry.get("code")), entry);
            imported.add(entry);
        }
        return imported;
    }

    public List<Map<String, Object>> listGraphNodes(Map<String, String> filters) {
        String graphVersion = filterValue(filters, "graphVersion");
        String type = filterValue(filters, "type");
        int limit = filterInt(filters, "limit", 200);
        if (limit <= 0) {
            limit = 200;
        }

        List<Map<String, Object>> all = new ArrayList<Map<String, Object>>(graphNodes.values());
        Collections.sort(all, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                int byVersion = String.valueOf(left.get("graph_version")).compareTo(String.valueOf(right.get("graph_version")));
                return byVersion != 0 ? byVersion : String.valueOf(left.get("code")).compareTo(String.valueOf(right.get("code")));
            }
        });

        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : all) {
            if (graphVersion != null && !graphVersion.equalsIgnoreCase(String.valueOf(entry.get("graph_version")))) {
                continue;
            }
            if (type != null && !type.equalsIgnoreCase(String.valueOf(entry.get("type")))) {
                continue;
            }
            matched.add(entry);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public List<Map<String, Object>> importGraphEdges(Object request) {
        List<Map<String, Object>> entries = normalize(request, "edges", "from_code");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("graph edges list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<Map<String, Object>> staged = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                staged.add(toEdgeEntry(entries.get(index)));
            } catch (IllegalArgumentException ex) {
                errors.add("edges[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("graph edges invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        synchronized (graphEdges) {
            for (Map<String, Object> entry : staged) {
                graphEdges.add(entry);
                imported.add(entry);
            }
        }
        return imported;
    }

    public List<Map<String, Object>> listGraphEdges(Map<String, String> filters) {
        String graphVersion = filterValue(filters, "graphVersion");
        String fromCode = filterValue(filters, "fromCode");
        String toCode = filterValue(filters, "toCode");
        String relationType = filterValue(filters, "relationType");
        int limit = filterInt(filters, "limit", 200);
        if (limit <= 0) {
            limit = 200;
        }

        List<Map<String, Object>> snapshot;
        synchronized (graphEdges) {
            snapshot = new ArrayList<Map<String, Object>>(graphEdges);
        }
        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> edge : snapshot) {
            if (graphVersion != null && !graphVersion.equalsIgnoreCase(String.valueOf(edge.get("graph_version")))) {
                continue;
            }
            if (fromCode != null && !fromCode.equalsIgnoreCase(String.valueOf(edge.get("from_code")))) {
                continue;
            }
            if (toCode != null && !toCode.equalsIgnoreCase(String.valueOf(edge.get("to_code")))) {
                continue;
            }
            if (relationType != null && !relationType.equalsIgnoreCase(String.valueOf(edge.get("relation_type")))) {
                continue;
            }
            matched.add(edge);
            if (matched.size() >= limit) {
                break;
            }
        }
        return matched;
    }

    public List<GraphCandidate> diseaseCandidates(Map<String, Object> request) {
        long start = System.currentTimeMillis();
        GraphCandidateQueryResult result = graphQueryService.diseaseCandidates(request, graphNodes, graphEdges);
        audit("DISEASE_CANDIDATES", "Disease", null, result.getGraphVersion(), result.getSource(),
                result.getCandidates().size(), result.isDegraded(), result.getDegradedReason(),
                System.currentTimeMillis() - start);
        return result.getCandidates();
    }

    public List<Map<String, Object>> evidence(Map<String, Object> request) {
        long start = System.currentTimeMillis();
        GraphEvidenceQueryResult result = graphQueryService.evidence(request, graphEvidences);
        audit("EVIDENCE_QUERY", "Evidence", result.getTargetCode(), result.getGraphVersion(), result.getSource(),
                result.getEvidence().size(), result.isDegraded(), result.getDegradedReason(),
                System.currentTimeMillis() - start);
        return result.getEvidence();
    }



    private void audit(String actionType, String targetType, String targetCode, String graphVersion, String source,
                       int resultCount, boolean degraded, String degradedReason, long elapsedMs) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("graph_version", graphVersion);
        detail.put("source", source);
        detail.put("result_count", resultCount);
        detail.put("degraded", degraded);
        detail.put("degraded_reason", degradedReason);
        detail.put("elapsed_ms", elapsedMs);
        try {
            persistenceService.saveAuditLog("GRAPH", actionType, targetType, targetCode, null, null, null, detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不应影响图谱召回和降级返回。
        }
    }



    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalize(Object request, String nestedKey, String singleHintField) {
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
            Object nested = map.get(nestedKey);
            if (nested instanceof List) {
                return normalize(nested, nestedKey, singleHintField);
            }
            if (map.containsKey(singleHintField)) {
                list.add(map);
            }
        }
        return list;
    }



    private Map<String, Object> toEvidenceEntry(Map<String, Object> entry) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("evidence_id", requireField(entry, "evidence_id"));
        view.put("graph_version", string(entry.get("graph_version"), properties.getDefaultVersion()));
        view.put("target_code", requireField(entry, "target_code"));
        view.put("target_type", canonical(string(entry.get("target_type"), "DISEASE")));
        view.put("evidence_type", canonical(string(entry.get("evidence_type"), "GUIDELINE")));
        view.put("title", string(entry.get("title"), String.valueOf(view.get("evidence_id"))));
        view.put("summary", string(entry.get("summary"), null));
        view.put("source", string(entry.get("source"), null));
        view.put("reference_document_code", string(entry.get("reference_document_code"), null));
        view.put("reference_binding_type", string(entry.get("reference_binding_type"), null));
        view.put("reference_url", string(entry.get("reference_url"), null));
        Object confidence = entry.get("confidence");
        if (confidence instanceof Number) {
            view.put("confidence", ((Number) confidence).doubleValue());
        } else {
            view.put("confidence", 1.0);
        }
        view.put("created_time", nowText());
        return view;
    }

    private Map<String, Object> toNodeEntry(Map<String, Object> entry) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("code", requireField(entry, "code"));
        view.put("name", string(entry.get("name"), String.valueOf(view.get("code"))));
        view.put("type", canonical(string(entry.get("type"), "DISEASE")));
        view.put("graph_version", string(entry.get("graph_version"), properties.getDefaultVersion()));
        view.put("description", string(entry.get("description"), null));
        view.put("source", string(entry.get("source"), null));
        view.put("created_time", nowText());
        return view;
    }

    private Map<String, Object> toEdgeEntry(Map<String, Object> entry) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("from_code", requireField(entry, "from_code"));
        view.put("to_code", requireField(entry, "to_code"));
        view.put("relation_type", canonical(string(entry.get("relation_type"), "RELATED_TO")));
        view.put("graph_version", string(entry.get("graph_version"), properties.getDefaultVersion()));
        Object weight = entry.get("weight");
        view.put("weight", weight instanceof Number ? ((Number) weight).doubleValue() : 0.5);
        view.put("description", string(entry.get("description"), null));
        view.put("created_time", nowText());
        return view;
    }

    private String nodeKey(String graphVersion, String code) {
        return string(graphVersion, "") + "::" + string(code, "");
    }





    private String filterValue(Map<String, String> filters, String key) {
        if (filters == null) {
            return null;
        }
        String value = filters.get(key);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int filterInt(Map<String, String> filters, String key, int defaultValue) {
        String value = filterValue(filters, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String canonical(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String requireField(Map<String, Object> entry, String field) {
        String value = string(entry.get(field), null);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private String nowText() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now());
    }

    // =========================================================================
    // GRAPH-005: 包级数据访问方法（供 GraphSyncService 使用）
    // =========================================================================

    /**
     * 获取指定版本的节点列表（包级访问，供 GraphSyncService）。
     */
    List<Map<String, Object>> getNodesByVersion(String graphVersion) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : graphNodes.values()) {
            if (graphVersion == null || graphVersion.equalsIgnoreCase(String.valueOf(entry.get("graph_version")))) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * 获取指定版本的边列表（包级访问，供 GraphSyncService）。
     */
    List<Map<String, Object>> getEdgesByVersion(String graphVersion) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : graphEdges) {
            if (graphVersion == null || graphVersion.equalsIgnoreCase(String.valueOf(entry.get("graph_version")))) {
                result.add(entry);
            }
        }
        return result;
    }
}
