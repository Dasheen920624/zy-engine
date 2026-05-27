package com.medkernel.engine.evaluation;

public record RectificationReviewResponse(
    String reviewId,
    QualityFindingStatus findingStatus,
    RectificationTaskStatus taskStatus,
    String traceId
) {}
