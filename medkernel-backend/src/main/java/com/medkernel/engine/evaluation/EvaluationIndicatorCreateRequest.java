package com.medkernel.engine.evaluation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationIndicatorCreateRequest(
    @NotBlank String indicatorCode,
    @Min(1) int versionNo,
    @NotBlank String name,
    @NotNull EvaluationSubjectType subjectType,
    @NotBlank String denominatorDefinition,
    @NotBlank String numeratorDefinition,
    String exclusionDefinition,
    String scoringDefinition,
    @NotBlank String timeWindow,
    @NotBlank String organizationScope,
    @NotBlank String responsibleDepartmentId,
    @NotBlank String sourceRef,
    String packageVersion
) {}
