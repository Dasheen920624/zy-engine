package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 单个标准临床资源（snapshot 内 12 类对象之一的持久化形态）。
 */
@Table("canonical_resource")
public record CanonicalResource(
    @Id Long id,
    @Column("resource_id") String resourceId,
    @Column("snapshot_id") String snapshotId,
    @Column("tenant_id") String tenantId,
    @Column("resource_type") CanonicalResourceType resourceType,
    @Column("resource_payload") String resourcePayloadJson,
    @Column("source_system") String sourceSystem,
    @Column("source_record_id") String sourceRecordId,
    @Column("mapped_version") String mappedVersion,
    @Column("event_time") Instant eventTime,
    @Column("received_time") Instant receivedTime,
    @Column("quality_status") QualityStatus qualityStatus,
    @Column("seq_no") Integer seqNo,
    @Column("trace_id") String traceId
) {}
