package com.medkernel.engine.rule;

import java.util.List;

public record RuleDetailResponse(
    RuleDefinition definition,
    RuleVersion version,
    List<RuleTestCase> testCases
) {
    public RuleDetailResponse {
        testCases = testCases == null ? List.of() : List.copyOf(testCases);
    }
}
