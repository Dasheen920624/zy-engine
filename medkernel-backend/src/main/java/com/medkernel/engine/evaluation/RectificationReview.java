package com.medkernel.engine.evaluation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 整改复核记录实体。
 *
 * <p>保存复核结论、复核意见、证据引用和复核人；记录追加留痕，不覆写历史复核意见。
 */
@Table("rectification_review")
public record RectificationReview(
    @Id Long id,
    @Column("review_id") String reviewId,
    @Column("tenant_id") String tenantId,
    @Column("finding_id") String findingId,
    @Column("task_id") String taskId,
    RectificationReviewDecision decision,
    @Column("review_comment") String comment,
    @Column("evidence_ref") String evidenceRef,
    @Column("reviewer_id") String reviewerId,
    @Column("reviewed_at") Instant reviewedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
