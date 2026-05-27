package com.medkernel.engine.rule;

public record RuleTestCaseResult(
    String caseId,
    RuleTestCaseType caseType,
    boolean expectedHit,
    boolean actualHit,
    RuleRiskLevel expectedSeverity,
    RuleRiskLevel actualSeverity,
    RuleTestCaseStatus status,
    String message
) {}
