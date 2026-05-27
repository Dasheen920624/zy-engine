package com.medkernel.engine.rule;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record RuleEvaluationItem(
    String executionId,
    String ruleId,
    String versionId,
    boolean hit,
    RuleRiskLevel severity,
    List<RuleActionResult> actions,
    JsonNode explanation
) {
    public RuleEvaluationItem {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
