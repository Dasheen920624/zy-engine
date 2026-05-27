package com.medkernel.engine.context;

import java.time.Instant;

/**
 * 临床事件受理结果。
 */
public record ClinicalEventAcceptedResponse(
    String eventId,
    ClinicalEventStatus status,
    String payloadDigest,
    String traceId,
    Instant acceptedAt
) {}
