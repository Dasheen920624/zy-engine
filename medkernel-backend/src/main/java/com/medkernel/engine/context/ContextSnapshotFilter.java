package com.medkernel.engine.context;

import java.time.Instant;

/**
 * snapshot 列表查询条件。
 */
public record ContextSnapshotFilter(
    String patientId,
    String encounterId,
    ContextSnapshotStatus status,
    Instant eventTimeFrom,
    Instant eventTimeTo
) {}
