package com.zyengine.graph;

import com.zyengine.persistence.EnginePersistenceService;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class GraphService {
    private final GraphProperties properties;
    private final EnginePersistenceService persistenceService;

    public GraphService(GraphProperties properties, EnginePersistenceService persistenceService) {
        this.properties = properties;
        this.persistenceService = persistenceService;
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

        Driver driver = newDriver();
        try {
            Session session = driver.session(sessionConfig());
            try {
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
            } finally {
                session.close();
            }
        } finally {
            driver.close();
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
        Driver driver = newDriver();
        try {
            Session session = driver.session(sessionConfig());
            try {
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
            } finally {
                session.close();
            }
        } finally {
            driver.close();
        }
    }

    private List<GraphCandidate> fallbackCandidates(Map<String, Object> request, String graphVersion, String reason) {
        List<GraphCandidate> candidates = new ArrayList<GraphCandidate>();
        List<String> codes = factCodes(request);
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
}
