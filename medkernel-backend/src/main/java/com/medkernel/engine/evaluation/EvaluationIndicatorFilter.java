package com.medkernel.engine.evaluation;

public record EvaluationIndicatorFilter(
    EvaluationIndicatorStatus status,
    EvaluationSubjectType subjectType,
    String indicatorCode
) {}
