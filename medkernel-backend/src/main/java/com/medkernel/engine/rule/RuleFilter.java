package com.medkernel.engine.rule;

public record RuleFilter(
    RuleDefinitionStatus status,
    RuleType ruleType,
    RuleRiskLevel riskLevel
) {}
