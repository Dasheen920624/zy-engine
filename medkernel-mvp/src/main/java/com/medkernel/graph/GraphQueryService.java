package com.medkernel.graph;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
@Service
public class GraphQueryService {
    private final GraphProperties properties;
    public GraphQueryService(GraphProperties properties) {
        this.properties = properties;
    }
    public GraphCandidateQueryResult diseaseCandidates(Map<String, Object> request,
                                                  Map<String, Map<String, Object>> graphNodes,
                                                  List<Map<String, Object>> graphEdges) {
        return diseaseCandidates(request, graphNodes, graphEdges, null);
    }
    public GraphCandidateQueryResult diseaseCandidates(Map<String, Object> request,
                                                  Map<String, Map<String, Object>> graphNodes,
                                                  List<Map<String, Object>> graphEdges,
                                                  String tenantId) {
        String graphVersion = graphVersion(request);
        try {
            if (properties.ready()) {
                List<GraphCandidate> candidates = queryNeo4jCandidates(request, graphVersion);
                return new GraphCandidateQueryResult(candidates, graphVersion, "NEO4J", false, null);
            }
            List<GraphCandidate> candidates = fallbackCandidates(request, graphVersion,
                    properties.isEnabled() ? "Neo4j配置不完整，已使用内置AMI图谱降级召回。" : null,
                    graphNodes, graphEdges, tenantId);
            return new GraphCandidateQueryResult(candidates, graphVersion, "FALLBACK_HEURISTIC",
                    properties.isEnabled(), degradedReason(candidates));
        } catch (RuntimeException ex) {
            List<GraphCandidate> candidates = fallbackCandidates(request, graphVersion,
                    "Neo4j查询不可用，已使用内置AMI图谱降级召回：" + ex.getClass().getSimpleName(),
                    graphNodes, graphEdges, tenantId);
            return new GraphCandidateQueryResult(candidates, graphVersion, "FALLBACK_HEURISTIC",
                    true, degradedReason(candidates));
        }
    }
    public GraphEvidenceQueryResult evidence(Map<String, Object> request,
                                        Map<String, Map<String, Object>> graphEvidences) {
        return evidence(request, graphEvidences, null);
    }
    public GraphEvidenceQueryResult evidence(Map<String, Object> request,
                                        Map<String, Map<String, Object>> graphEvidences,
                                        String tenantId) {
        String targetCode = string(request.get("target_code"), null);
        String graphVersion = graphVersion(request);
        try {
            if (properties.ready()) {
                List<Map<String, Object>> evidence = queryNeo4jEvidence(targetCode, graphVersion);
                return new GraphEvidenceQueryResult(evidence, targetCode, graphVersion, "NEO4J", false, null);
            }
            List<Map<String, Object>> evidence = fallbackEvidence(targetCode, graphVersion,
                    properties.isEnabled() ? "Neo4j配置不完整，已使用内置AMI证据降级返回。" : null,
                    graphEvidences, tenantId);
            return new GraphEvidenceQueryResult(evidence, targetCode, graphVersion, "FALLBACK_HEURISTIC",
                    properties.isEnabled(), degradedReasonFromMaps(evidence));
        } catch (RuntimeException ex) {
            List<Map<String, Object>> evidence = fallbackEvidence(targetCode, graphVersion,
                    "Neo4j证据查询不可用，已使用内置AMI证据降级返回：" + ex.getClass().getSimpleName(),
                    graphEvidences, tenantId);
            return new GraphEvidenceQueryResult(evidence, targetCode, graphVersion, "FALLBACK_HEURISTIC",
                    true, degradedReasonFromMaps(evidence));
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
        private List<GraphCandidate> fallbackCandidates(Map<String, Object> request, String graphVersion, String reason,
                                                      Map<String, Map<String, Object>> graphNodes,
                                                      List<Map<String, Object>> graphEdges,
                                                      String tenantId) {
            List<String> codes = factCodes(request);
            // 已注册图谱：用 imported edges 召回 Disease，得到任何 Disease 即采用导入数据；
            // 否则回退到内置 AMI 启发式，保持向后兼容。
            List<GraphCandidate> registered = registeredCandidatesFor(codes, graphVersion, reason, graphNodes, graphEdges,
                    tenantId);
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
        private List<Map<String, Object>> fallbackEvidence(String targetCode, String graphVersion, String reason,
                                                         Map<String, Map<String, Object>> graphEvidences,
                                                         String tenantId) {
            // 优先使用已注册的图谱证据：targetCode 匹配时返回所有匹配项（不限版本），让用户可以通过 import 配置扩展演示数据。
            List<Map<String, Object>> registered = registeredEvidenceFor(targetCode, graphVersion, graphEvidences,
                    tenantId);
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
        private List<GraphCandidate> registeredCandidatesFor(List<String> factCodes, String graphVersion, String reason,
                                                           Map<String, Map<String, Object>> graphNodes,
                                                           List<Map<String, Object>> graphEdges,
                                                           String tenantId) {
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
                if (!matchesTenant(edge, tenantId)) {
                    continue;
                }
                String edgeVersion = String.valueOf(edge.get("graph_version"));
                if (graphVersion != null && !graphVersion.equalsIgnoreCase(edgeVersion)) {
                    continue;
                }
                String fromCode = String.valueOf(edge.get("from_code"));
                if (!factCodes.contains(fromCode)) {
                    continue;
                }
                String toCode = String.valueOf(edge.get("to_code"));
                Map<String, Object> diseaseNode = findNode(graphNodes, tenantId, string(edge.get("tenant_id"), null),
                        edgeVersion, toCode);
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
                Map<String, Object> fromNode = findNode(graphNodes, tenantId, string(edge.get("tenant_id"), null),
                        edgeVersion, fromCode);
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
        private List<Map<String, Object>> registeredEvidenceFor(String targetCode, String graphVersion,
                                                              Map<String, Map<String, Object>> graphEvidences,
                                                              String tenantId) {
            List<Map<String, Object>> matched = new ArrayList<Map<String, Object>>();
            if (targetCode == null || graphEvidences.isEmpty()) {
                return matched;
            }
            // 优先匹配传入版本，没有时返回所有 targetCode 命中的证据，作为跨版本的兜底。
            List<Map<String, Object>> sameVersion = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> entry : graphEvidences.values()) {
                if (!matchesTenant(entry, tenantId)) {
                    continue;
                }
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
        private Map<String, Object> findNode(Map<String, Map<String, Object>> graphNodes, String tenantId,
                                             String entryTenantId, String graphVersion, String code) {
            Map<String, Object> node = graphNodes.get(tenantNodeKey(tenantId, graphVersion, code));
            if (node != null) {
                return node;
            }
            if (entryTenantId != null && !entryTenantId.equals(tenantId)) {
                node = graphNodes.get(tenantNodeKey(entryTenantId, graphVersion, code));
                if (node != null) {
                    return node;
                }
            }
            return graphNodes.get(legacyNodeKey(graphVersion, code));
        }
        private boolean matchesTenant(Map<String, Object> entry, String tenantId) {
            if (!hasText(tenantId)) {
                return true;
            }
            String entryTenant = string(entry.get("tenant_id"), null);
            return entryTenant == null || tenantId.equals(entryTenant);
        }
        private String tenantNodeKey(String tenantId, String graphVersion, String code) {
            return string(tenantId, "") + "::" + string(graphVersion, "") + "::" + string(code, "");
        }
        private String legacyNodeKey(String graphVersion, String code) {
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
}
