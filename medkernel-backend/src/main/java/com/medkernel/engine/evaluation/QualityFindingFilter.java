package com.medkernel.engine.evaluation;

public record QualityFindingFilter(
    QualityFindingSeverity severity,
    QualityFindingStatus status,
    String responsibleDepartmentId
) {}
