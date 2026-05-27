package com.medkernel.engine.evaluation;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationResultRequest(
    @NotBlank String indicatorId,
    @NotNull EvaluationSubjectType subjectType,
    @NotBlank String subjectRefId,
    BigDecimal scoreValue,
    @NotNull EvaluationResultLevel resultLevel,
    boolean hitFlag,
    @NotBlank String evidenceSummary,
    String sourceRef,
    String responsibleDepartmentId,
    List<@Valid QualityFindingRequest> findings
) {}
