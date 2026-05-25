package com.medkernel.engine.knowledge;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 来源文件版本。{@code content_hash} 用于真实性核验 + 重复检测。
 */
@Table("source_version")
public record SourceVersion(
    @Id Long id,
    @Column("tenant_id") String tenantId,
    @Column("source_document_id") Long sourceDocumentId,
    @Column("version_no") String versionNo,
    @Column("published_at") Instant publishedAt,
    @Column("content_hash") String contentHash,
    @Column("file_uri") String fileUri,
    @Column("language") String language,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy
) {
}
