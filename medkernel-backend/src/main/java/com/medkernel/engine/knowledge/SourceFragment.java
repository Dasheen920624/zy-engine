package com.medkernel.engine.knowledge;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 来源版本下的引用锚点（章节 / 条款 / 表格行）。
 *
 * <p>{@code anchor_path} 是稳定的层级路径（如 {@code "section-3.2.1"} 或 {@code "table-2/row-5"}），
 * 在同一 {@code source_version_id} 内唯一。
 */
@Table("source_fragment")
public record SourceFragment(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("source_version_id") Long sourceVersionId,
    @Column("anchor_path") String anchorPath,
    @Column("anchor_label") String anchorLabel,
    @Column("text_excerpt") String textExcerpt,
    @Column("content_hash") String contentHash,
    @Column("created_at") Instant createdAt
) {
}
