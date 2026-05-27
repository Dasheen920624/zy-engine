package com.medkernel.engine.rule;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleEvaluateRequest(
    @NotBlank String triggerPoint,
    @NotNull JsonNode context,
    String eventId,
    List<String> ruleIds
) {
    public RuleEvaluateRequest {
        ruleIds = ruleIds == null ? List.of() : List.copyOf(ruleIds);
    }
}
