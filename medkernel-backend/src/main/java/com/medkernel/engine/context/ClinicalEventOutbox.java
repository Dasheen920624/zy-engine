package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 临床事件 outbox。通过数据库领取任务实现零外部依赖可靠处理。
 */
@Table("clinical_event_outbox")
public record ClinicalEventOutbox(
    @Id Long id,
    @Column("event_id") String eventId,
    @Column("tenant_id") String tenantId,
    @Column("claim_status") String claimStatus,
    @Column("claimed_by") String claimedBy,
    @Column("claimed_at") Instant claimedAt,
    @Column("next_attempt_at") Instant nextAttemptAt,
    @Column("retry_count") Integer retryCount,
    @Column("last_error_code") String lastErrorCode,
    @Column("created_at") Instant createdAt,
    @Column("processed_at") Instant processedAt
) {}
