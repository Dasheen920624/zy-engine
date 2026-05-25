package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("local_term")
public record LocalTerm(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("source_system") String sourceSystem,
    @Column("local_code") String localCode,
    @Column("category") TermCategory category,
    @Column("local_name") String localName,
    @Column("normalized_name") String normalizedName,
    @Column("department_id") String departmentId,
    @Column("status") LocalTermStatus status,
    @Column("first_seen_at") Instant firstSeenAt,
    @Column("last_seen_at") Instant lastSeenAt,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {
}
