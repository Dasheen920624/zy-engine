package com.medkernel.engine.pathway;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PathwayEdgeRequest(
    @NotBlank String edgeCode,
    @NotBlank String fromNodeCode,
    @NotBlank String toNodeCode,
    @NotNull PathwayEdgeType edgeType,
    JsonNode condition,
    @Min(0) Integer priority
) {}
