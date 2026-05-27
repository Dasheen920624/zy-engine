package com.medkernel.engine.evaluation;

public record RectificationResponse(
    String taskId,
    QualityFindingStatus findingStatus,
    RectificationTaskStatus taskStatus,
    String traceId
) {}
