package com.medkernel.engine.pathway;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PathwayTemplateCreateRequest(
    @NotBlank String packageId,
    @NotBlank String templateCode,
    @NotBlank String name,
    @NotBlank String diseaseCode,
    @NotNull Integer templateVersion,
    @NotNull PathwayTemplateLevel templateLevel,
    @NotBlank String startNodeCode,
    @NotBlank String sourceRef,
    String description,
    JsonNode entryCriteria,
    JsonNode exitCriteria,
    @NotEmpty List<@Valid PathwayNodeRequest> nodes,
    List<@Valid PathwayEdgeRequest> edges,
    List<@Valid SpecialtyMetricBindingRequest> metricBindings
) {}
