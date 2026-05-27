package com.medkernel.engine.evaluation;

/**
 * 整改复核响应。
 *
 * <p>返回复核记录 ID、问题状态、整改任务状态和 traceId；幂等重放时返回首次成功复核结果。
 */
public record RectificationReviewResponse(
    String reviewId,
    QualityFindingStatus findingStatus,
    RectificationTaskStatus taskStatus,
    String traceId
) {}
