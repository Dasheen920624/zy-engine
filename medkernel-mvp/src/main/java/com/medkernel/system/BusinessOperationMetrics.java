package com.medkernel.system;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 业务操作指标，为关键业务操作提供 Timer 和 Counter。
 *
 * 使用方式：在 Service 层注入 BusinessOperationMetrics，
 * 调用 recordRuleEvaluation / recordPathwayOperation / recordLlmCall 等方法。
 */
@Component
public class BusinessOperationMetrics {

    private final Timer ruleEvaluationTimer;
    private final Counter ruleEvaluationCounter;
    private final Counter ruleHitCounter;

    private final Timer pathwayOperationTimer;
    private final Counter pathwayOperationCounter;

    private final Timer llmCallTimer;
    private final Counter llmCallCounter;
    private final Counter llmCallErrorCounter;

    private final Counter qualityAlertCounter;
    private final Counter cdssOverrideCounter;

    public BusinessOperationMetrics(MeterRegistry registry) {
        this.ruleEvaluationTimer = Timer.builder("medkernel_rule_evaluation_duration")
                .description("Rule evaluation execution time")
                .tag("component", "rule_engine")
                .register(registry);

        this.ruleEvaluationCounter = Counter.builder("medkernel_rule_evaluation_total")
                .description("Total rule evaluations")
                .tag("component", "rule_engine")
                .register(registry);

        this.ruleHitCounter = Counter.builder("medkernel_rule_hit_total")
                .description("Total rule hits")
                .tag("component", "rule_engine")
                .register(registry);

        this.pathwayOperationTimer = Timer.builder("medkernel_pathway_operation_duration")
                .description("Pathway operation execution time")
                .tag("component", "pathway_engine")
                .register(registry);

        this.pathwayOperationCounter = Counter.builder("medkernel_pathway_operation_total")
                .description("Total pathway operations")
                .tag("component", "pathway_engine")
                .register(registry);

        this.llmCallTimer = Timer.builder("medkernel_llm_call_duration")
                .description("LLM call execution time")
                .tag("component", "model_gateway")
                .register(registry);

        this.llmCallCounter = Counter.builder("medkernel_llm_call_total")
                .description("Total LLM calls")
                .tag("component", "model_gateway")
                .register(registry);

        this.llmCallErrorCounter = Counter.builder("medkernel_llm_call_errors_total")
                .description("Total LLM call errors")
                .tag("component", "model_gateway")
                .register(registry);

        this.qualityAlertCounter = Counter.builder("medkernel_quality_alert_total")
                .description("Total quality alerts")
                .tag("component", "quality")
                .register(registry);

        this.cdssOverrideCounter = Counter.builder("medkernel_cdss_override_total")
                .description("Total CDSS overrides")
                .tag("component", "cdss")
                .register(registry);
    }

    public void recordRuleEvaluation(long durationNanos, boolean hit) {
        ruleEvaluationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        ruleEvaluationCounter.increment();
        if (hit) {
            ruleHitCounter.increment();
        }
    }

    public void recordPathwayOperation(long durationNanos) {
        pathwayOperationTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        pathwayOperationCounter.increment();
    }

    public void recordLlmCall(long durationNanos, boolean error) {
        llmCallTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        llmCallCounter.increment();
        if (error) {
            llmCallErrorCounter.increment();
        }
    }

    public void recordQualityAlert() {
        qualityAlertCounter.increment();
    }

    public void recordCdssOverride() {
        cdssOverrideCounter.increment();
    }
}
