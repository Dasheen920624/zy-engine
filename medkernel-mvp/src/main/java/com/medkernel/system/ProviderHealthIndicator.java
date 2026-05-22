package com.medkernel.system;

import com.medkernel.dify.workflow.DifyProperties;
import com.medkernel.graph.GraphProperties;
import com.medkernel.llm.ModelGatewayService;
import com.medkernel.persistence.EnginePersistenceService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider 健康指标，集成到 Actuator /actuator/health 端点。
 *
 * 当所有 Provider 就绪时状态为 UP，任一关键 Provider 降级时为 UP with details，
 * 数据库不可用时为 DOWN（数据库是主存储，不可降级）。
 */
@Component
public class ProviderHealthIndicator implements HealthIndicator {

    private final EnginePersistenceService persistenceService;
    private final GraphProperties graphProperties;
    private final DifyProperties difyProperties;
    private final ModelGatewayService modelGatewayService;

    public ProviderHealthIndicator(EnginePersistenceService persistenceService,
                                   GraphProperties graphProperties,
                                   DifyProperties difyProperties,
                                   ModelGatewayService modelGatewayService) {
        this.persistenceService = persistenceService;
        this.graphProperties = graphProperties;
        this.difyProperties = difyProperties;
        this.modelGatewayService = modelGatewayService;
    }

    @Override
    public Health health() {
        boolean databaseReady = persistenceService.enabled();
        boolean graphReady = graphProperties.ready();
        boolean difyReady = difyProperties.ready();
        boolean modelGatewayReady = modelGatewayService.isEnabled() && anyModelProviderReady();

        Health.Builder builder = databaseReady ? Health.up() : Health.down();

        builder.withDetail("database", providerDetail(databaseReady, graphReady, difyReady));
        builder.withDetail("graph", providerDetail(graphReady, graphProperties.isEnabled()));
        builder.withDetail("dify", providerDetail(difyReady, difyProperties.isEnabled()));
        builder.withDetail("model_gateway", providerDetail(modelGatewayReady, modelGatewayService.isEnabled()));

        if (!graphReady || !difyReady || !modelGatewayReady) {
            builder.withDetail("run_mode", runMode(databaseReady, graphReady, difyReady));
        }

        return builder.build();
    }

    private Map<String, Object> providerDetail(boolean ready, boolean configured) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("ready", ready);
        detail.put("configured", configured);
        if (!ready && configured) {
            detail.put("status", "DEGRADED");
        } else if (!ready) {
            detail.put("status", "DISABLED");
        } else {
            detail.put("status", "READY");
        }
        return detail;
    }

    private Map<String, Object> providerDetail(boolean databaseReady, boolean graphReady, boolean difyReady) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("ready", databaseReady);
        detail.put("configured", true);
        detail.put("status", databaseReady ? "READY" : "DOWN");
        return detail;
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

    private boolean anyModelProviderReady() {
        List<Map<String, Object>> providers = modelGatewayService.listProviders();
        for (Map<String, Object> provider : providers) {
            if (Boolean.TRUE.equals(provider.get("ready"))) {
                return true;
            }
        }
        return false;
    }
}
