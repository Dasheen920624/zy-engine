package com.medkernel.engine.rule;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;

public record RuleSimulateRequest(
    @NotNull JsonNode context
) {}
