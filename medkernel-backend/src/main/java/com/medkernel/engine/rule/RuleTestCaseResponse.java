package com.medkernel.engine.rule;

public record RuleTestCaseResponse(
    String caseId,
    RuleTestCaseType caseType,
    RuleTestCaseStatus lastStatus
) {}
