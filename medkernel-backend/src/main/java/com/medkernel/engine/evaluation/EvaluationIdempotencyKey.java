package com.medkernel.engine.evaluation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("evaluation_idempotency_key")
public record EvaluationIdempotencyKey(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("idem_key") String idempotencyKey,
    @Column("operation_type") EvaluationIdempotencyOperation operationType,
    @Column("finding_id") String findingId,
    @Column("task_id") String taskId,
    @Column("review_id") String reviewId,
    @Column("request_digest") String requestDigest,
    @Column("finding_status") QualityFindingStatus findingStatus,
    @Column("task_status") RectificationTaskStatus taskStatus,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("trace_id") String traceId
) {}
