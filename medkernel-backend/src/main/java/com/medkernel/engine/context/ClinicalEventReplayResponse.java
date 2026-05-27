package com.medkernel.engine.context;

/**
 * 临床事件重放响应。
 */
public record ClinicalEventReplayResponse(
    String sourceEventId,
    String newEventId,
    ClinicalEventStatus status,
    String traceId
) {}
