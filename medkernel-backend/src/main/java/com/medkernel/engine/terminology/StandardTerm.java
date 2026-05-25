package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("standard_term")
public record StandardTerm(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("standard_system") String standardSystem,
    @Column("term_code") String termCode,
    @Column("category") TermCategory category,
    @Column("display_name") String displayName,
    @Column("normalized_name") String normalizedName,
    @Column("version_no") String versionNo,
    @Column("status") StandardTermStatus status,
    @Column("source_version_id") Long sourceVersionId,
    @Column("evidence_text") String evidenceText,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {
}
