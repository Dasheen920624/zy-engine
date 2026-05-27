package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record RuleTestCaseRequest(
    @NotNull RuleTestCaseType caseType,
    @NotNull JsonNode inputPayload,
    boolean expectedHit,
    RuleRiskLevel expectedSeverity,
    String expectedActionCode
) {}
