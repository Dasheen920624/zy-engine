package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 临床事件流水。snapshot 上游入口或下游触发记录。
 */
@Table("clinical_event")
public record ClinicalEvent(
    @Id Long id,
    @Column("event_id") String eventId,
    @Column("tenant_id") String tenantId,
    @Column("event_type") ClinicalEventType eventType,
    @Column("source_system") String sourceSystem,
    @Column("payload_digest") String payloadDigest,
    @Column("occurred_at") Instant occurredAt,
    @Column("received_at") Instant receivedAt,
    @Column("snapshot_id") String snapshotId,
    @Column("processing_status") ClinicalEventStatus processingStatus,
    @Column("trace_id") String traceId
) {}
