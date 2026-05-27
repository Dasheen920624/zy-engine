package com.medkernel.engine.rule;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 规则真实执行入参（GA-ENG-API-05 {@code POST /api/v1/engine/rules/evaluate}）。
 *
 * <p>{@code triggerPoint} 与 DSL 中 {@code trigger} 匹配的已发布规则参与本次评估；
 * {@code ruleIds} 留空表示用全租户已发布规则集合，否则限定到给定规则列表。
 */
public record RuleEvaluateRequest(
    @NotBlank String triggerPoint,
    @NotNull JsonNode context,
    String eventId,
    List<String> ruleIds
) {
    public RuleEvaluateRequest {
        ruleIds = ruleIds == null ? List.of() : List.copyOf(ruleIds);
    }
}
