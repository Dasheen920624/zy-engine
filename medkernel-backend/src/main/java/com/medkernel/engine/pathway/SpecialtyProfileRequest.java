package com.medkernel.engine.pathway;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;

public record SpecialtyProfileRequest(
    @NotBlank String profileCode,
    @NotBlank String name,
    JsonNode stratification,
    JsonNode entryCriteria,
    JsonNode exitCriteria,
    JsonNode followupPlan
) {}
