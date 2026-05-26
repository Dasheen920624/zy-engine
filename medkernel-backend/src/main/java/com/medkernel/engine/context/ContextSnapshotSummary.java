package com.medkernel.engine.context;

import java.time.Instant;

/**
 * snapshot 列表项摘要（GET ?patientId=... 返回）。
 */
public record ContextSnapshotSummary(
    String snapshotId,
    String patientId,
    String encounterId,
    ContextSnapshotStatus status,
    QualityStatus qualityStatus,
    Instant createdAt
) {}
