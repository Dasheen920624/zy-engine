package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 临床事件原始 payload 旁路表，避免大字段拖慢事件列表。
 */
@Table("clinical_event_payload")
public record ClinicalEventPayload(
    @Id Long id,
    @Column("event_id") String eventId,
    @Column("tenant_id") String tenantId,
    @Column("payload") String payload,
    @Column("payload_uri") String payloadUri,
    @Column("storage_type") String storageType,
    @Column("content_type") String contentType,
    @Column("digest") String digest,
    @Column("size_bytes") Long sizeBytes,
    @Column("created_at") Instant createdAt,
    @Column("deleted_at") Instant deletedAt
) {}
