package com.medkernel.engine.evaluation;

/**
 * 整改提交响应。
 *
 * <p>返回整改任务 ID、问题状态、任务状态和 traceId；幂等重放时返回首次成功写入的同一组状态。
 */
public record RectificationResponse(
    String taskId,
    QualityFindingStatus findingStatus,
    RectificationTaskStatus taskStatus,
    String traceId
) {}
