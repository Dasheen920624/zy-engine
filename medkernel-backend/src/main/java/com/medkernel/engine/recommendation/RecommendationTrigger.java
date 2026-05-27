package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_trigger")
public record RecommendationTrigger(
    @Id Long id,
    @Column("trigger_id") String triggerId,
    @Column("tenant_id") String tenantId,
    @Column("trigger_code") String triggerCode,
    @Column("trigger_type") String triggerType,
    @Column("source_event_id") String sourceEventId,
    @Column("context_snapshot_id") String contextSnapshotId,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("patient_pathway_id") String patientPathwayId,
    @Column("scenario_code") String scenarioCode,
    @Column("package_version") String packageVersion,
    @Column("input_digest") String inputDigest,
    RecommendationTriggerStatus status,
    @Column("error_code") String errorCode,
    @Column("occurred_at") Instant occurredAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
