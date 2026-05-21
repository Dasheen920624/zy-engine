package com.medkernel.system;

import com.medkernel.dify.workflow.DifyProperties;
import com.medkernel.graph.GraphProperties;
import com.medkernel.llm.ModelGatewayService;
import com.medkernel.persistence.EnginePersistenceProperties;
import com.medkernel.persistence.EnginePersistenceService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class MedKernelBusinessMetrics {
    private final EnginePersistenceProperties databaseProperties;
    private final EnginePersistenceService persistenceService;
    private final GraphProperties graphProperties;
    private final DifyProperties difyProperties;
    private final ModelGatewayService modelGatewayService;

    public MedKernelBusinessMetrics(MeterRegistry registry,
                                    EnginePersistenceProperties databaseProperties,
                                    EnginePersistenceService persistenceService,
                                    GraphProperties graphProperties,
                                    DifyProperties difyProperties,
                                    ModelGatewayService modelGatewayService) {
        this.databaseProperties = databaseProperties;
        this.persistenceService = persistenceService;
        this.graphProperties = graphProperties;
        this.difyProperties = difyProperties;
        this.modelGatewayService = modelGatewayService;

        registerProviderMetrics(registry);
        registerCapabilityMetrics(registry);
    }

    private void registerProviderMetrics(MeterRegistry registry) {
        registerProviderGauge(registry, "medkernel_provider_configured", "Provider configured flag.",
                "database", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(databaseProperties.isEnabled());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_ready", "Provider readiness flag.",
                "database", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(persistenceService.enabled());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_configured", "Provider configured flag.",
                "graph", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(graphProperties.isEnabled());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_ready", "Provider readiness flag.",
                "graph", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(graphProperties.ready());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_configured", "Provider configured flag.",
                "dify", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(difyProperties.isEnabled());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_ready", "Provider readiness flag.",
                "dify", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(difyProperties.ready());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_configured", "Provider configured flag.",
                "model_gateway", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(modelGatewayService.isEnabled());
                    }
                });
        registerProviderGauge(registry, "medkernel_provider_ready", "Provider readiness flag.",
                "model_gateway", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return one(modelGatewayService.isEnabled() && anyModelProviderReady());
                    }
                });
        registerProviderGauge(registry, "medkernel_model_provider_count", "Registered model provider count.",
                "model_gateway", new Supplier<Number>() {
                    @Override
                    public Number get() {
                        return modelGatewayService.listProviders().size();
                    }
                });
    }

    private void registerCapabilityMetrics(MeterRegistry registry) {
        registerStaticGauge(registry, "pathway_engine", 1.0);
        registerStaticGauge(registry, "rule_engine", 1.0);
        registerStaticGauge(registry, "organization_context", 1.0);
        registerStaticGauge(registry, "graph_fallback", 1.0);
        registerStaticGauge(registry, "dify_fallback", 1.0);
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

    private void registerProviderGauge(MeterRegistry registry,
                                       String metricName,
                                       String description,
                                       String provider,
                                       Supplier<Number> supplier) {
        Gauge.builder(metricName, supplier, value -> value.get().doubleValue())
                .description(description)
                .strongReference(true)
                .tag("provider", provider)
                .register(registry);
    }

    private void registerStaticGauge(MeterRegistry registry, String capability, double value) {
        Gauge.builder("medkernel_capability_enabled", new StaticGaugeValue(value), StaticGaugeValue::get)
                .description("MedKernel product capability flag.")
                .strongReference(true)
                .tag("capability", capability)
                .register(registry);
    }

    private double one(boolean value) {
        return value ? 1.0 : 0.0;
    }

    private static final class StaticGaugeValue {
        private final double value;

        private StaticGaugeValue(double value) {
            this.value = value;
        }

        private double get() {
            return value;
        }
    }
}
