package com.medkernel.graph;

import com.medkernel.persistence.EnginePersistenceService;
import com.medkernel.persistence.Ids;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * GRAPH-005: Neo4j 同步服务（dry-run + 重试）。
 * 从 GraphService 拆分，满足单文件 ≤600 行规范。
 */
@Service
public class GraphSyncService {
    private final GraphProperties properties;
    private final GraphService graphService;
    private final EnginePersistenceService persistenceService;

    public GraphSyncService(GraphProperties properties, GraphService graphService,
                            EnginePersistenceService persistenceService) {
        this.properties = properties;
        this.graphService = graphService;
        this.persistenceService = persistenceService;
    }

    /**
     * 将内存中的图谱数据同步到 Neo4j。
     *
     * @param graphVersion 图谱版本号（同步该版本下的节点和边）
     * @param dryRun       true 时仅预览同步结果，不实际写入 Neo4j
     * @param triggeredBy  触发人
     * @return 同步任务结果
     */
    public GraphSyncTask syncToNeo4j(String graphVersion, boolean dryRun, String triggeredBy) {
        if (!properties.ready()) {
            throw new IllegalStateException("Neo4j 配置不完整，无法执行同步。请检查 medkernel.graph.uri/username/password 配置。");
        }
        if (!hasText(graphVersion)) {
            graphVersion = properties.getDefaultVersion();
        }

        GraphSyncTask task = new GraphSyncTask();
        task.setId(Ids.next());
        task.setTenantId("default");
        task.setTaskCode("GRAPH_SYNC-" + task.getId());
        task.setTaskType("GRAPH_SYNC");
        task.setTargetSystem("NEO4J");
        task.setTargetVersion(graphVersion);
        task.setDryRun(dryRun);
        task.setStatus("RUNNING");
        task.setTriggeredBy(triggeredBy != null ? triggeredBy : "SYSTEM");
        task.setStartedTime(LocalDateTime.now());
        task.setRetryCount(0);
        task.setMaxRetries(3);

        // 从 GraphService 获取待同步数据
        List<Map<String, Object>> nodesToSync = graphService.getNodesByVersion(graphVersion);
        List<Map<String, Object>> edgesToSync = graphService.getEdgesByVersion(graphVersion);
        task.setTotalCount(nodesToSync.size() + edgesToSync.size());

        List<GraphSyncDetail> details = new ArrayList<GraphSyncDetail>();
        long startMs = System.currentTimeMillis();

        try (Driver driver = newDriver();
             Session session = driver.session(sessionConfig())) {

            // 同步节点
            for (Map<String, Object> node : nodesToSync) {
                GraphSyncDetail detail = syncNode(session, node, dryRun);
                details.add(detail);
                if ("SUCCESS".equals(detail.getStatus())) {
                    task.setSuccessCount(task.getSuccessCount() + 1);
                } else if ("SKIPPED".equals(detail.getStatus())) {
                    task.setSkipCount(task.getSkipCount() + 1);
                } else {
                    task.setFailedCount(task.getFailedCount() + 1);
                }
            }

            // 同步边
            for (Map<String, Object> edge : edgesToSync) {
                GraphSyncDetail detail = syncEdge(session, edge, dryRun);
                details.add(detail);
                if ("SUCCESS".equals(detail.getStatus())) {
                    task.setSuccessCount(task.getSuccessCount() + 1);
                } else if ("SKIPPED".equals(detail.getStatus())) {
                    task.setSkipCount(task.getSkipCount() + 1);
                } else {
                    task.setFailedCount(task.getFailedCount() + 1);
                }
            }

            task.setStatus(task.getFailedCount() > 0 ? "FAILED" : "SUCCESS");
        } catch (RuntimeException ex) {
            task.setStatus("FAILED");
            task.setErrorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        task.setFinishedTime(LocalDateTime.now());
        task.setDurationMs(System.currentTimeMillis() - startMs);
        task.setDetailJson(buildDetailJson(details));

        // 审计日志
        auditSync(task);

        return task;
    }

    /**
     * 重试失败的同步任务。
     *
     * @param taskCode    原任务编码
     * @param dryRun      true 时仅预览
     * @param triggeredBy 触发人
     * @return 重试后的同步任务结果
     */
    public GraphSyncTask retrySync(String taskCode, boolean dryRun, String triggeredBy) {
        // 由于当前为内存态，无法从 DB 加载历史任务，直接抛出提示
        throw new UnsupportedOperationException(
                "同步任务重试需要 OPS_SYNC_TASK 持久化支持（OPS-002）。当前为内存态，请直接重新执行 syncToNeo4j。");
    }

    /**
     * 列出同步任务（内存态仅返回最近的同步记录）。
     */
    public List<Map<String, Object>> listSyncTasks(Map<String, String> filters) {
        // 内存态无持久化，返回空列表；待 OPS-002 接通后从 DB 查询
        return new ArrayList<Map<String, Object>>();
    }

    /**
     * 获取同步任务详情。
     */
    public Map<String, Object> getSyncTask(String taskCode) {
        // 内存态无持久化，抛出提示
        throw new UnsupportedOperationException(
                "同步任务查询需要 OPS_SYNC_TASK 持久化支持（OPS-002）。");
    }

    private GraphSyncDetail syncNode(Session session, Map<String, Object> node, boolean dryRun) {
        GraphSyncDetail detail = new GraphSyncDetail();
        detail.setId(Ids.next());
        detail.setTenantId("default");
        detail.setItemType("NODE");
        detail.setItemCode(string(node.get("code"), ""));
        detail.setOperation("CREATE");
        detail.setCreatedTime(LocalDateTime.now());

        if (dryRun) {
            detail.setStatus("SKIPPED");
            return detail;
        }

        String cypher = "MERGE (n:GraphNode {code: $code, graph_version: $graph_version}) " +
                "SET n.name = $name, n.type = $type, n.description = $description, n.source = $source, " +
                "n.updated_time = datetime() " +
                "RETURN id(n) AS neo4j_id";

        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("code", node.get("code"));
            params.put("graph_version", node.get("graph_version"));
            params.put("name", node.get("name"));
            params.put("type", node.get("type"));
            params.put("description", node.get("description"));
            params.put("source", node.get("source"));

            Result result = session.run(cypher, Values.value(params));
            if (result.hasNext()) {
                Record record = result.next();
                detail.setNeo4jNodeId(String.valueOf(record.get("neo4j_id").asLong()));
                detail.setStatus("SUCCESS");
            } else {
                detail.setStatus("FAILED");
                detail.setErrorMessage("Neo4j MERGE 未返回结果");
            }
        } catch (RuntimeException ex) {
            detail.setStatus("FAILED");
            detail.setErrorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        return detail;
    }

    private GraphSyncDetail syncEdge(Session session, Map<String, Object> edge, boolean dryRun) {
        GraphSyncDetail detail = new GraphSyncDetail();
        detail.setId(Ids.next());
        detail.setTenantId("default");
        detail.setItemType("EDGE");
        detail.setItemCode(string(edge.get("from_code"), "") + "->" + string(edge.get("to_code"), ""));
        detail.setOperation("CREATE");
        detail.setCreatedTime(LocalDateTime.now());

        if (dryRun) {
            detail.setStatus("SKIPPED");
            return detail;
        }

        String relationType = canonical(string(edge.get("relation_type"), "RELATED_TO"));
        String cypher = "MATCH (a:GraphNode {code: $from_code, graph_version: $graph_version}) " +
                "MATCH (b:GraphNode {code: $to_code, graph_version: $graph_version}) " +
                "MERGE (a)-[r:" + relationType + "]->(b) " +
                "SET r.weight = $weight, r.description = $description, r.updated_time = datetime() " +
                "RETURN id(r) AS neo4j_id";

        try {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("from_code", edge.get("from_code"));
            params.put("to_code", edge.get("to_code"));
            params.put("graph_version", edge.get("graph_version"));
            params.put("weight", edge.get("weight"));
            params.put("description", edge.get("description"));

            Result result = session.run(cypher, Values.value(params));
            if (result.hasNext()) {
                Record record = result.next();
                detail.setNeo4jNodeId(String.valueOf(record.get("neo4j_id").asLong()));
                detail.setStatus("SUCCESS");
            } else {
                detail.setStatus("FAILED");
                detail.setErrorMessage("Neo4j MERGE 未返回结果（节点可能不存在）");
            }
        } catch (RuntimeException ex) {
            detail.setStatus("FAILED");
            detail.setErrorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        return detail;
    }

    private String buildDetailJson(List<GraphSyncDetail> details) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < details.size(); i++) {
            GraphSyncDetail d = details.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"item_type\":\"").append(d.getItemType())
              .append("\",\"item_code\":\"").append(d.getItemCode())
              .append("\",\"operation\":\"").append(d.getOperation())
              .append("\",\"status\":\"").append(d.getStatus());
            if (d.getErrorMessage() != null) {
                sb.append("\",\"error_message\":\"").append(d.getErrorMessage().replace("\"", "\\\""));
            }
            sb.append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }

    private void auditSync(GraphSyncTask task) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("task_code", task.getTaskCode());
        detail.put("target_system", task.getTargetSystem());
        detail.put("target_version", task.getTargetVersion());
        detail.put("dry_run", task.isDryRun());
        detail.put("status", task.getStatus());
        detail.put("total_count", task.getTotalCount());
        detail.put("success_count", task.getSuccessCount());
        detail.put("failed_count", task.getFailedCount());
        detail.put("skip_count", task.getSkipCount());
        detail.put("duration_ms", task.getDurationMs());
        detail.put("error_message", task.getErrorMessage());
        try {
            persistenceService.saveAuditLog("GRAPH", "SYNC_TO_NEO4J", "SyncTask",
                    task.getTaskCode(), null, null, task.getTriggeredBy(), detail);
        } catch (RuntimeException ignored) {
            // 审计写入失败不应影响同步返回
        }
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

    private String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String canonical(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
