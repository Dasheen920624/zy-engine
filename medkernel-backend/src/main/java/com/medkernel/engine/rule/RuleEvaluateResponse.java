package com.medkernel.engine.rule;

import java.util.List;

public record RuleEvaluateResponse(
    String requestId,
    List<RuleEvaluationItem> items,
    RuleRiskLevel highestSeverity,
    String traceId
) {
    public RuleEvaluateResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
