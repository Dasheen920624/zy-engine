package com.medkernel.engine.context;

import java.time.Instant;

/**
 * 临床事件 payload 响应。
 */
public record ClinicalEventPayloadResponse(
    String eventId,
    String contentType,
    String storageType,
    String digest,
    Long sizeBytes,
    String payload,
    Instant createdAt
) {}
