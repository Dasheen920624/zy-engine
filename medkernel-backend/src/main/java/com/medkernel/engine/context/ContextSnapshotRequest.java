package com.medkernel.engine.context;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/v1/engine/context/snapshots 请求体。
 */
public record ContextSnapshotRequest(
    @NotBlank String patientId,
    String encounterId,
    @NotBlank String orgUnitId,
    @NotBlank String knowledgePackageVersion,
    @NotBlank String rulePackageVersion,
    @NotBlank String pathwayPackageVersion,
    @NotNull @Valid ContextSnapshotResources resources
) {}
