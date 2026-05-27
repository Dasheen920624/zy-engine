package com.medkernel.engine.followup;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 随访问卷实体。
 */
@Table("followup_questionnaire")
public record FollowupQuestionnaire(
    @Id Long id,
    @Column("questionnaire_id") String questionnaireId,
    @Column("tenant_id") String tenantId,
    @Column("task_id") String taskId,
    @Column("form_data") String formData,
    BigDecimal score,
    String status,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
