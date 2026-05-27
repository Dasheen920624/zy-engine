package com.medkernel.engine.recommendation;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("recommendation_source")
public record RecommendationSource(
    @Id Long id,
    @Column("source_id") String sourceId,
    @Column("tenant_id") String tenantId,
    @Column("card_id") String cardId,
    @Column("source_type") RecommendationSourceType sourceType,
    @Column("source_ref_id") String sourceRefId,
    @Column("source_version") String sourceVersion,
    @Column("source_title") String sourceTitle,
    @Column("citation_locator") String citationLocator,
    @Column("source_hash") String sourceHash,
    String summary,
    @Column("created_at") Instant createdAt,
    @Column("created_by") String createdBy,
    @Column("updated_at") Instant updatedAt,
    @Column("updated_by") String updatedBy,
    @Column("trace_id") String traceId
) {}
