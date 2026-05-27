package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("clinical_clock")
public record ClinicalClock(
    @Id Long id,
    @Column("clock_id") String clockId,
    @Column("tenant_id") String tenantId,
    @Column("patient_pathway_id") String patientPathwayId,
    @Column("node_code") String nodeCode,
    @Column("metric_code") String metricCode,
    @Column("started_at") Instant startedAt,
    @Column("due_at") Instant dueAt,
    @Column("completed_at") Instant completedAt,
    ClinicalClockStatus status,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
