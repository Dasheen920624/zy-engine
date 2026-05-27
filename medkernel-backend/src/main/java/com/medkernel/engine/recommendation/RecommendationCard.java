package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_card")
public record RecommendationCard(
    @Id Long id,
    @Column("card_id") String cardId,
    @Column("tenant_id") String tenantId,
    @Column("trigger_id") String triggerId,
    @Column("card_code") String cardCode,
    @Column("card_type") RecommendationCardType cardType,
    String title,
    String summary,
    @Column("suggested_action") String suggestedAction,
    @Column("risk_level") RecommendationRiskLevel riskLevel,
    @Column("interrupt_level") RecommendationInterruptLevel interruptLevel,
    RecommendationCardStatus status,
    @Column("requires_physician_confirmation") boolean requiresPhysicianConfirmation,
    @Column("ai_generated") boolean aiGenerated,
    @Column("source_summary") String sourceSummary,
    @Column("explanation_json") String explanationJson,
    @Column("fatigue_key") String fatigueKey,
    @Column("expires_at") Instant expiresAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
