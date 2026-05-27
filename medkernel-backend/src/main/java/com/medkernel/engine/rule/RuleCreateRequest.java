package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleCreateRequest(
    @NotBlank String ruleCode,
    @NotBlank String name,
    @NotNull RuleType ruleType,
    RuleAuthoringMode authoringMode,
    RuleRiskLevel riskLevel,
    String packageVersion,
    String applicableOrgUnitId,
    @NotBlank String sourceRef,
    String changeSummary,
    @NotNull JsonNode dsl,
    JsonNode explanation
) {}
