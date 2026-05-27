package com.medkernel.engine.rule;

public record RuleCreateResponse(
    String ruleId,
    String versionId,
    RuleDefinitionStatus status,
    String traceId
) {}
