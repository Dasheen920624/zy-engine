package com.medkernel.engine.followup;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 随访计划实体。
 */
@Table("followup_plan")
public record FollowupPlan(
    @Id Long id,
    @Column("plan_id") String planId,
    @Column("tenant_id") String tenantId,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("pathway_id") String pathwayId,
    @Column("disease_code") String diseaseCode,
    @Column("risk_level") String riskLevel,
    FollowupPlanStatus status,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
