package com.medkernel.engine.evaluation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("quality_finding")
public record QualityFinding(
    @Id Long id,
    @Column("finding_id") String findingId,
    @Column("tenant_id") String tenantId,
    @Column("run_id") String runId,
    @Column("result_id") String resultId,
    @Column("indicator_id") String indicatorId,
    @Column("finding_code") String findingCode,
    String title,
    String description,
    QualityFindingSeverity severity,
    QualityFindingStatus status,
    @Column("evidence_summary") String evidenceSummary,
    @Column("responsible_department_id") String responsibleDepartmentId,
    @Column("due_at") Instant dueAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
