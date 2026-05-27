package com.medkernel.engine.pathway;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 专病包主数据。
 *
 * <p>保存病种、专病包编码、版本、状态、来源、说明和发布信息，是路径模板的资产归属。
 */
@Table("specialty_package")
public record SpecialtyPackage(
    @Id Long id,
    @Column("package_id") String packageId,
    @Column("tenant_id") String tenantId,
    @Column("package_code") String packageCode,
    @Column("disease_code") String diseaseCode,
    String name,
    @Column("package_version") String packageVersion,
    SpecialtyPackageStatus status,
    @Column("source_ref") String sourceRef,
    String description,
    @Column("published_at") Instant publishedAt,
    @Column("published_by") String publishedBy,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
