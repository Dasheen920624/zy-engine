package com.medkernel.engine.evaluation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("rectification_task")
public record RectificationTask(
    @Id Long id,
    @Column("task_id") String taskId,
    @Column("tenant_id") String tenantId,
    @Column("finding_id") String findingId,
    @Column("responsible_department_id") String responsibleDepartmentId,
    @Column("assignee_user_id") String assigneeUserId,
    RectificationTaskStatus status,
    @Column("due_at") Instant dueAt,
    @Column("rectification_summary") String rectificationSummary,
    @Column("evidence_ref") String evidenceRef,
    @Column("submitted_at") Instant submittedAt,
    @Column("submitted_by") String submittedBy,
    @Column("closed_at") Instant closedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
