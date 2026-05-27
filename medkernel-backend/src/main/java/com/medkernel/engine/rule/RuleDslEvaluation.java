package com.medkernel.engine.rule;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record RuleDslEvaluation(
    boolean hit,
    RuleRiskLevel severity,
    List<RuleActionResult> actions,
    JsonNode explanation
) {
    public RuleDslEvaluation {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
