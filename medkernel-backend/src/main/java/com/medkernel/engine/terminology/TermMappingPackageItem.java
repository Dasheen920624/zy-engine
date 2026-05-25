package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("term_mapping_package_item")
public record TermMappingPackageItem(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("package_id") Long packageId,
    @Column("mapping_id") Long mappingId,
    @Column("mapping_snapshot") String mappingSnapshot,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy
) {
}
