package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_fatigue_signal")
public record RecommendationFatigueSignal(
    @Id Long id,
    @Column("signal_id") String signalId,
    @Column("tenant_id") String tenantId,
    @Column("trigger_id") String triggerId,
    @Column("card_id") String cardId,
    @Column("fatigue_key") String fatigueKey,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("operator_id") String operatorId,
    @Column("signal_type") RecommendationFatigueSignalType signalType,
    @Column("occurrence_count") int occurrenceCount,
    @Column("window_started_at") Instant windowStartedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
