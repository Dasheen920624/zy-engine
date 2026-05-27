package com.medkernel.engine.rule;

import java.util.List;

public record RulePublishResponse(
    String ruleId,
    String versionId,
    RuleDefinitionStatus status,
    String traceId,
    List<RuleTestCaseResult> results
) {
    public RulePublishResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }
}
