package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_feedback")
public record RecommendationFeedback(
    @Id Long id,
    @Column("feedback_id") String feedbackId,
    @Column("tenant_id") String tenantId,
    @Column("card_id") String cardId,
    @Column("feedback_type") RecommendationFeedbackType feedbackType,
    @Column("reason_code") String reasonCode,
    @Column("reason_text") String reasonText,
    @Column("operator_id") String operatorId,
    @Column("operator_role") String operatorRole,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
