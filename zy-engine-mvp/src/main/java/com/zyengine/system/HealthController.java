package com.zyengine.system;

import com.zyengine.common.ApiResult;
import com.zyengine.dify.DifyProperties;
import com.zyengine.graph.GraphProperties;
import com.zyengine.persistence.EnginePersistenceProperties;
import com.zyengine.persistence.EnginePersistenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    private final EnginePersistenceProperties databaseProperties;
    private final EnginePersistenceService persistenceService;
    private final GraphProperties graphProperties;
    private final DifyProperties difyProperties;

    public HealthController(EnginePersistenceProperties databaseProperties,
                            EnginePersistenceService persistenceService,
                            GraphProperties graphProperties,
                            DifyProperties difyProperties) {
        this.databaseProperties = databaseProperties;
        this.persistenceService = persistenceService;
        this.graphProperties = graphProperties;
        this.difyProperties = difyProperties;
    }

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("service", "zy-engine-mvp");
        data.put("status", "UP");
        data.put("jdk", "1.8-compatible");
        return ApiResult.success(data);
    }

    @GetMapping("/system/providers")
    public ApiResult<Map<String, Object>> providers() {
        boolean databaseReady = persistenceService.enabled();
        boolean graphReady = graphProperties.ready();
        boolean difyReady = difyProperties.ready();

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("service", "zy-engine-mvp");
        data.put("run_mode", runMode(databaseReady, graphReady, difyReady));
        data.put("db_only_supported", true);
        data.put("external_graph_required", false);
        data.put("external_dify_required", false);

        Map<String, Object> providers = new LinkedHashMap<String, Object>();
        providers.put("database", databaseProvider(databaseReady));
        providers.put("graph", graphProvider(graphReady));
        providers.put("dify", difyProvider(difyReady));
        data.put("providers", providers);

        Map<String, Object> capabilities = new LinkedHashMap<String, Object>();
        capabilities.put("pathway_engine", true);
        capabilities.put("rule_engine", true);
        capabilities.put("organization_context", true);
        capabilities.put("organization_directory", true);
        capabilities.put("graph_fallback", !graphReady);
        capabilities.put("dify_fallback", !difyReady);
        capabilities.put("config_primary_store", "ORACLE");
        capabilities.put("local_development_store", "LOCAL_H2_FILE");
        capabilities.put("oracle_schema_required", true);
        data.put("capabilities", capabilities);
        return ApiResult.success(data);
    }

    private Map<String, Object> databaseProvider(boolean ready) {
        Map<String, Object> provider = new LinkedHashMap<String, Object>();
        provider.put("role", "CONFIG_PRIMARY_STORE");
        provider.put("configured", databaseProperties.isEnabled());
        provider.put("ready", ready);
        provider.put("status", ready ? "READY" : "DISABLED");
        // database_role 是契约字段（测试与运维都依赖），保留；具体方言/驱动细节不暴露。
        provider.put("database_role", databaseProperties.roleName());
        provider.put("provider", persistenceService.providerName());
        provider.put("production_ready", ready && databaseProperties.productionAuthority());
        // 隐藏 schema_init / dialect / production_authority 等部署细节，避免暴露内部基础设施拓扑；
        // 仅在 degraded 时给出通用 reason，不再回显具体的中文失败描述（避免帮助攻击者识别状态）。
        provider.put("degraded_reason", ready ? null : "DB_PROVIDER_UNAVAILABLE");
        return provider;
    }

    private Map<String, Object> graphProvider(boolean ready) {
        Map<String, Object> provider = new LinkedHashMap<String, Object>();
        provider.put("role", "GRAPH_QUERY_PROVIDER");
        provider.put("configured", graphProperties.isEnabled());
        provider.put("ready", ready);
        provider.put("status", ready ? "READY" : fallbackStatus(graphProperties.isEnabled()));
        provider.put("provider", ready ? "NEO4J" : "DATABASE_OR_HEURISTIC_FALLBACK");
        provider.put("default_version", text(graphProperties.getDefaultVersion(), "AMI_GRAPH_2026_01"));
        provider.put("degraded_reason", ready ? null : "GRAPH_PROVIDER_UNAVAILABLE");
        return provider;
    }

    private Map<String, Object> difyProvider(boolean ready) {
        Map<String, Object> provider = new LinkedHashMap<String, Object>();
        provider.put("role", "WORKFLOW_PROVIDER");
        provider.put("configured", difyProperties.isEnabled());
        provider.put("ready", ready);
        provider.put("status", ready ? "READY" : fallbackStatus(difyProperties.isEnabled()));
        provider.put("provider", ready ? "DIFY" : "LOCAL_TEMPLATE_FALLBACK");
        // 不再暴露具体 timeout_ms 数值（可被用于猜测服务负载/超时调优策略）。
        provider.put("degraded_reason", ready ? null : "DIFY_PROVIDER_UNAVAILABLE");
        return provider;
    }

    private String runMode(boolean databaseReady, boolean graphReady, boolean difyReady) {
        if (databaseReady && graphReady && difyReady) {
            return "FULL_INTEGRATION";
        }
        if (databaseReady && (graphReady || difyReady)) {
            return "HYBRID";
        }
        if (databaseReady) {
            return "DB_ONLY";
        }
        return "IN_MEMORY_DEMO";
    }

    private String fallbackStatus(boolean configured) {
        return configured ? "MISCONFIGURED" : "FALLBACK";
    }

    private String graphReason() {
        return graphProperties.isEnabled()
                ? "Neo4j已启用但连接参数不完整，图谱查询将回退到数据库/内置启发式。"
                : "Neo4j未启用，图谱查询将回退到数据库/内置启发式。";
    }

    private String difyReason() {
        return difyProperties.isEnabled()
                ? "Dify已启用但连接参数不完整，工作流调用将使用本地模板降级。"
                : "Dify未启用，工作流调用将使用本地模板降级。";
    }

    private String text(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
