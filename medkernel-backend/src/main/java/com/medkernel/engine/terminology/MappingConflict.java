package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 术语映射冲突记录（一对多 / 多对一 / 跨体系不一致 / 同名异义等场景）。
 *
 * <p>冲突由系统在产生候选或确认映射时检测出，并以独立条目保留供人工处置；
 * 状态字段 {@link MappingConflictStatus} 反映流转（OPEN/RESOLVED/IGNORED），
 * 冲突类型见 {@link MappingConflictType}。
 */
@Table("mapping_conflict")
public record MappingConflict(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("conflict_type") MappingConflictType conflictType,
    @Column("local_term_id") Long localTermId,
    @Column("standard_term_id") Long standardTermId,
    @Column("mapping_id") Long mappingId,
    @Column("risk_level") TermRiskLevel riskLevel,
    @Column("description") String description,
    @Column("status") MappingConflictStatus status,
    @Column("resolved_by") String resolvedBy,
    @Column("resolved_at") Instant resolvedAt,
    @Column("resolution_note") String resolutionNote,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    MappingConflict resolved(String note, String userId, Instant now) {
        return new MappingConflict(
            id, tenantId, conflictType, localTermId, standardTermId, mappingId,
            riskLevel, description, MappingConflictStatus.RESOLVED, userId, now,
            note, createdAt, createdBy, now, userId
        );
    }
}
