package com.medkernel.engine.pathway;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PathwayNodeRequest(
    @NotBlank String nodeCode,
    @NotBlank String name,
    @NotNull PathwayNodeType nodeType,
    @Min(0) Integer sortOrder,
    String responsibleRole,
    JsonNode dependency,
    @Min(0) Integer timeWindowMinutes,
    Boolean terminal,
    JsonNode config
) {}
