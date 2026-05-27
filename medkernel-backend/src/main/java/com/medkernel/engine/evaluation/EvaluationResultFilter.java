package com.medkernel.engine.evaluation;

public record EvaluationResultFilter(
    String indicatorCode,
    EvaluationResultLevel resultLevel,
    String responsibleDepartmentId
) {}
