package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

/**
 * 规则试运行入参（GA-ENG-API-05 {@code POST /api/v1/engine/rules/{ruleId}/simulate}）：仅需提供上下文 JSON。
 */
public record RuleSimulateRequest(
    @NotNull JsonNode context
) {}
