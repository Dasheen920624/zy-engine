package com.medkernel.graph;

import com.medkernel.audit.PublishGateService;
import com.medkernel.common.TraceContext;
import com.medkernel.persistence.EnginePersistenceService;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class GraphService {
    private final GraphProperties properties;
    private final EnginePersistenceService persistenceService;
    private final PublishGateService publishGateService;
    private final Map<String, Map<String, Object>> graphVersions = new ConcurrentHashMap<String, Map<String, Object>>();
    // 图谱版本激活/回滚的串行化锁。多版本切换 ACTIVE→RETIRED 是一个 read-modify-write 序列，
    // 必须互斥防止并发时出现"多个版本同时 ACTIVE"或前一个 active 漏置 RETIRED。
    private final java.util.concurrent.locks.ReentrantLock graphVersionLock = new java.util.concurrent.locks.ReentrantLock();
    private final Map<String, Map<String, Object>> graphEvidences = new ConcurrentHashMap<String, Map<String, Object>>();
    private final Map<String, Map<String, Object>> graphNodes = new ConcurrentHashMap<String, Map<String, Object>>();
    private final List<Map<String, Object>> graphEdges = Collections.synchronizedList(new ArrayList<Map<String, Object>>());

    public GraphService(GraphProperties properties, EnginePersistenceService persistenceService,
                        PublishGateService publishGateService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
        this.publishGateService = publishGateService;
    }

    public List<Map<String, Object>> importGraphVersions(Object request) {
        List<Map<String, Object>> entries = normalize(request, "versions", "graph_version");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("graph versions list is empty");
        }
        List<String> errors = new ArrayList<String>();
        List<Map<String, Object>> staged = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < entries.size(); index++) {
            try {
                staged.add(toVersionEntry(entries.get(index)));
            } catch (IllegalArgumentException ex) {
                errors.add("versions[" + index + "]: " + ex.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("graph versions invalid: " + errors);
        }

        List<Map<String, Object>> imported = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : staged) {
            graphVersions.put((String) entry.get("graph_version"), entry);
            imported.add(entry);
        }
        return imported;
    }

    public List<Map<String, Object>> listGraphVersions() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(graphVersions.values());
        Collections.sort(list, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return String.valueOf(left.get("graph_version")).compareTo(String.valueOf(right.get("graph_version")));
            }
        });
        return list;
    }

    public Map<String, Object> getGraphVersion(String graphVersion) {
        Map<String, Object> entry = graphVersions.get(graphVersion);
        if (entry == null) {
            throw new IllegalArgumentException("graph version not found: " + graphVersion);
        }
        return entry;
    }

    public Map<String, Object> activateGraphVersion(String graphVersion, Map<String, Object> request) {
        graphVersionLock.lock();
        try {
            Map<String, Object> entry = graphVersions.get(graphVersion);
            if (entry == null) {
                throw new IllegalArgumentException("graph version not found: " + graphVersion);
            }

            // 发布门禁：来源绑定检查（阻断级，对应产品不变量 H4）
            String refDoc = string(entry.get("reference_document_code"), null);
            PublishGateService.GateCheckResult gateResult = publishGateService.checkGraphReference(graphVersion, refDoc);
            String operatorId = string(request == null ? null : request.get("published_by"), "SYSTEM");
            publishGateService.auditGateCheck("GRAPH", "ACTIVATE", "GRAPH_VERSION", graphVersion, operatorId, gateResult);
            if (!gateResult.isReadyToPublish()) {
                throw new IllegalStateException(publishGateService.formatBlockingMessage(gateResult));
            }

            // 同一版本号即唯一键，激活时把所有同 family 前缀（::之前）的其他版本置 RETIRED，便于多版本共存时切换。
            String family = versionFamily(graphVersion);
            for (Map<String, Object> other : graphVersions.values()) {
                String otherVersion = String.valueOf(other.get("graph_version"));
                if (!otherVersion.equals(graphVersion) && versionFamily(otherVersion).equals(family)
                        && "ACTIVE".equals(other.get("status"))) {
                    other.put("status", "RETIRED");
                    other.put("retired_time", nowText());
                }
            }
            entry.put("status", "ACTIVE");
            entry.put("published_by", operatorId);
            entry.put("published_time", nowText());
            entry.put("reference_warnings", gateResult.toMapList());

            // 审计日志：图谱版本激活操作
            Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
            auditDetail.put("graph_version", graphVersion);
            auditDetail.put("action", "ACTIVATE");
            auditDetail.put("operator_id", operatorId);
            auditDetail.put("gate_check_ready", gateResult.isReadyToPublish());
            persistenceService.saveAuditLog("GRAPH", "ACTIVATE", "GRAPH_VERSION", graphVersion, null, null, operatorId, auditDetail);

            return entry;
        } finally {
            graphVersionLock.unlock();
        }
    }

    public Map<String, Object> rollbackVersion(String graphVersion, Map<String, Object> request) {
        // 与 activateGraphVersion 共享 graphVersionLock：回滚也是 read-modify-write 序列，
        // 必须互斥防止并发回滚导致多版本同时 ACTIVE。
        graphVersionLock.lock();
        try {
            Map<String, Object> target = graphVersions.get(graphVersion);
            if (target == null) {
                throw new IllegalArgumentException("graph version not found: " + graphVersion);
            }

            String previousActiveVersion = null;
            for (Map<String, Object> existing : graphVersions.values()) {
                if ("ACTIVE".equals(string(existing.get("status"), null))) {
                    previousActiveVersion = string(existing.get("graph_version"), null);
                    existing.put("status", "RETIRED");
                    existing.put("retired_time", nowText());
                }
            }

            target.put("status", "ACTIVE");
            String operatorId = string(request == null ? null : request.get("published_by"), "SYSTEM");
            target.put("published_by", operatorId);
            target.put("published_time", nowText());

            Map<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("graph_version", graphVersion);
            result.put("status", "ACTIVE");
            result.put("previous_active_version", previousActiveVersion);
            result.put("rolled_back_by", operatorId);
            result.put("rolled_back_time", nowText());

            // 审计日志：图谱版本回滚操作
            Map<String, Object> auditDetail = new LinkedHashMap<String, Object>();
            auditDetail.put("graph_version", graphVersion);
            auditDetail.put("action", "ROLLBACK");
            auditDetail.put("previous_active_version", previousActiveVersion);
            auditDetail.put("operator_id", operatorId);
            persistenceService.saveAuditLog("GRAPH", "ROLLBACK", "GRAPH_VERSION", graphVersion, null, null, operatorId, auditDetail);

            return result;
        } finally {
            graphVersionLock.unlock();
        }
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
        String graphVersion = graphVersion(request);
        try {
            if (properties.ready()) {
                List<GraphCandidate> candidates = queryNeo4jCandidates(request, graphVersion);
                audit("DISEASE_CANDIDATES", "Disease", null, graphVersion, "NEO4J",
                        candidates.size(), false, null, System.currentTimeMillis() - start);
                return candidates;
            }
            List<GraphCandidate> candidates = fallbackCandidates(request, graphVersion,
                    properties.isEnabled() ? "Neo4j配置不完整，已使用内置AMI图谱降级召回。" : null);
            audit("DISEASE_CANDIDATES", "Disease", null, graphVersion, "FALLBACK_HEURISTIC",
                    candidates.size(), properties.isEnabled(), degradedReason(candidates), System.currentTimeMillis() - start);
            return candidates;
        } catch (RuntimeException ex) {
            List<GraphCandidate> candidates = fallbackCandidates(request, graphVersion,
                    "Neo4j查询不可用，已使用内置AMI图谱降级召回：" + ex.getClass().getSimpleName());
            audit("DISEASE_CANDIDATES", "Disease", null, graphVersion, "FALLBACK_HEURISTIC",
                    candidates.size(), true, degradedReason(candidates), System.currentTimeMillis() - start);
            return candidates;
        }
    }

    public List<Map<String, Object>> evidence(Map<String, Object> request) {
        long start = System.currentTimeMillis();
        String targetCode = string(request.get("target_code"), null);
        String graphVersion = graphVersion(request);
        try {
            if (properties.ready()) {
                List<Map<String, Object>> evidence = queryNeo4jEvidence(targetCode, graphVersion);
                audit("EVIDENCE_QUERY", "Evidence", targetCode, graphVersion, "NEO4J",
                        evidence.size(), false, null, System.currentTimeMillis() - start);
                return evidence;
            }
            List<Map<String, Object>> evidence = fallbackEvidence(targetCode, graphVersion,
                    properties.isEnabled() ? "Neo4j配置不完整，已使用内置AMI证据降级返回。" : null);
            audit("EVIDENCE_QUERY", "Evidence", targetCode, graphVersion, "FALLBACK_HEURISTIC",
                    evidence.size(), properties.isEnabled(), degradedReasonFromMaps(evidence), System.currentTimeMillis() - start);
            return evidence;
        } catch (RuntimeException ex) {
            List<Map<String, Object>> evidence = fallbackEvidence(targetCode, graphVersion,
                    "Neo4j证据查询不可用，已使用内置AMI证据降级返回：" + ex.getClass().getSimpleName());
            audit("EVIDENCE_QUERY", "Evidence", targetCode, graphVersion, "FALLBACK_HEURISTIC",
                    evidence.size(), true, degradedReasonFromMaps(evidence), System.currentTimeMillis() - start);
            return evidence;
        }
    }

    private List<GraphCandidate> queryNeo4jCandidates(Map<String, Object> request, String graphVersion) {
        List<String> factCodes = factCodes(request);
        if (factCodes.isEmpty()) {
            return new ArrayList<GraphCandidate>();
        }
        String cypher = "MATCH (fact)-[r]->(d:Disease) " +
                "WHERE fact.code IN $fact_codes AND type(r) IN ['SUGGESTS','SUPPORTS_DIAGNOSIS','HAS_RISK_FACTOR','HAS_CORE_SYMPTOM','HAS_EXAM_FINDING'] " +
                "AND ($graph_version IS NULL OR coalesce(fact.graph_version, $graph_version) = $graph_version) " +
                "AND ($graph_version IS NULL OR coalesce(d.graph_version, $graph_version) = $graph_version) " +
                "OPTIONAL MATCH (d)-[:HAS_PATHWAY]->(p:Pathway) " +
                "OPTIONAL MATCH (ev:Evidence)-[:SUPPORTS_PATHWAY]->(p) " +
                "WITH d, collect(DISTINCT {type: type(r), code: fact.code, name: coalesce(fact.name, fact.code), weight: coalesce(r.weight, 0.5)}) AS relations, " +
                "collect(DISTINCT ev.code) AS evidenceRefs, sum(coalesce(r.weight, 0.5)) * 100 AS score " +
                "RETURN d.code AS disease_code, coalesce(d.name, d.code) AS disease_name, score, relations, evidenceRefs " +
                "ORDER BY score DESC LIMIT $limit";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("fact_codes", factCodes);
        params.put("graph_version", hasText(graphVersion) ? graphVersion : null);
        params.put("limit", integer(request.get("limit"), 10));

        // try-with-resources 保证 Driver 与 Session 在任何异常路径下都被释放，
        // 比手写嵌套 try/finally 更不易遗漏（GRAPH-003 review finding F-NEXT GRAPH-3）。
        try (Driver driver = newDriver();
             Session session = driver.session(sessionConfig())) {
            Result result = session.run(cypher, Values.value(params));
            List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
            while (result.hasNext()) {
                Record record = result.next();
                GraphCandidate candidate = new GraphCandidate();
                candidate.setDiseaseCode(record.get("disease_code").asString(null));
                candidate.setDiseaseName(record.get("disease_name").asString(null));
                candidate.setRawGraphScore(record.get("score").asDouble(0.0));
                candidate.setMatchedRelations(record.get("relations").asList(new Function<Value, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> apply(Value value) {
                        return value.asMap();
                    }
                }));
                candidate.setEvidenceRefs(cleanStrings(record.get("evidenceRefs").asList(new Function<Value, String>() {
                    @Override
                    public String apply(Value value) {
                        return value.isNull() ? null : value.asString(null);
                    }
                })));
                candidate.setGraphVersion(graphVersion);
                candidate.setGraphSource("NEO4J");
                candidates.add(candidate);
            }
            return candidates;
        }
    }

    private List<Map<String, Object>> queryNeo4jEvidence(String targetCode, String graphVersion) {
        if (!hasText(targetCode)) {
            return new ArrayList<Map<String, Object>>();
        }
        String cypher = "MATCH (ev:Evidence)-[r]->(target) " +
                "WHERE target.code = $target_code " +
                "AND ($graph_version IS NULL OR coalesce(ev.graph_version, $graph_version) = $graph_version) " +
                "RETURN ev.code AS evidence_id, coalesce(ev.title, ev.name, ev.code) AS title, coalesce(ev.source, '') AS source, " +
                "target.code AS target_code, type(r) AS relation_type " +
                "UNION " +
                "MATCH (ev:Evidence)-[r]->(:Pathway {code: $target_code}) " +
                "WHERE ($graph_version IS NULL OR coalesce(ev.graph_version, $graph_version) = $graph_version) " +
                "RETURN ev.code AS evidence_id, coalesce(ev.title, ev.name, ev.code) AS title, coalesce(ev.source, '') AS source, " +
                "$target_code AS target_code, type(r) AS relation_type";
        try (Driver driver = newDriver();
             Session session = driver.session(sessionConfig())) {
            Result result = session.run(cypher, Values.parameters("target_code", targetCode,
                    "graph_version", hasText(graphVersion) ? graphVersion : null));
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> evidence = new LinkedHashMap<String, Object>();
                evidence.put("evidence_id", record.get("evidence_id").asString(null));
                evidence.put("title", record.get("title").asString(null));
                evidence.put("source", record.get("source").asString(null));
                evidence.put("target_code", record.get("target_code").asString(targetCode));
                evidence.put("relation_type", record.get("relation_type").asString(null));
                evidence.put("graph_version", graphVersion);
                evidence.put("graph_source", "NEO4J");
                list.add(evidence);
            }
            return list;
        }
    }

    private List<GraphCandidate> fallbackCandidates(Map<String, Object> request, String graphVersion, String reason) {
        List<String> codes = factCodes(request);

        // 已注册图谱：用 imported edges 召回 Disease，得到任何 Disease 即采用导入数据；
        // 否则回退到内置 AMI 启发式，保持向后兼容。
        List<GraphCandidate> registered = registeredCandidatesFor(codes, graphVersion, reason);
        if (!registered.isEmpty()) {
            return registered;
        }

        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        boolean chestPain = codes.contains("CHEST_PAIN");
        boolean stElevation = codes.contains("ST_ELEVATION_CONTIGUOUS_LEADS");
        boolean diabetes = codes.contains("DIABETES");
        boolean hypertension = codes.contains("HYPERTENSION");

        if (chestPain || stElevation) {
            GraphCandidate candidate = new GraphCandidate();
            candidate.setDiseaseCode("AMI_STEMI");
            candidate.setDiseaseName("急性ST段抬高型心肌梗死");
            candidate.setRawGraphScore(stElevation ? 92 : 72);
            if (chestPain) {
                candidate.getMatchedRelations().add(relation("HAS_CORE_SYMPTOM", "CHEST_PAIN", "胸痛", 1.0));
            }
            if (stElevation) {
                candidate.getMatchedRelations().add(relation("HAS_EXAM_FINDING", "ST_ELEVATION_CONTIGUOUS_LEADS", "相邻导联ST段抬高", 1.0));
            }
            if (diabetes) {
                candidate.getMatchedRelations().add(relation("HAS_RISK_FACTOR", "DIABETES", "糖尿病", 0.45));
            }
            if (hypertension) {
                candidate.getMatchedRelations().add(relation("HAS_RISK_FACTOR", "HYPERTENSION", "高血压", 0.45));
            }
            candidate.setEvidenceRefs(Arrays.asList("EV_AMI_001"));
            candidate.setGraphVersion(graphVersion);
            candidate.setGraphSource("FALLBACK_HEURISTIC");
            if (reason != null) {
                candidate.setDegraded(true);
                candidate.setDegradedReason(reason);
            }
            candidates.add(candidate);
        }
        return candidates;
    }

    private List<Map<String, Object>> fallbackEvidence(String targetCode, String graphVersion, String reason) {
        // 优先使用已注册的图谱证据：targetCode 匹配时返回所有匹配项（不限版本），让用户可以通过 import 配置扩展演示数据。
        List<Map<String, Object>> registered = registeredEvidenceFor(targetCode, graphVersion);
        if (!registered.isEmpty()) {
            for (Map<String, Object> evidence : registered) {
                evidence.put("graph_source", "REGISTERED_FALLBACK");
                if (reason != null) {
                    evidence.put("degraded", true);
                    evidence.put("degraded_reason", reason);
                }
            }
            return registered;
        }

        Map<String, Object> evidence = new HashMap<String, Object>();
        evidence.put("evidence_id", "EV_AMI_001");
        evidence.put("title", "AMI院内路径专家审核证据");
        evidence.put("source", "院内AMI路径 V1.0");
        evidence.put("target_code", targetCode);
        evidence.put("graph_version", graphVersion);
        evidence.put("graph_source", "FALLBACK_HEURISTIC");
        if (reason != null) {
            evidence.put("degraded", true);
            evidence.put("degraded_reason", reason);
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(evidence);
        return list;
    }

    private List<GraphCandidate> registeredCandidatesFor(List<String> factCodes, String graphVersion, String reason) {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        if (factCodes == null || factCodes.isEmpty()) {
            return candidates;
        }
        List<Map<String, Object>> snapshot;
        synchronized (graphEdges) {
            snapshot = new ArrayList<Map<String, Object>>(graphEdges);
        }
        if (snapshot.isEmpty()) {
            return candidates;
        }

        // 聚合 disease -> matched edges/weight，让多 fact 命中同一 disease 时分数累加。
        Map<String, DiseaseAggregate> aggregates = new LinkedHashMap<String, DiseaseAggregate>();
        for (Map<String, Object> edge : snapshot) {
            String edgeVersion = String.valueOf(edge.get("graph_version"));
            if (graphVersion != null && !graphVersion.equalsIgnoreCase(edgeVersion)) {
                continue;
            }
            String fromCode = String.valueOf(edge.get("from_code"));
            if (!factCodes.contains(fromCode)) {
                continue;
            }
            String toCode = String.valueOf(edge.get("to_code"));
            Map<String, Object> diseaseNode = graphNodes.get(nodeKey(edgeVersion, toCode));
            if (diseaseNode == null || !"DISEASE".equalsIgnoreCase(String.valueOf(diseaseNode.get("type")))) {
                continue;
            }
            DiseaseAggregate agg = aggregates.get(toCode);
            if (agg == null) {
                agg = new DiseaseAggregate(toCode, String.valueOf(diseaseNode.get("name")));
                aggregates.put(toCode, agg);
            }
            double weight = toDouble(edge.get("weight"), 0.5);
            agg.score += weight * 100;
            Map<String, Object> fromNode = graphNodes.get(nodeKey(edgeVersion, fromCode));
            String fromName = fromNode == null ? fromCode : String.valueOf(fromNode.get("name"));
            agg.relations.add(relation(String.valueOf(edge.get("relation_type")), fromCode, fromName, weight));
        }

        for (DiseaseAggregate agg : aggregates.values()) {
            GraphCandidate candidate = new GraphCandidate();
            candidate.setDiseaseCode(agg.diseaseCode);
            candidate.setDiseaseName(agg.diseaseName);
            candidate.setRawGraphScore(Math.round(agg.score * 100.0) / 100.0);
            candidate.setMatchedRelations(agg.relations);
            candidate.setGraphVersion(graphVersion);
            candidate.setGraphSource("REGISTERED_FALLBACK");
            if (reason != null) {
                candidate.setDegraded(true);
                candidate.setDegradedReason(reason);
            }
            candidates.add(candidate);
        }
        Collections.sort(candidates, new Comparator<GraphCandidate>() {
            @Override
            public int compare(GraphCandidate left, GraphCandidate right) {
                return Double.compare(right.getRawGraphScore(), left.getRawGraphScore());
            }
        });
        return candidates;
    }

    private static class DiseaseAggregate {
        private final String diseaseCode;
        private final String diseaseName;
        private double score;
        private final List<Map<String, Object>> relations = new ArrayList<Map<String, Object>>();

        DiseaseAggregate(String diseaseCode, String diseaseName) {
            this.diseaseCode = diseaseCode;
            this.diseaseName = diseaseName;
        }
    }

    private List<Map<String, Object>> registeredEvidenceFor(String targetCode, String graphVersion) {
        List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
        if (targetCode == null || graphEvidences.isEmpty()) {
            return matched;
        }
        // 优先匹配传入版本，没有时返回所有 targetCode 命中的证据，作为跨版本的兜底。
        List<Map<String, Object>> sameVersion = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> entry : graphEvidences.values()) {
            if (!targetCode.equalsIgnoreCase(String.valueOf(entry.get("target_code")))) {
                continue;
            }
            Map<String, Object> copy = new LinkedHashMap<String, Object>(entry);
            if (graphVersion != null && graphVersion.equalsIgnoreCase(String.valueOf(entry.get("graph_version")))) {
                sameVersion.add(copy);
            } else {
                matched.add(copy);
            }
        }
        if (!sameVersion.isEmpty()) {
            return sameVersion;
        }
        return matched;
    }

    private Map<String, Object> relation(String type, String code, String name, double weight) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        map.put("code", code);
        map.put("name", name);
        map.put("weight", weight);
        return map;
    }

    private Driver newDriver() {
        Config config = Config.builder()
                .withConnectionTimeout(properties.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .withMaxConnectionPoolSize(4)
                .build();
        return GraphDatabase.driver(properties.getUri(),
                AuthTokens.basic(properties.getUsername(), properties.getPassword()), config);
    }

    private SessionConfig sessionConfig() {
        if (hasText(properties.getDatabase())) {
            return SessionConfig.forDatabase(properties.getDatabase());
        }
        return SessionConfig.defaultConfig();
    }

    @SuppressWarnings("unchecked")
    private List<String> factCodes(Map<String, Object> request) {
        List<String> codes = new ArrayList<String>();
        addCodes(codes, request.get("symptom_codes"));
        addCodes(codes, request.get("finding_codes"));
        addCodes(codes, request.get("risk_factor_codes"));
        addCodes(codes, request.get("lab_abnormal_codes"));
        Object patientContext = request.get("patient_context");
        if (patientContext instanceof Map) {
            Map<String, Object> context = (Map<String, Object>) patientContext;
            Object facts = context.get("facts");
            if (facts instanceof Map) {
                Map<String, Object> factMap = (Map<String, Object>) facts;
                addCodes(codes, factMap.get("symptom_codes"));
                addCodes(codes, factMap.get("finding_codes"));
                addCodes(codes, factMap.get("risk_factor_codes"));
                addCodesFromObjects(codes, factMap.get("histories"), "code");
                addFindingCodesFromExams(codes, factMap.get("exams"));
            }
        }
        return codes;
    }

    private void addCodes(List<String> target, Object values) {
        if (values instanceof Iterable) {
            for (Object value : (Iterable<?>) values) {
                addCode(target, value);
            }
        } else {
            addCode(target, values);
        }
    }

    @SuppressWarnings("unchecked")
    private void addCodesFromObjects(List<String> target, Object values, String field) {
        if (values instanceof Iterable) {
            for (Object value : (Iterable<?>) values) {
                if (value instanceof Map) {
                    addCode(target, ((Map<String, Object>) value).get(field));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void addFindingCodesFromExams(List<String> target, Object exams) {
        if (exams instanceof Iterable) {
            for (Object exam : (Iterable<?>) exams) {
                if (exam instanceof Map) {
                    addCodes(target, ((Map<String, Object>) exam).get("finding_codes"));
                }
            }
        }
    }

    private void addCode(List<String> target, Object value) {
        String code = string(value, null);
        if (code != null && !target.contains(code)) {
            target.add(code);
        }
    }

    private List<String> cleanStrings(List<String> values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (hasText(value) && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private String graphVersion(Map<String, Object> request) {
        return string(request.get("graph_version"), string(properties.getDefaultVersion(), "AMI_GRAPH_2026_01"));
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

    private String degradedReason(List<GraphCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0).getDegradedReason();
    }

    private String degradedReasonFromMaps(List<Map<String, Object>> maps) {
        if (maps.isEmpty()) {
            return null;
        }
        Object reason = maps.get(0).get("degraded_reason");
        return reason == null ? null : String.valueOf(reason);
    }

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private Integer integer(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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

    private Map<String, Object> toVersionEntry(Map<String, Object> entry) {
        String graphVersion = requireField(entry, "graph_version");
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("graph_version", graphVersion);
        view.put("name", string(entry.get("name"), graphVersion));
        view.put("status", canonical(string(entry.get("status"), "DRAFT")));
        view.put("description", string(entry.get("description"), null));
        view.put("source_uri", string(entry.get("source_uri"), null));
        view.put("reference_document_code", string(entry.get("reference_document_code"), null));
        view.put("reference_binding_type", string(entry.get("reference_binding_type"), null));
        view.put("published_by", string(entry.get("published_by"), null));
        view.put("published_time", string(entry.get("published_time"), null));
        view.put("created_time", nowText());
        return view;
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

    private double toDouble(Object value, double defaultValue) {
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

    private String versionFamily(String graphVersion) {
        if (graphVersion == null) {
            return "";
        }
        int idx = graphVersion.indexOf("::");
        return idx > 0 ? graphVersion.substring(0, idx) : graphVersion.replaceAll("_[0-9]{4,}_[0-9]{2}.*$", "");
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
}
