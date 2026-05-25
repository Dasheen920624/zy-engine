package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("term_mapping_package")
public record TermMappingPackage(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("package_code") String packageCode,
    @Column("package_version") String packageVersion,
    @Column("display_name") String displayName,
    @Column("scope_level") String scopeLevel,
    @Column("scope_code") String scopeCode,
    @Column("status") TermMappingPackageStatus status,
    @Column("mapping_count") Integer mappingCount,
    @Column("content_hash") String contentHash,
    @Column("gray_scope_json") String grayScopeJson,
    @Column("published_by") String publishedBy,
    @Column("published_at") Instant publishedAt,
    @Column("rollback_from_package_id") Long rollbackFromPackageId,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy
) {

    TermMappingPackage withStatus(TermMappingPackageStatus nextStatus, String userId, Instant now) {
        return new TermMappingPackage(
            id, tenantId, packageCode, packageVersion, displayName, scopeLevel, scopeCode,
            nextStatus, mappingCount, contentHash, grayScopeJson,
            nextStatus == TermMappingPackageStatus.PUBLISHED || nextStatus == TermMappingPackageStatus.GRAY ? userId : publishedBy,
            nextStatus == TermMappingPackageStatus.PUBLISHED || nextStatus == TermMappingPackageStatus.GRAY ? now : publishedAt,
            rollbackFromPackageId, createdAt, createdBy, now, userId
        );
    }

    TermMappingPackage withGrayScope(String newGrayScopeJson) {
        return new TermMappingPackage(
            id, tenantId, packageCode, packageVersion, displayName, scopeLevel, scopeCode,
            status, mappingCount, contentHash, newGrayScopeJson, publishedBy, publishedAt,
            rollbackFromPackageId, createdAt, createdBy, updatedAt, updatedBy
        );
    }

    TermMappingPackage rolledBack(String userId, Instant now) {
        return new TermMappingPackage(
            id, tenantId, packageCode, packageVersion, displayName, scopeLevel, scopeCode,
            TermMappingPackageStatus.ROLLED_BACK, mappingCount, contentHash, grayScopeJson,
            publishedBy, publishedAt, rollbackFromPackageId, createdAt, createdBy, now, userId
        );
    }
}
