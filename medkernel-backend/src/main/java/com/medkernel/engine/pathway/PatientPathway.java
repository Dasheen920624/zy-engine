package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("patient_pathway")
public record PatientPathway(
    @Id Long id,
    @Column("patient_pathway_id") String patientPathwayId,
    @Column("tenant_id") String tenantId,
    @Column("patient_id") String patientId,
    @Column("encounter_id") String encounterId,
    @Column("template_id") String templateId,
    @Column("current_node_code") String currentNodeCode,
    PatientPathwayStatus status,
    @Column("entered_at") Instant enteredAt,
    @Column("completed_at") Instant completedAt,
    @Column("exited_at") Instant exitedAt,
    @Column("exit_reason") String exitReason,
    @Column("last_event_id") String lastEventId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
