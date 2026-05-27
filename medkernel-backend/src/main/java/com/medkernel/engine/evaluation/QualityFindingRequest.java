package com.medkernel.engine.evaluation;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QualityFindingRequest(
    @NotBlank String findingCode,
    @NotBlank String title,
    @NotBlank String description,
    @NotNull QualityFindingSeverity severity,
    @NotBlank String evidenceSummary,
    String responsibleDepartmentId,
    Instant dueAt,
    String assigneeUserId
) {}
