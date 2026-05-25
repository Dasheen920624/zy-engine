package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("term_mapping")
public record TermMapping(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("local_term_id") Long localTermId,
    @Column("standard_term_id") Long standardTermId,
    @Column("source_system") String sourceSystem,
    @Column("category") TermCategory category,
    @Column("confidence") Double confidence,
    @Column("risk_level") TermRiskLevel riskLevel,
    @Column("status") TermMappingStatus status,
    @Column("evidence_text") String evidenceText,
    @Column("confirmed_by") String confirmedBy,
    @Column("confirmed_at") Instant confirmedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    TermMapping confirmed(String userId, Instant now, String evidence, String sourceSystem, TermCategory category) {
        return new TermMapping(
            id, tenantId, localTermId, standardTermId,
            sourceSystem == null ? this.sourceSystem : sourceSystem,
            category == null ? this.category : category,
            confidence,
            riskLevel, TermMappingStatus.CONFIRMED, evidence, userId, now,
            createdAt, createdBy, now, userId
        );
    }
}
