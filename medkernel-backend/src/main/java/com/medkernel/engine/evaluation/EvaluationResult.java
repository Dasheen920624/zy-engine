package com.medkernel.engine.evaluation;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("evaluation_result")
public record EvaluationResult(
    @Id Long id,
    @Column("result_id") String resultId,
    @Column("tenant_id") String tenantId,
    @Column("run_id") String runId,
    @Column("indicator_id") String indicatorId,
    @Column("indicator_code") String indicatorCode,
    @Column("indicator_version") int indicatorVersion,
    @Column("subject_type") EvaluationSubjectType subjectType,
    @Column("subject_ref_id") String subjectRefId,
    @Column("score_value") BigDecimal scoreValue,
    @Column("result_level") EvaluationResultLevel resultLevel,
    @Column("hit_flag") boolean hitFlag,
    @Column("evidence_summary") String evidenceSummary,
    @Column("source_ref") String sourceRef,
    @Column("responsible_department_id") String responsibleDepartmentId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
