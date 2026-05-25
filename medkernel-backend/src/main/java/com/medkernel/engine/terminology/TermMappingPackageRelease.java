package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("term_mapping_package_release")
public record TermMappingPackageRelease(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("package_id") Long packageId,
    @Column("target_package_id") Long targetPackageId,
    @Column("event_type") TermPackageReleaseEventType eventType,
    @Column("release_mode") PackageReleaseMode releaseMode,
    @Column("reason") String reason,
    @Column("gray_scope_json") String grayScopeJson,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy
) {
}
