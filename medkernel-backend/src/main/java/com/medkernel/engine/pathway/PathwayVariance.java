package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 患者路径变异记录。
 *
 * <p>记录执行偏离的节点、类型、原因、处置动作和可选继续节点，作为人工确认和审计事实。
 */
@Table("pathway_variance")
public record PathwayVariance(
    @Id Long id,
    @Column("variance_id") String varianceId,
    @Column("tenant_id") String tenantId,
    @Column("patient_pathway_id") String patientPathwayId,
    @Column("node_code") String nodeCode,
    @Column("variance_type") VarianceType varianceType,
    String reason,
    @Column("resolution_action") String resolutionAction,
    @Column("continue_node_code") String continueNodeCode,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
