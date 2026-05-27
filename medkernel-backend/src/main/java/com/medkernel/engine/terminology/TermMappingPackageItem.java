package com.medkernel.engine.terminology;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 术语映射包内单条映射快照（构包时定格）。
 *
 * <p>每条 item 对应一条 {@link TermMapping}，{@code mapping_snapshot} 保留构包时的不可变 JSON 快照，
 * 用于后续即使映射被改动或回滚，包内容仍可复现。
 */
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
