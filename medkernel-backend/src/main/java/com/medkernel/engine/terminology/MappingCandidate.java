package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("mapping_candidate")
public record MappingCandidate(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("local_term_id") Long localTermId,
    @Column("standard_term_id") Long standardTermId,
    @Column("confidence") Double confidence,
    @Column("candidate_source") MappingCandidateSource candidateSource,
    @Column("risk_level") TermRiskLevel riskLevel,
    @Column("evidence_text") String evidenceText,
    @Column("conflict_flag") Boolean conflictFlag,
    @Column("status") MappingCandidateStatus status,
    @Column("review_note") String reviewNote,
    @Column("reviewed_by") String reviewedBy,
    @Column("reviewed_at") Instant reviewedAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    MappingCandidate confirmed(String note, String userId, Instant now) {
        return new MappingCandidate(
            id, tenantId, localTermId, standardTermId, confidence, candidateSource,
            riskLevel, evidenceText, conflictFlag, MappingCandidateStatus.CONFIRMED,
            note, userId, now, createdAt, createdBy, now, userId
        );
    }
}
