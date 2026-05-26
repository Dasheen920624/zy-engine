package com.medkernel.engine.context;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 租户作用域内的 snapshot 创建幂等键。
 */
@Table("context_idempotency_key")
public record ContextIdempotencyKey(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("idem_key") String idempotencyKey,
    @Column("snapshot_id") String snapshotId,
    @Column("payload_digest") String payloadDigest,
    @Column("expires_at") Instant expiresAt,
    @Column("created_at") Instant createdAt
) {}
